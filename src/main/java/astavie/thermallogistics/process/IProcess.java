package astavie.thermallogistics.process;

import astavie.thermallogistics.util.IRequester;
import astavie.thermallogistics.util.request.IRequest;
import cofh.thermaldynamics.duct.tiles.DuctUnit;
import net.minecraft.nbt.NBTTagCompound;

import java.util.Collections;
import java.util.List;

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
	default List<I> getStacks() {
		return Collections.singletonList(getOutput());
	}

	@Override
	default IRequester<T, I> getStart() {
		return this;
	}

}
