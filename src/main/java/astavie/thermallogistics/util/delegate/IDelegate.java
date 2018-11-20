package astavie.thermallogistics.util.delegate;

import net.minecraft.nbt.NBTTagCompound;

public interface IDelegate<I> {

	boolean isNull(I stack);

	NBTTagCompound writeStack(I stack);

	I readStack(NBTTagCompound tag);

}
