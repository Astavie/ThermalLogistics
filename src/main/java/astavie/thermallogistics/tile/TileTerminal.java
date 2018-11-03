package astavie.thermallogistics.tile;

import astavie.thermallogistics.ThermalLogistics;
import astavie.thermallogistics.attachment.Crafter;
import astavie.thermallogistics.block.BlockTerminal;
import astavie.thermallogistics.event.EventHandler;
import astavie.thermallogistics.item.ItemRequester;
import astavie.thermallogistics.process.IProcess;
import astavie.thermallogistics.util.IProcessHolder;
import astavie.thermallogistics.util.IProcessLoader;
import cofh.CoFHCore;
import cofh.core.block.TileCore;
import cofh.core.block.TileNameable;
import cofh.core.network.PacketBase;
import cofh.thermaldynamics.duct.tiles.DuctToken;
import cofh.thermaldynamics.duct.tiles.DuctUnit;
import cofh.thermaldynamics.duct.tiles.TileGrid;
import cofh.thermaldynamics.multiblock.MultiBlockGrid;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.relauncher.Side;

import java.util.*;

public abstract class TileTerminal<P extends IProcess<P, T, I>, T extends DuctUnit<T, ?, ?>, I> extends TileNameable implements IProcessHolder<P, T, I>, IProcessLoader, ITickable {

	public final InventorySpecial requester = new InventorySpecial(1, i -> i.getItem() instanceof ItemRequester, null);

	private final Set<Container> registry = new HashSet<>();

	protected List<P> processes = new LinkedList<>();
	protected EnumFacing duct;
	private NBTTagList _processes;

	public void register(Container container) {
		if (!world.isRemote) {
			registry.add(container);
			setActive(true);
		}
	}

	public void remove(Container container) {
		if (!world.isRemote) {
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
	public TileCore getTile() {
		return this;
	}

	@Override
	public Set<Crafter> getLinked() {
		return Collections.emptySet();
	}

	@Override
	public List<P> getProcesses() {
		return processes;
	}

	@Override
	public void addProcess(P process) {
		processes.add(process);
	}

	@Override
	public void removeProcess(P process) {
		processes.remove(process);
	}

	@Override
	@SuppressWarnings("unchecked")
	public T getDuct() {
		return (T) getDuct(getDuctToken());
	}

	public void setDuct(EnumFacing facing) {
		this.duct = facing;
		sendTilePacket(Side.CLIENT);
	}

	@Override
	public byte getSide() {
		return (byte) duct.getOpposite().getIndex();
	}

	@Override
	public int getType() {
		return requester.get().getItemDamage() % 5;
	}

	protected abstract DuctToken<?, ?, ?> getDuctToken();

	protected <U extends DuctUnit<U, G, C>, G extends MultiBlockGrid<U>, C> U getDuct(DuctToken<U, G, C> token) {
		TileEntity tile = world.getTileEntity(pos.offset(duct));
		if (tile instanceof TileGrid)
			return ((TileGrid) tile).getDuct(token);
		return null;
	}

	protected <U extends DuctUnit<U, G, C>, G extends MultiBlockGrid<U>, C> G getGrid(DuctToken<U, G, C> token) {
		U duct = getDuct(token);
		if (duct != null)
			return duct.getGrid();
		return null;
	}

	@Override
	public void update() {
		processes.removeIf(IProcess::isRemoved);
	}

	@Override
	public void loadProcesses() {
		if (_processes != null) {
			for (int i = 0; i < _processes.tagCount(); i++)
				ThermalLogistics.readProcess(world, _processes.getCompoundTagAt(i));
			_processes = null;
		}
	}

	@Override
	public void onNeighborBlockChange() {
		if (getDuct() == null) {
			IBlockState state = world.getBlockState(pos);
			state.getBlock().dropBlockAsItem(world, pos, state, 0);
			world.setBlockToAir(pos);
		}
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		nbt = super.writeToNBT(nbt);
		nbt.setTag("Requester", requester.get().writeToNBT(new NBTTagCompound()));
		nbt.setByte("Duct", (byte) duct.getIndex());

		NBTTagList processes = new NBTTagList();
		for (P process : this.processes)
			processes.appendTag(process.save());
		nbt.setTag("Processes", processes);

		return nbt;
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		this.requester.set(new ItemStack(nbt.getCompoundTag("Requester")));
		this.duct = EnumFacing.byIndex(nbt.getByte("Duct"));
		this._processes = nbt.getTagList("Processes", Constants.NBT.TAG_COMPOUND);
		EventHandler.LOADERS.add(this);
	}

	@Override
	public PacketBase getTilePacket() {
		PacketBase payload = super.getTilePacket();
		payload.addByte(duct.getIndex());
		return payload;
	}

	@Override
	public void handleTilePacket(PacketBase payload) {
		super.handleTilePacket(payload);
		this.duct = EnumFacing.byIndex(payload.getByte());
	}

	@Override
	public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newSate) {
		return oldState.getBlock() != newSate.getBlock();
	}

	@Override
	protected Object getMod() {
		return CoFHCore.instance;
	}

	@Override
	protected String getModVersion() {
		return ThermalLogistics.VERSION;
	}

	@Override
	public String getTileName() {
		return ThermalLogistics.terminalItem.getTranslationKey() + ".name";
	}

	@Override
	public boolean hasGui() {
		return true;
	}

	@Override
	public abstract Object getGuiClient(InventoryPlayer inventory);

	@Override
	public abstract Object getGuiServer(InventoryPlayer inventory);

}
