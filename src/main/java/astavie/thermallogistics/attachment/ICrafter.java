package astavie.thermallogistics.attachment;

import astavie.thermallogistics.util.RequesterReference;
import astavie.thermallogistics.util.type.Type;

import java.util.Collection;

public interface ICrafter<I> extends IRequester<I> {

	Collection<RequesterReference<?>> getLinked();

	boolean isLinked(RequesterReference<?> reference);

	void link(RequesterReference<?> reference);

	void unlink(RequesterReference<?> reference);

	boolean isEnabled();

	Collection<I> getInputs();

	Collection<I> getOutputs();

	boolean canCraft(I item);

	/**
	 * Requests an item. Linked crafters will also be notified.
	 */
	boolean request(I item);

	/**
	 * Requests this recipe x times. Will <strong>not</strong> notify linked crafters.
	 * Recommended to only use this within {@link #request(I)}.
	 */
	boolean requestLinked(int recipes);

	/**
	 * A requester doesn't need this crafter anymore
	 */
	void cancel(IRequester<I> requester, Type<I> type, long amount);

	/**
	 * A linked crafter failed to make its recipe x times.
	 */
	void cancelLinked(int recipes);

	/**
	 * @return The amount of items reserved for the requester
	 */
	long reserved(IRequester<I> requester, Type<I> type);

	void finish(IRequester<I> requester, Type<I> type, long amount);

	default boolean hasRouteTo(IRequester<I> requester) {
		return getDuct().getGrid() == requester.getDuct().getGrid();
	}

}
