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

public interface IRequester<T extends DuctUnit<T, ?, ?>, I> {

	@SuppressWarnings("unchecked")
	static <T extends DuctUnit<T, ?, ?>, I> IRequester<T, I> read(World world, NBTTagCompound tag) {
		TileEntity tile = world.getTileEntity(new BlockPos(tag.getInteger("x"), tag.getInteger("y"), tag.getInteger("z")));
		if (tile != null) {
			if (tag.hasKey("index") && tile instanceof IProcessHolder)
				return (IRequester<T, I>) ((IProcessHolder) tile).getProcesses().get(tag.getInteger("index"));
			if (tile instanceof IRequester)
				return (IRequester<T, I>) tile;
			if (tile instanceof TileGrid) {
				Attachment attachment = ((TileGrid) tile).getAttachment(tag.getByte("side"));
				if (attachment != null) {
					if (tag.hasKey("index") && attachment instanceof IProcessHolder)
						return (IRequester<T, I>) ((IProcessHolder) attachment).getProcesses().get(tag.getInteger("index"));
					if (attachment instanceof IRequester)
						return (IRequester<T, I>) attachment;
				}
			}
		}
		return null;
	}

	T getDuct();

	byte getSide();

	int getType();

	static NBTTagCompound write(IRequester requester) {
		NBTTagCompound tag = new NBTTagCompound();
		tag.setInteger("x", requester.getBase().getX());
		tag.setInteger("y", requester.getBase().getY());
		tag.setInteger("z", requester.getBase().getZ());
		tag.setByte("side", requester.getSide());
		if (requester.getIndex() != -1)
			tag.setInteger("index", requester.getIndex());
		return tag;
	}

	boolean isInvalid();

	boolean isTick();

	BlockPos getBase();

	default int getIndex() {
		return -1;
	}

	ItemStack getDisplayStack();

	void removeLeftover(IRequester<T, I> requester, I leftover);

	Collection<IRequest<T, I>> getRequests();

	IDelegate<I> getDelegate();

	@SideOnly(Side.CLIENT)
	IDelegateClient<I, ?> getClientDelegate();

}
