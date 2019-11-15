package astavie.thermallogistics.tile;

import astavie.thermallogistics.ThermalLogistics;
import astavie.thermallogistics.attachment.IRequester;
import astavie.thermallogistics.block.BlockTerminal;
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

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

public abstract class TileTerminal<I> extends TileNameable implements ITickable, IRequester<I> {

	public final InventorySpecial requester = new InventorySpecial(1, i -> i.getItem() == ThermalLogistics.Items.requester, null);

	public final StackList<I> terminal;
	public final LinkedList<Request<I>> requests = new LinkedList<>();

	private final Set<Container> registry = new HashSet<>();

	public boolean refresh = false;

	public TileTerminal(StackList<I> terminal) {
		this.terminal = terminal;
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
		for (Request<I> request : requests) {
			Request.writePacket(packet, request);
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
				markChunkDirty();
				PacketHandler.sendToAllAround(getSyncPacket(), this);
			} else if (message == 1) {
				int index = payload.getInt();
				if (index < requests.size()) {
					Request<I> remove = requests.remove(index);
					markChunkDirty();

					PacketHandler.sendToAllAround(getSyncPacket(), this);

					// TODO: Shrink
				}
			} else read(payload, message, player);
		} else {
			refresh = true;
			read(payload);
		}
	}

	@Override
	public boolean shouldRefresh(World world, BlockPos pos, @Nonnull IBlockState oldState, @Nonnull IBlockState newSate) {
		return oldState.getBlock() != newSate.getBlock();
	}

	@Override
	public void cancel(MultiBlockGrid<?> grid, Type<I> type, long amount) {
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);

		NBTTagList list = new NBTTagList();
		for (Request<I> request : requests)
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
			requests.add(Request.readNBT(list.getCompoundTagAt(i), terminal::readType));
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
		boolean b = requester.get().isEmpty();
	}

	private void request(PacketBase payload) {
		Type<I> type = terminal.readType(payload);
		long amount = payload.getLong();

		for (byte side = 0; side < 6; side++) {
			DuctUnit<?, ?, ?> duct = getDuct(side);
			if (duct == null)
				continue;

			//noinspection unchecked
			StackList<I> stacks = (StackList<I>) Snapshot.INSTANCE.getStacks(duct.getGrid());

			long left = stacks.remove(type, amount);
			long removed = amount - left;

			addRequest(new Request<>(type, removed, side));

			amount = left;
			if (amount == 0)
				break;
		}

		if (amount > 0) {
			addRequest(new Request<>(type, amount, "Not enough items"));
		}
	}

	protected long amountRequested(Type<I> type, byte side) {
		long amount = 0;

		for (Request<I> request : requests)
			if (!request.isError() && request.side == side && request.type.equals(type))
				amount += request.amount;

		return amount;
	}

	protected void removeRequested(Type<I> type, long amount, byte side) {
		for (Iterator<Request<I>> iterator = requests.iterator(); iterator.hasNext(); ) {
			Request<I> request = iterator.next();
			if (request.side != side || !request.type.equals(type))
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
		if (!request.isError() && !requests.isEmpty()) {
			Request<I> last = requests.getLast();
			if (!last.isError() && last.side == request.side && last.type.equals(request.type)) {
				last.amount += request.amount;
				return;
			}
		}
		requests.add(request);
	}

	protected abstract void updateTerminal();

	protected abstract DuctUnit<?, ?, ?> getDuct(byte side);

}
