package astavie.thermallogistics.tile;

import astavie.thermallogistics.ThermalLogistics;
import astavie.thermallogistics.attachment.ICrafter;
import astavie.thermallogistics.attachment.IRequester;
import astavie.thermallogistics.attachment.IRequesterContainer;
import astavie.thermallogistics.block.BlockTerminal;
import astavie.thermallogistics.process.Process;
import astavie.thermallogistics.process.Request;
import astavie.thermallogistics.util.StackHandler;
import cofh.CoFHCore;
import cofh.core.block.TileNameable;
import cofh.core.gui.GuiHandler;
import cofh.core.network.PacketBase;
import cofh.core.network.PacketHandler;
import cofh.core.network.PacketTileInfo;
import cofh.thermaldynamics.duct.attachments.servo.ServoFluid;
import cofh.thermaldynamics.duct.attachments.servo.ServoItem;
import cofh.thermaldynamics.duct.item.DuctUnitItem;
import cofh.thermaldynamics.duct.item.GridItem;
import cofh.thermaldynamics.duct.tiles.DuctToken;
import cofh.thermaldynamics.duct.tiles.DuctUnit;
import cofh.thermaldynamics.duct.tiles.TileGrid;
import cofh.thermaldynamics.multiblock.IGridTile;
import cofh.thermaldynamics.multiblock.Route;
import cofh.thermaldynamics.util.ListWrapper;
import com.google.common.collect.Lists;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import org.apache.commons.lang3.tuple.Triple;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.stream.Stream;

public abstract class TileTerminal<I> extends TileNameable implements ITickable, IRequesterContainer {

	public final InventorySpecial requester = new InventorySpecial(1, i -> i.getItem() == ThermalLogistics.Items.requester, null);
	public final List<Triple<I, Long, Boolean>> terminal = NonNullList.create();

	protected final Requester[] processes = new Requester[6];

	private final Set<Container> registry = new HashSet<>();

	public boolean refresh = false;

	public TileTerminal() {
		for (byte i = 0; i < 6; i++)
			processes[i] = new Requester<>(this, i);
	}

	public PacketTileInfo getSyncPacket() {
		updateTerminal();

		PacketTileInfo packet = PacketTileInfo.newPacket(this);

		// Terminal
		packet.addInt(terminal.size());
		for (Triple<I, Long, Boolean> stack : terminal) {
			StackHandler.writePacket(packet, stack.getLeft(), getItemClass(), true);
			packet.addLong(stack.getMiddle());
			packet.addBool(stack.getRight());
		}

		// Other
		sync(packet);
		return packet;
	}

	protected abstract void sync(PacketBase packet);

	protected abstract void read(PacketBase packet);

	protected abstract void read(PacketBase packet, byte message, EntityPlayer player);

	@Override
	public void handleTileInfoPacket(PacketBase payload, boolean isServer, EntityPlayer player) {
		if (isServer) {
			byte message = payload.getByte();
			if (message == 0) {
				request(payload);
				for (Requester requester : processes)
					requester.process.offset = world.getTotalWorldTime() + 1;
				markChunkDirty();
				PacketHandler.sendToAllAround(getSyncPacket(), this);
			} else read(payload, message, player);
		} else {
			terminal.clear();

			int size = payload.getInt();
			for (int i = 0; i < size; i++)
				terminal.add(Triple.of(StackHandler.readPacket(payload), payload.getLong(), payload.getBool()));

			refresh = true;
			read(payload);
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
		for (byte i = 0; i < 6; i++)
			list.appendTag(processes[i].process.writeNbt());

		nbt.setTag("requester", requester.get().writeToNBT(new NBTTagCompound()));
		nbt.setTag("processes", list);
		return nbt;
	}

	@Override
	public abstract String getTileName();

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);

		requester.set(new ItemStack(nbt.getCompoundTag("requester")));

