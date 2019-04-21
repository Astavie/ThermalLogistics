package astavie.thermallogistics.process;

import astavie.thermallogistics.attachment.IRequester;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.NonNullList;

import java.util.Collections;
import java.util.List;

public abstract class Process<I> {

	public long offset = 0;

	public final IRequester<I> requester;
	public final List<Request<I>> requests = NonNullList.create();

	public Process(IRequester<I> requester) {
		this.requester = requester;
	}

	public static <I> List<I> getStacks(List<Request<I>> requests, IRequester<I> requester) {
		for (Request<I> request : requests)
			if (request.attachment.references(requester))
				return request.stacks;
		return Collections.emptyList();
	}

	public abstract NBTTagList writeNbt();

	public abstract void readNbt(NBTTagList nbt);

	public List<I> getStacks(IRequester<I> requester) {
		return getStacks(requests, requester);
	}

	public List<I> getStacks() {
		List<I> stacks = NonNullList.create();
		for (Request<I> request : requests)
			stacks.addAll(request.stacks);
		return stacks;
	}

	public int getCount(I stack) {
		int count = 0;
		for (Request<I> request : requests)
			count += request.getCount(stack);
		return count;
	}

	public abstract void tick();

}
