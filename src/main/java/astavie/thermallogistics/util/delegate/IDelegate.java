package astavie.thermallogistics.util.delegate;

import cofh.core.network.PacketBase;
import net.minecraft.nbt.NBTTagCompound;

public interface IDelegate<I> {

	boolean isNull(I stack);

	NBTTagCompound writeNbt(I stack);

	void writePacket(PacketBase packet, I stack);

	I readNbt(NBTTagCompound tag);

	I readPacket(PacketBase packet);

}
