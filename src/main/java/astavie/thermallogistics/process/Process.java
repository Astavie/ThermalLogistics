package astavie.thermallogistics.process;

import astavie.thermallogistics.attachment.IRequester;
import net.minecraft.util.NonNullList;

import java.util.Collections;
import java.util.List;

public abstract class Process<I> {

	protected final IRequester<I> requester;
	protected final List<Request<I>> requests = NonNullList.create();

	public Process(IRequester<I> requester) {
		this.requester = requester;
	}

	public List<I> getStacks(IRequester<I> requester) {
		for (Request<I> request : requests)
			if (request.attachment.references(requester))
				return request.stacks;
		return Collections.emptyList();
	}

	public abstract void tick();

}
