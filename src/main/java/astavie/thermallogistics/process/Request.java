package astavie.thermallogistics.process;

import astavie.thermallogistics.util.RequesterReference;
import net.minecraft.util.NonNullList;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.List;

public abstract class Request<I> implements Comparable<Request<?>> {

	private static long ID = 0;

	public final RequesterReference<I> attachment;
	public final List<I> stacks = NonNullList.create();

	protected long id;

	public Request(RequesterReference<I> attachment, Collection<I> stacks) {
		this(attachment, ID++);
		this.stacks.addAll(stacks);
	}

	public Request(RequesterReference<I> attachment, I stack) {
		this(attachment, ID++);
		this.stacks.add(stack);
	}

	protected Request(RequesterReference<I> attachment, long id) {
		this.attachment = attachment;
		this.id = id;
	}

	public abstract void addStack(I stack);

	public abstract I decreaseStack(I stack);

	@Override
	public int compareTo(@Nonnull Request<?> request) {
		return Long.compare(id, request.id);
	}

}
