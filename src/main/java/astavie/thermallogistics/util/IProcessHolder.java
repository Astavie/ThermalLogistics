package astavie.thermallogistics.util;

import astavie.thermallogistics.attachment.Crafter;
import astavie.thermallogistics.process.IProcess;
import cofh.core.block.TileCore;
import cofh.thermaldynamics.duct.Attachment;
import cofh.thermaldynamics.duct.tiles.DuctUnit;
import cofh.thermaldynamics.duct.tiles.TileGrid;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.List;
import java.util.Set;

public interface IProcessHolder<P extends IProcess<P, T, I>, T extends DuctUnit<T, ?, ?>, I> extends IDestination<T, I>, IProcessLoader {

	TileCore getTile();

	Set<Crafter> getLinked();

	@SuppressWarnings("unchecked")
	static <P extends IProcess<P, T, I>, T extends DuctUnit<T, ?, ?>, I> IProcessHolder<P, T, I> read(World world, NBTTagCompound nbt) {
		TileEntity tile = world.getTileEntity(new BlockPos(nbt.getInteger("x"), nbt.getInteger("y"), nbt.getInteger("z")));
		if (tile instanceof IProcessHolder)
			return (IProcessHolder<P, T, I>) tile;
		if (tile instanceof TileGrid) {
			Attachment attachment = ((TileGrid) tile).getAttachment(nbt.getByte("side"));
			if (attachment != null && attachment instanceof IProcessHolder)
				return (IProcessHolder<P, T, I>) attachment;
		}
		return null;
	}

	void removeProcess(P process);

	static NBTTagCompound write(IProcessHolder holder) {
		NBTTagCompound nbt = new NBTTagCompound();
		nbt.setInteger("x", holder.getTile().getPos().getX());
		nbt.setInteger("y", holder.getTile().getPos().getY());
		nbt.setInteger("z", holder.getTile().getPos().getZ());
		nbt.setInteger("side", holder.getSide());
		return nbt;
	}

	int amountRequired(P process, I item);

	boolean itemsIdentical(I one, I two);

	I[] getInputs(P process);

	void addProcess(P process, int index);

	List<P> getProcesses();

}
