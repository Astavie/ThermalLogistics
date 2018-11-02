package astavie.thermallogistics.util;

import cofh.thermaldynamics.duct.Attachment;
import cofh.thermaldynamics.duct.tiles.DuctUnit;
import cofh.thermaldynamics.duct.tiles.TileGrid;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public interface IDestination<T extends DuctUnit<T, ?, ?>, I> {

	T getDuct();

	byte getSide();

	int getType();

	boolean isInvalid();

	boolean isTick();

	static IDestination readDestination(World world, NBTTagCompound tag) {
		TileEntity tile = world.getTileEntity(new BlockPos(tag.getInteger("x"), tag.getInteger("y"), tag.getInteger("z")));
		if (tile != null) {
			if (tile instanceof IDestination)
				return (IDestination) tile;
			if (tile instanceof TileGrid) {
				Attachment attachment = ((TileGrid) tile).getAttachment(tag.getByte("side"));
				if (attachment != null && attachment instanceof IDestination)
					return (IDestination) attachment;
			}
		}
		return null;
	}

	static NBTTagCompound writeDestination(IDestination destination) {
		NBTTagCompound tag = new NBTTagCompound();
		tag.setInteger("x", destination.getDuct().x());
		tag.setInteger("y", destination.getDuct().y());
		tag.setInteger("z", destination.getDuct().z());
		tag.setByte("side", destination.getSide());
		return tag;
	}

	void removeLeftover(I leftover);

}
