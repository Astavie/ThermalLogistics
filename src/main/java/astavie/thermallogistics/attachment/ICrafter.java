package astavie.thermallogistics.attachment;

import astavie.thermallogistics.util.RequesterReference;
import astavie.thermallogistics.util.type.Type;

import java.util.Collection;

public interface ICrafter<I> extends IRequester<I> {

	Collection<RequesterReference<?>> getLinked();

	boolean isLinked(RequesterReference<?> reference);

	void link(ICrafter<?> crafter);

	void unlink(ICrafter<?> crafter);

	void link(RequesterReference<?> reference);

	void unlink(RequesterReference<?> reference);

	boolean isEnabled();

	Collection<I> getOutputs();

	/**
	 * Requests an item. Linked crafters will also be notified.
	 */
	boolean request(IRequester<I> requester, Type<I> type, long amount);

	/**
	 * A requester doesn't need this crafter anymore
	 */
	void cancel(IRequester<I> requester, Type<I> type, long amount);

	/**
	 * @return The amount of items reserved for the requester
	 */
	long reserved(IRequester<I> requester, Type<I> type);

	void finish(IRequester<I> requester, Type<I> type, long amount);

	default boolean hasRouteTo(IRequester<I> requester) {
		return getDuct().getGrid() == requester.getDuct().getGrid();
	}

}
