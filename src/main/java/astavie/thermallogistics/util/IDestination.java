package astavie.thermallogistics.util;

import astavie.thermallogistics.util.delegate.IDelegate;
import astavie.thermallogistics.util.delegate.IDelegateClient;
import cofh.thermaldynamics.duct.Attachment;
import cofh.thermaldynamics.duct.tiles.DuctUnit;
import cofh.thermaldynamics.duct.tiles.TileGrid;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Collection;

public interface IDestination<T extends DuctUnit<T, ?, ?>, I> {

	T getDuct();

	byte getSide();

	int getType();

	@SuppressWarnings("unchecked")
	static <T extends DuctUnit<T, ?, ?>, I> IDestination<T, I> read(World world, NBTTagCompound tag) {
		TileEntity tile = world.getTileEntity(new BlockPos(tag.getInteger("x"), tag.getInteger("y"), tag.getInteger("z")));
		if (tile != null) {
			if (tile instanceof IDestination)
				return (IDestination<T, I>) tile;
			if (tile instanceof TileGrid) {
				Attachment attachment = ((TileGrid) tile).getAttachment(tag.getByte("side"));
				if (attachment != null) {
					if (tag.hasKey("index") && attachment instanceof IProcessHolder)
						return (IDestination<T, I>) ((IProcessHolder) attachment).getProcesses().get(tag.getInteger("index"));
					if (attachment instanceof IDestination)
						return (IDestination<T, I>) attachment;
				}
			}
		}
		return null;
	}

	boolean isInvalid();

	boolean isTick();

	static NBTTagCompound write(IDestination destination) {
		NBTTagCompound tag = new NBTTagCompound();
		tag.setInteger("x", destination.getDuct().x());
		tag.setInteger("y", destination.getDuct().y());
		tag.setInteger("z", destination.getDuct().z());
		tag.setByte("side", destination.getSide());
		if (destination.getIndex() != -1)
			tag.setInteger("index", destination.getIndex());
		return tag;
	}

	default int getIndex() {
		return -1;
	}

	ItemStack getDisplayStack();

	void removeLeftover(I leftover);

	Collection<Request<T, I>> getRequests();

	IDelegate<I> getDelegate();

	@SideOnly(Side.CLIENT)
	IDelegateClient<I, ?> getClientDelegate();

}
