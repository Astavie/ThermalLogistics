package astavie.thermallogistics.tile;

import astavie.thermallogistics.ThermalLogistics;
import astavie.thermallogistics.attachment.ICrafter;
import astavie.thermallogistics.block.BlockTerminal;
import astavie.thermallogistics.process.IProcessRequester;
import astavie.thermallogistics.process.Process;
import astavie.thermallogistics.process.Request;
import astavie.thermallogistics.process.Source;
import astavie.thermallogistics.util.RequesterReference;
import astavie.thermallogistics.util.Snapshot;
import astavie.thermallogistics.util.collection.StackList;
import astavie.thermallogistics.util.type.Type;
import cofh.CoFHCore;
import cofh.core.block.TileNameable;
import cofh.core.gui.GuiHandler;
import cofh.core.network.PacketBase;
import cofh.core.network.PacketHandler;
import cofh.core.network.PacketTileInfo;
import cofh.thermaldynamics.duct.tiles.DuctUnit;
import cofh.thermaldynamics.multiblock.MultiBlockGrid;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public abstract class TileTerminal<I> extends TileNameable implements ITickable, IProcessRequester<I> {

	public final InventorySpecial requester = new InventorySpecial(1, i -> i.getItem() == ThermalLogistics.Items.requester, null);

	public final StackList<I> terminal = createStackList();
	public final LinkedList<Request<I>> requests = new LinkedList<>();

	private final Set<Container> registry = new HashSet<>();

	protected Process<I> process;

	public boolean refresh = false;

	private static <I> boolean references(@Nullable RequesterReference<I> reference, @Nullable ICrafter<I> crafter) {
		return (reference == null && crafter == null) || (reference != null && reference.references(crafter));
	}

	public PacketTileInfo getSyncPacket() {
		updateTerminal();

		PacketTileInfo packet = PacketTileInfo.newPacket(this);

		// Other
		sync(packet);
		return packet;
	}

	private void sync(PacketBase packet) {
		terminal.writePacket(packet);

		packet.addInt(requests.size());
		for (int i = 0; i < requests.size(); i++) {
			Request<I> request = requests.get(i);
			Request.writePacket(packet, request, i);
		}
	}

	private void read(PacketBase packet) {
		terminal.readPacket(packet);

		requests.clear();

		int size = packet.getInt();
		for (int i = 0; i < size; i++) {
			addRequest(Request.readPacket(packet, terminal::readType));
		}
	}

	protected abstract void read(PacketBase packet, byte message, EntityPlayer player);

	@Override
	public boolean referencedBy(RequesterReference<?> reference) {
		return reference.dim == world.provider.getDimension() && reference.pos.equals(pos);
	}

	@Override
	public RequesterReference<I> createReference() {
		return new RequesterReference<>(world.provider.getDimension(), pos);
	}

	@Override
	public ItemStack getIcon() {
		return ItemStack.EMPTY;
	}

	@Override
	public void handleTileInfoPacket(PacketBase payload, boolean isServer, EntityPlayer player) {
		if (isServer) {
			byte message = payload.getByte();
			if (message == 0) {
				request(payload);
			} else if (message == 1) {
				int start = payload.getInt();
				int end = payload.getInt();

				if (start < requests.size()) {
					clearRequests(requests.subList(start, Math.min(end, requests.size())));

					markChunkDirty();
					PacketHandler.sendToAllAround(getSyncPacket(), this);
				}
			} else read(payload, message, player);
		} else {
			refresh = true;
			read(payload);
		}
	}

	private void clearRequests(List<Request<I>> list) {
		for (Iterator<Request<I>> iterator = list.iterator(); iterator.hasNext(); ) {
			removeRequest(iterator.next());
			iterator.remove();
		}
	}

	private void removeRequest(Request<I> request) {
		if (!request.isError()) {
			if (request.source.isCrafter()) {
				((ICrafter<I>) request.source.crafter.get()).cancel(this, request.type, request.amount);
			} else {
				Snapshot.INSTANCE.<I>getStacks(getDuct(request.source.side).getGrid()).add(request.type, request.amount);
				terminal.add(request.type, request.amount);
			}
		}
	}

	@Override
	public boolean shouldRefresh(World world, BlockPos pos, @Nonnull IBlockState oldState, @Nonnull IBlockState newSate) {
		return oldState.getBlock() != newSate.getBlock();
	}

	@Override
	public void onFail(MultiBlockGrid<?> grid, RequesterReference<I> crafter, Type<I> type, long amount) {
		long left = amount;

		if (grid != null) {
			for (byte side = 0; side < 6; side++) {
				DuctUnit<?, ?, ?> duct = getDuct(side);
				if (duct == null || duct.getGrid() != grid)
					continue;

				long remove = Math.min(left, amountRequested(new Source<>(side), type));
				removeRequested(new Source<>(side), type, remove);
				left -= remove;

				if (left == 0)
					break;
			}
		} else {
			long remove = Math.min(left, amountRequested(new Source<>(crafter), type));
			removeRequested(new Source<>(crafter), type, remove);
			left -= remove;
		}

		if (left > 0) {
			// This shouldn't happen
			throw new IllegalStateException();
		}

		request(type, amount);
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);

		NBTTagList list = new NBTTagList();
		for (Request<I> request : requests)
			if (!request.isError())
				list.appendTag(Request.writeNBT(request));

		nbt.setTag("requester", requester.get().writeToNBT(new NBTTagCompound()));
		nbt.setTag("requests", list);
		return nbt;
	}

	@Override
	public abstract String getTileName();

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);

		requester.set(new ItemStack(nbt.getCompoundTag("requester")));

		requests.clear();
		NBTTagList list = nbt.getTagList("requests", Constants.NBT.TAG_COMPOUND);
		for (int i = 0; i < list.tagCount(); i++)
			requests.add(Request.readNBT(list.getCompoundTagAt(i), terminal::readType, i));
	}

	public void register(Container container) {
		if (!world.isRemote && !isInvalid()) {
			registry.add(container);
			setActive(true);
		}
	}

	public void remove(Container container) {
		if (!world.isRemote && !isInvalid()) {
			registry.remove(container);
			if (registry.isEmpty())
				setActive(false);
		}
	}

	private void setActive(boolean active) {
		IBlockState state = world.getBlockState(pos);
		if (state.getValue(BlockTerminal.ACTIVE) != active)
			world.setBlockState(pos, state.withProperty(BlockTerminal.ACTIVE, active), 2);
	}

	@Override
	protected Object getMod() {
		return CoFHCore.instance;
	}

	@Override
	protected String getModVersion() {
		return ThermalLogistics.MOD_VERSION;
	}

	@Override
	public boolean hasGui() {
		return true;
	}

	@Override
	public abstract Object getGuiClient(InventoryPlayer inventory);

	@Override
	public abstract Object getGuiServer(InventoryPlayer inventory);

	@Override
	public boolean openGui(EntityPlayer player) {
		if (hasGui()) {
			PacketHandler.sendTo(getSyncPacket(), player);
			player.openGui(getMod(), GuiHandler.TILE_ID, world, pos.getX(), pos.getY(), pos.getZ());
		}
		return hasGui();
	}

	@Override
	public DuctUnit<?, ?, ?> getDuct(MultiBlockGrid<?> grid) {
		for (byte side = 0; side < 6; side++) {
			DuctUnit<?, ?, ?> duct = getDuct(side);
			if (duct == null || duct.getGrid() != grid)
				continue;

			return duct;
		}

		return null;
	}

	@Override
	public byte getSide(MultiBlockGrid<?> grid) {
		for (byte side = 0; side < 6; side++) {
			DuctUnit<?, ?, ?> duct = getDuct(side);
			if (duct == null || duct.getGrid() != grid)
				continue;

			return side;
		}

		throw new IllegalStateException();
	}

	@Override
	public void update() {
		if (!world.isRemote) {
			if (requester.get().isEmpty()) {
				clearRequests(requests);
			} else {

				// Remove requests without valid side
				for (int i = requests.size() - 1; i >= 0; i--) {
					Request<I> request = requests.get(i);
					if (!request.isError() && !request.source.isCrafter() && getDuct(request.source.side) == null) {
						requests.remove(i);
						request(request.type, request.amount);
					}
				}

				process.update();
			}
		}
	}

	@Override
	public void onCrafterSend(ICrafter<I> crafter, Type<I> type, long amount, byte side) {
		for (int i = 0; i < requests.size(); i++) {
			Request<I> request = requests.get(i);
			if (request.isError() || !request.source.isCrafter() || !references(request.source.crafter, crafter) || !request.type.equals(type))
				continue;

			if (request.amount <= amount) {
				amount -= request.amount;
				request.source = new Source<>(side);
			} else {
				request.amount -= amount;
				requests.add(i, new Request<>(type, amount, new Source<>(side), 0));
				amount = 0;
			}

			if (amount == 0)
				return;
		}

		// This shouldn't happen
		throw new IllegalStateException();
	}

	@Override
	public BlockPos getDestination() {
		return pos;
	}

	@Override
	public Map<Source<I>, StackList<I>> getRequests() {
		Map<Source<I>, StackList<I>> map = new HashMap<>();

		for (Request<I> request : requests) {
			if (!request.isError()) {
				map.computeIfAbsent(request.source, (c) -> createStackList());
				map.get(request.source).add(request.type, request.amount);
			}
		}

		return map;
	}

	protected abstract StackList<I> createStackList();

	private void request(PacketBase payload) {
		Type<I> type = terminal.readType(payload);
		long amount = payload.getLong();

		request(type, amount);
	}

	@Override
	public Set<MultiBlockGrid<?>> getGrids() {
		Set<MultiBlockGrid<?>> grids = new HashSet<>();

		for (byte side = 0; side < 6; side++) {
			DuctUnit<?, ?, ?> duct = getDuct(side);
			if (duct != null)
				grids.add(duct.getGrid());
		}

		return grids;
	}

	private void request(Type<I> type, long amount) { // TODO: Put this in Process
		updateTerminal();

		Set<MultiBlockGrid<?>> grids = getGrids();

		// REQUEST

		long left = amount;

		for (MultiBlockGrid<?> grid : grids) {
			List<Request<I>> requests = process.request(grid, type, left, terminal::amount);
			left = 0;

			for (Request<I> request : requests) {
				if (request.isError()) {
					left += request.amount;
				} else {
					addRequest(request);
				}
			}
		}

		// MAKE SOME ERRORS!

		if (left > 0) {
			Request<I> error = new Request<>(type, left, 0, new LinkedList<>());

			for (MultiBlockGrid<?> grid : grids) {
				List<Request<I>> requests = process.request(grid, type, left, terminal::amount);
				for (Request<I> request : requests) {
					if (request.isError()) {
						for (List<Pair<Type<I>, Long>> list : request.error)
							if (!error.error.contains(list))
								error.error.add(list);
					} else {
						// This shouldn't happen...
						throw new IllegalStateException();
					}
				}
			}

			addRequest(error);
		}

		if (!world.isRemote) {
			markChunkDirty();
			PacketHandler.sendToAllAround(getSyncPacket(), this);
		}
	}

	protected long amountRequested(Source<I> source, Type<I> type) {
		long amount = 0;

		for (Request<I> request : requests)
			if (!request.isError() && request.source.equals(source) && request.type.equals(type))
				amount += request.amount;

		return amount;
	}

	protected void removeRequested(Source<I> source, Type<I> type, long amount) {
		for (Iterator<Request<I>> iterator = requests.iterator(); iterator.hasNext(); ) {
			Request<I> request = iterator.next();
			if (request.isError() || !request.source.equals(source) || !request.type.equals(type))
				continue;

			long remove = Math.min(request.amount, amount);
			request.amount -= remove;
			amount -= remove;

			if (request.amount == 0)
				iterator.remove();
			if (amount == 0)
				return;
		}

		// This shouldn't happen...
		throw new IllegalStateException();
	}

	private void addRequest(Request<I> request) {
		if (!request.isError()) {

			if (!world.isRemote && !request.source.isCrafter()) {
				Snapshot.INSTANCE.<I>getStacks(getDuct(request.source.side).getGrid()).remove(request.type, request.amount);
				terminal.remove(request.type, request.amount);
			}

			if (!requests.isEmpty()) {
				Request<I> last = requests.getLast();
				if (!last.isError() && last.source.equals(request.source) && last.type.equals(request.type)) {
					last.amount += request.amount;
					return;
				}
			}
		}

		requests.add(request);
	}

	protected abstract void updateTerminal();

}
