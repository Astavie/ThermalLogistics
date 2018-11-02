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

public interface IProcessHolder<P extends IProcess<P, T, I>, T extends DuctUnit<T, ?, ?>, I> {

	TileCore getTile();

	boolean isInvalid();

	List<P> getProcesses();

	void addProcess(P process);

	void removeProcess(P process);

	Set<Crafter> getLinked();

	T getDuct();

	byte getSide();

	int getType();

	int amountRequired(P process, I item);

	boolean itemsIdentical(I one, I two);

	I[] getInputs(P process);

	static IProcessHolder read(World world, NBTTagCompound nbt) {
		TileEntity tile = world.getTileEntity(new BlockPos(nbt.getInteger("x"), nbt.getInteger("y"), nbt.getInteger("z")));
		if (tile instanceof IProcessHolder)
			return (IProcessHolder) tile;
		if (tile instanceof TileGrid) {
			Attachment attachment = ((TileGrid) tile).getAttachment(nbt.getByte("side"));
			if (attachment != null && attachment instanceof IProcessHolder)
				return (IProcessHolder) attachment;
		}
		return null;
	}

	default NBTTagCompound write() {
		NBTTagCompound nbt = new NBTTagCompound();
		nbt.setInteger("x", getTile().getPos().getX());
		nbt.setInteger("y", getTile().getPos().getY());
		nbt.setInteger("z", getTile().getPos().getZ());
		nbt.setInteger("side", getSide());
		return nbt;
	}

}
