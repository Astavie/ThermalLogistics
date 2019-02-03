package astavie.thermallogistics.util;

import astavie.thermallogistics.attachment.Crafter;
import astavie.thermallogistics.process.IProcess;
import astavie.thermallogistics.util.reference.RequesterReference;
import cofh.core.block.TileCore;
import cofh.thermaldynamics.duct.Attachment;
import cofh.thermaldynamics.duct.tiles.DuctUnit;
import cofh.thermaldynamics.duct.tiles.TileGrid;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface IProcessHolder<P extends IProcess<P, T, I>, T extends DuctUnit<T, ?, ?>, I> extends IRequester<T, I>, IProcessLoader {

	TileCore getTile();

	Set<Crafter> getLinked();

	@Override
	default BlockPos getBase() {
		return getTile().getPos();
	}

	@SuppressWarnings("unchecked")
	static <P extends IProcess<P, T, I>, T extends DuctUnit<T, ?, ?>, I> IProcessHolder<P, T, I> read(World world, NBTTagCompound nbt) {
		TileEntity tile = world.getTileEntity(new BlockPos(nbt.getInteger("x"), nbt.getInteger("y"), nbt.getInteger("z")));
		if (tile instanceof IProcessHolder)
			return (IProcessHolder<P, T, I>) tile;
		if (tile instanceof TileGrid) {
			Attachment attachment = ((TileGrid) tile).getAttachment(nbt.getByte("side"));
			if (attachment instanceof IProcessHolder)
				return (IProcessHolder<P, T, I>) attachment;
		}
		return null;
	}

	void removeProcess(P process);

	static NBTTagCompound write(RequesterReference<? extends IProcessHolder> holder) {
		NBTTagCompound nbt = new NBTTagCompound();
		nbt.setInteger("x", holder.pos.getX());
		nbt.setInteger("y", holder.pos.getY());
		nbt.setInteger("z", holder.pos.getZ());
		nbt.setInteger("side", holder.side);
		return nbt;
	}

	int amountRequired(P process, I item);

	boolean itemsIdentical(I one, I two);

	Collection<I> getInputs(P process);

	void addProcess(P process, int index);

	List<P> getProcesses();

}
