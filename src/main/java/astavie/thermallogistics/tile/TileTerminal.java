package astavie.thermallogistics.tile;

import astavie.thermallogistics.ThermalLogistics;
import astavie.thermallogistics.attachment.IRequester;
import astavie.thermallogistics.block.BlockTerminal;
import astavie.thermallogistics.util.RequesterReference;
import astavie.thermallogistics.util.collection.StackList;
import astavie.thermallogistics.util.type.Type;
import cofh.CoFHCore;
import cofh.core.block.TileNameable;
import cofh.core.gui.GuiHandler;
import cofh.core.network.PacketBase;
import cofh.core.network.PacketHandler;
import cofh.core.network.PacketTileInfo;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.Set;

public abstract class TileTerminal<I> extends TileNameable implements ITickable, IRequester<I> {

	public final InventorySpecial requester = new InventorySpecial(1, i -> i.getItem() == ThermalLogistics.Items.requester, null);

	public final StackList<I> terminal;
	public final StackList<I> requests;

	private final Set<Container> registry = new HashSet<>();

	public boolean refresh = false;

	public TileTerminal(StackList<I> terminal, StackList<I> requests) {
		this.terminal = terminal;
		this.requests = requests;
	}

	public PacketTileInfo getSyncPacket() {
		updateTerminal();

		PacketTileInfo packet = PacketTileInfo.newPacket(this);

		// Other
		sync(packet);
		return packet;
	}

	protected abstract void sync(PacketBase packet);

	protected abstract void read(PacketBase packet);

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
					Pair<Type<I>, Long> remove = requests.remove(index);
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
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);

		nbt.setTag("requester", requester.get().writeToNBT(new NBTTagCompound()));
		return nbt;
	}

	@Override
	public abstract String getTileName();

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);

		requester.set(new ItemStack(nbt.getCompoundTag("requester")));
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

	protected abstract void request(PacketBase payload);

	protected abstract void updateTerminal();

	public abstract Class<I> getItemClass();

}
