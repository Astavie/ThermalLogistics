package astavie.thermallogistics.attachment;

import astavie.thermallogistics.util.RequesterReference;

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
	boolean request(int recipes);

	/**
	 * A requester doesn't need this crafter anymore
	 */
	void cancel(IRequester<I> requester, I output);

	/**
	 * A linked crafter failed to make its recipe x times.
	 */
	void cancelLinked(int recipes);

}
