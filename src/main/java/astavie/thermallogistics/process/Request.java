package astavie.thermallogistics.process;

import astavie.thermallogistics.attachment.ICrafter;
import astavie.thermallogistics.util.RequesterReference;
import cofh.thermaldynamics.duct.item.TravelingItem;
import net.minecraft.util.NonNullList;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class Request<I> implements Comparable<Request<?>> {

	private static long ID = 0;

	public final RequesterReference<I> attachment;
	public final List<I> stacks = NonNullList.create();

	public final Set<RequesterReference<I>> blacklist = new HashSet<>();

	protected long id;

	public Request(RequesterReference<I> attachment) {
		this(attachment, ID++);
	}

	public Request(RequesterReference<I> attachment, I stack) {
		this(attachment, ID++);
		this.stacks.add(stack);
	}

	protected Request(RequesterReference<I> attachment, long id) {
		this.attachment = attachment;
		this.id = id;
		if (ID <= id)
			ID = id + 1;
	}

	public abstract void addStack(I stack);

	public abstract void decreaseStack(I stack);

	public abstract int getCount(I stack);

	public abstract void claim(ICrafter<I> crafter, TravelingItem item);

	@Override
	public int compareTo(@Nonnull Request<?> request) {
		return Long.compare(id, request.id);
	}

}