		NBTTagList list = nbt.getTagList("processes", Constants.NBT.TAG_LIST);
		for (byte i = 0; i < list.tagCount(); i++)
			processes[i].process.readNbt((NBTTagList) list.get(i));
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
		for (Requester requester : processes) {
			if (!b && requester.getDuct() != null)
				requester.process.tick();
			else
				requester.process.requests.clear();
		}
	}

	protected abstract DuctToken<?, ?, ?> getDuctToken();

	protected abstract Process<I> createProcess(Requester<I> requester);

	protected abstract int amountRequired(I stack);

	protected abstract void request(PacketBase payload);

	protected abstract void updateTerminal();

	protected abstract boolean hasRequests();

	public abstract Class<I> getItemClass();

	@Override
	public IRequester<?> getRequester(int index) {
		return processes[index];
	}

	protected static class Requester<I> implements IRequester<I> {

		private final TileTerminal<I> terminal;
		private final byte side;

		protected final Process<I> process;

		private Requester(TileTerminal<I> terminal, byte side) {
			this.terminal = terminal;
			this.side = side;
			this.process = terminal.createProcess(this);
		}

		@Override
		public List<I> getInputFrom(IRequester<I> requester) {
			return process.getStacks(requester);
		}

		@Override
		public List<I> getOutputTo(IRequester<I> requester) {
			return Collections.emptyList();
		}

		@Override
		public boolean isEnabled() {
			return true;
		}

		@Override
		public boolean hasRequests() {
			return terminal.hasRequests();
		}

		@Override
		public int amountRequired(I stack) {
			return terminal.amountRequired(stack);
		}

		@Override
		public DuctUnit getDuct() {
			TileEntity tile = terminal.world.getTileEntity(terminal.pos.offset(EnumFacing.byIndex(side)));
			if (tile instanceof TileGrid) {
				DuctUnit duct = ((TileGrid) tile).getDuct(terminal.getDuctToken());
				if (duct != null && duct.isOutput((byte) (side ^ 1)))
					return duct;
			}
			return null;
		}

		@Override
		public TileEntity getTile() {
			return terminal;
		}

		@Override
		public byte getSide() {
			return (byte) (side ^ 1);
		}

		@Override
		public TileEntity getCachedTile() {
			return terminal;
		}

		@Override
		public ItemStack getIcon() {
			return new ItemStack(terminal.getBlockType());
		}

		@Override
		public ItemStack getTileIcon() {
			return new ItemStack(terminal.getBlockType());
		}

		@Override
		public void markDirty() {
			terminal.markChunkDirty();
		}

		@Override
		public int tickDelay() {
			return ServoItem.tickDelays[terminal.requester.get().getMetadata()];
		}

		@Override
		public void claim(ICrafter<I> crafter, I stack) {
			for (Iterator<Request<I>> iterator = process.requests.iterator(); iterator.hasNext(); ) {
				Request<I> request = iterator.next();
				if (request.attachment.references(crafter)) {
					request.decreaseStack(stack);
					if (request.stacks.isEmpty())
						iterator.remove();
					return;
				}
			}
		}

		@Override
		public int getMaxSend() {
			return ServoItem.maxSize[terminal.requester.get().getMetadata()];
		}

		@Override
		public byte getSpeed() {
			return ServoItem.speedBoost[terminal.requester.get().getMetadata()];
		}

		@Override
		public ListWrapper<Route<DuctUnitItem, GridItem>> getRoutes() {
			ListWrapper<Route<DuctUnitItem, GridItem>> routesWithInsertSideList = new ListWrapper<>();

			IGridTile duct = getDuct();
			if (!(duct instanceof DuctUnitItem) || duct.getGrid() == null) {
				routesWithInsertSideList.setList(new LinkedList<>(), ListWrapper.SortType.NORMAL);
				return routesWithInsertSideList;
			}

			Stream<Route<DuctUnitItem, GridItem>> routesWithDestinations = ServoItem.getRoutesWithDestinations(((DuctUnitItem) duct).getCache().outputRoutes);
			LinkedList<Route<DuctUnitItem, GridItem>> objects = Lists.newLinkedList();
			routesWithDestinations.forEach(objects::add);

			routesWithInsertSideList.setList(objects, ListWrapper.SortType.NORMAL);

			return routesWithInsertSideList;
		}

		@Override
		public boolean hasMultiStack() {
			return ServoItem.multiStack[terminal.requester.get().getMetadata()];
		}

		@Override
		public IItemHandler getCachedInv() {
			return terminal.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, EnumFacing.byIndex(side));
		}

		@Override
		public float getThrottle() {
			return ServoFluid.throttle[terminal.requester.get().getMetadata()];
		}

		@Override
		public int getIndex() {
			return side;
		}

		// Not used

		@Override
		public void onFinishCrafting(IRequester<I> requester, I stack) {
		}

		@Override
		public void onFinishCrafting(int index, int recipes) {
		}

	}

}
