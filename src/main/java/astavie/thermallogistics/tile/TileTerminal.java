package astavie.thermallogistics.tile;

import astavie.thermallogistics.ThermalLogistics;
import astavie.thermallogistics.attachment.ICrafter;
import astavie.thermallogistics.attachment.IRequester;
import astavie.thermallogistics.attachment.IRequesterContainer;
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
import cofh.thermaldynamics.duct.attachments.servo.ServoItem;
import cofh.thermaldynamics.duct.tiles.DuctUnit;
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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public abstract class TileTerminal<I> extends TileNameable implements ITickable, IRequesterContainer<I> {

	public final InventorySpecial requester = new InventorySpecial(1, i -> i.getItem() == ThermalLogistics.Items.requester, null);

	public final StackList<I> terminal = createStackList();
	public final LinkedList<Request<I>> requests = new LinkedList<>();

	private final Set<Container> registry = new HashSet<>();

	@SuppressWarnings("unchecked")
	protected TerminalRequester<I>[] requesters = new TerminalRequester[6];

	public boolean refresh = false;

	private static <I> boolean references(@Nullable RequesterReference<I> reference, @Nullable ICrafter<I> crafter) {
		return (reference == null && crafter == null) || (reference != null && reference.references(crafter));
	}

	public PacketTileInfo getSyncPacket() {
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
			addRequest(Request.readPacket(packet, terminal::readType, this::readStackList));
		}
	}

	protected abstract void read(PacketBase packet, byte message, EntityPlayer player);

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
				IRequester<I> requester = requesters[request.source.side];
				((ICrafter<I>) request.source.crafter.get()).cancel(requester, request.type, request.amount);
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
	public void update() {
		if (!world.isRemote) {
			if (requester.get().isEmpty()) {
				clearRequests(requests);
			} else if (world.getTotalWorldTime() % ServoItem.tickDelays[requester.get().getMetadata()] == 0) {

				// Remove requests without valid side
				for (int i = requests.size() - 1; i >= 0; i--) {
					Request<I> request = requests.get(i);
					if (!request.isError() && !requesters[request.source.side].isEnabled()) {
						requests.remove(i);
						request(request.type, request.amount);
					}
				}

				boolean onlyCheck = false;
				for (TerminalRequester<I> requester : requesters) {
					if (requester.isEnabled() && requester.process.update(onlyCheck)) {
						onlyCheck = true;
					}
				}
			}
		}
	}

	@Override
	public List<IRequester<I>> getRequesters() {
		return Arrays.asList(requesters);
	}

	protected abstract StackList<I> createStackList();

	private void request(PacketBase payload) {
		Type<I> type = terminal.readType(payload);
		long amount = payload.getLong();

		request(type, amount);
	}

	private void request(Type<I> type, long amount) {
		updateTerminal();

		// REQUEST

		long left = amount;

		for (byte side = 0; side < 6; side++) {
			if (!requesters[side].isEnabled())
				continue;

			List<Request<I>> requests = requesters[side].process.request(type, left, terminal::amount, this::createStackList);
			left = 0;

			for (Request<I> request : requests) {
				if (request.isError()) {
					left += request.amount;
				} else {
					addRequest(request);
				}
			}
		}

		if (left > 0) {
			// Make some errors!

			Request<I> error = null;

			for (byte side = 0; side < 6; side++) {
				if (!requesters[side].isEnabled())
					continue;

				List<Request<I>> requests = requesters[side].process.request(type, left, terminal::amount, this::createStackList);
				for (Request<I> request : requests) {
					if (request.isError()) {
						if (error == null || error.missing.size() < request.missing.size() || (error.missing.size() == 1 && error.missing.amount(type) > 0)) {
							error = request;
						}
					} else {
						throw new IllegalStateException();
					}
				}
			}

			if (error == null) {
				throw new IllegalStateException();
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

	public abstract void updateTerminal();

	protected abstract DuctUnit<?, ?, ?> getDuct(byte side);

	protected abstract ItemStack getIcon();

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

	private StackList<I> readStackList(PacketBase payload) {
		StackList<I> list = createStackList();
		list.readPacket(payload);
		return list;
	}

	public static abstract class TerminalRequester<I> implements IProcessRequester<I> {

		protected final TileTerminal<I> terminal;
		protected final byte side;

		protected Process<I> process;

		public TerminalRequester(TileTerminal<I> terminal, byte side) {
			this.terminal = terminal;
			this.side = side;
		}

		public boolean isEnabled() {
			return getDuct() != null;
		}

		@Override
		public Map<RequesterReference<I>, StackList<I>> getRequests() {
			Map<RequesterReference<I>, StackList<I>> map = new HashMap<>();

			for (Request<I> request : terminal.requests) {
				if (!request.isError() && request.source.side == side) {
					map.computeIfAbsent(request.source.crafter, (c) -> terminal.createStackList());
					map.get(request.source.crafter).add(request.type, request.amount);
				}
			}

			return map;
		}

		@Override
		public void onCrafterSend(ICrafter<I> crafter, Type<I> type, long amount) {
			for (int i = 0; i < terminal.requests.size(); i++) {
				Request<I> request = terminal.requests.get(i);
				if (request.isError() || !request.source.isCrafter() || !references(request.source.crafter, crafter) || request.source.side != side || !request.type.equals(type))
					continue;

				if (request.amount <= amount) {
					amount -= request.amount;
					request.source = new Source<>(side);
				} else {
					request.amount -= amount;
					terminal.requests.add(i, new Request<>(type, amount, new Source<>(side), 0));
					amount = 0;
				}

				if (amount == 0)
					return;
			}

			// This shouldn't happen
			throw new IllegalStateException();
		}

		@Override
		public boolean hasWants() {
			return false;
		}

		@Override
		public long amountRequired(Type<I> type) {
			return 0;
		}

		@Override
		public void addRequest(Request<I> request) {
			terminal.addRequest(request);
		}

		@Override
		public boolean referencedBy(RequesterReference<?> reference) {
			return reference.dim == terminal.world.provider.getDimension() && reference.pos.equals(terminal.pos) && reference.index == side;
		}

		@Override
		public RequesterReference<I> createReference() {
			return new RequesterReference<>(terminal.world.provider.getDimension(), terminal.pos, (byte) 0, side);
		}

		@Override
		public ItemStack getIcon() {
			return ItemStack.EMPTY;
		}

		@Override
		public ItemStack getTileIcon() {
			return terminal.getIcon();
		}

		@Override
		public BlockPos getDestination() {
			return terminal.pos;
		}

		@Override
		public DuctUnit<?, ?, ?> getDuct() {
			return terminal.getDuct(side);
		}

		@Override
		public byte getSide() {
			return side;
		}

		@Override
		public void onFail(Type<I> type, long amount) {
			Source<I> source = new Source<>(side);

			long remove = terminal.amountRequested(source, type);
			terminal.removeRequested(source, type, remove);

			terminal.request(type, amount);
		}

		@Override
		public void onFail(RequesterReference<I> crafter, Type<I> type, long amount) {
			Source<I> source = new Source<>(side, crafter);

			long remove = terminal.amountRequested(source, type);
			terminal.removeRequested(source, type, remove);

			terminal.request(type, amount);
		}

		@Override
		public StackList<I> getRequestedStacks() {
			StackList<I> list = terminal.createStackList();

			for (Request<I> request : terminal.requests) {
				if (request.isError() || request.source.isCrafter() || request.source.side != side)
					continue;

				list.add(request.type, request.amount);
			}

			return list;
		}

		@Override
		public StackList<I> getRequestedStacks(ICrafter<I> crafter) {
			StackList<I> list = terminal.createStackList();

			for (Request<I> request : terminal.requests) {
				if (request.isError() || !request.source.isCrafter() || request.source.side != side)
					continue;

				if (request.source.crafter.references(crafter))
					list.add(request.type, request.amount);
			}

			return list;
		}

	}

}
