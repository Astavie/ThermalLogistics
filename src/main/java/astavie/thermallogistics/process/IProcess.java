package astavie.thermallogistics.process;

import astavie.thermallogistics.util.IDestination;
import cofh.thermaldynamics.duct.tiles.DuctUnit;
import net.minecraft.nbt.NBTTagCompound;

public interface IProcess<P extends IProcess<P, T, I>, T extends DuctUnit<T, ?, ?>, I> {

	boolean isDone();

	boolean hasFailed();

	void setFailed();

	I getOutput();

	void update();

	void remove();

	boolean isRemoved();

	NBTTagCompound save();

	P setDestination(IDestination<T, I> destination);

	boolean isLoaded();

	void addDependent(IProcess other);

	void fail();

}
