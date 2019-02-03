package astavie.thermallogistics.util;

import astavie.thermallogistics.util.delegate.IDelegate;
import astavie.thermallogistics.util.delegate.IDelegateClient;
import astavie.thermallogistics.util.request.Requests;
import cofh.core.network.PacketBase;
import cofh.thermaldynamics.duct.Attachment;
import cofh.thermaldynamics.duct.tiles.DuctUnit;
import cofh.thermaldynamics.duct.tiles.TileGrid;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

public interface IRequester<T extends DuctUnit<T, ?, ?>, I> {

	@SuppressWarnings("unchecked")
	static <T extends DuctUnit<T, ?, ?>, I> IRequester<T, I> readNbt(NBTTagCompound tag) {
		TileEntity tile = DimensionManager.getWorld(tag.getInteger("dim")).getTileEntity(new BlockPos(tag.getInteger("x"), tag.getInteger("y"), tag.getInteger("z")));
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

	static NBTTagCompound writeNbt(IRequester requester, boolean writeIndex) {
		NBTTagCompound tag = new NBTTagCompound();
		tag.setInteger("dim", requester.getDuct().parent.world().provider.getDimension());
		tag.setInteger("x", requester.getBase().getX());
		tag.setInteger("y", requester.getBase().getY());
		tag.setInteger("z", requester.getBase().getZ());
		tag.setByte("side", requester.getSide());
		if (writeIndex && requester.getIndex() != -1)
			tag.setInteger("index", requester.getIndex());
		return tag;
	}

	static void writePacket(PacketBase packet, IRequester requester) {
		packet.addInt(requester.getDuct().parent.world().provider.getDimension());
		packet.addInt(requester.getBase().getX());
		packet.addInt(requester.getBase().getY());
		packet.addInt(requester.getBase().getZ());
		packet.addInt(requester.getSide());
		packet.addInt(requester.getIndex());
	}

	boolean isInvalid();

	boolean isTick();

	BlockPos getBase();

	default int getIndex() {
		return -1;
	}

	void removeLeftover(IRequester<T, I> requester, I leftover);

	List<Requests<T, I>> getRequests();

	IDelegate<I> getDelegate();

	@SideOnly(Side.CLIENT)
	IDelegateClient<I, ?> getClientDelegate();

}
