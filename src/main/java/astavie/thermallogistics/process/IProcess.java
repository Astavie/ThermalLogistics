package astavie.thermallogistics.process;

import astavie.thermallogistics.util.IRequester;
import astavie.thermallogistics.util.request.IRequest;
import cofh.thermaldynamics.duct.tiles.DuctUnit;
import net.minecraft.nbt.NBTTagCompound;

import java.util.Collection;
import java.util.Collections;

public interface IProcess<P extends IProcess<P, T, I>, T extends DuctUnit<T, ?, ?>, I> extends IRequest<T, I>, IRequester<T, I> {

	boolean isDone();

	boolean hasFailed();

	void setFailed();

	I getOutput();

	void update();

	void remove();

	boolean isRemoved();

	NBTTagCompound save();

	P setDestination(IRequester<T, I> destination);

	boolean isLoaded();

	void addDependent(IProcess other);

	void fail();

	@Override
	default Collection<I> getStacks() {
		return Collections.singleton(getOutput());
	}

	@Override
	default IRequester<T, I> getStart() {
		return this;
	}

}
