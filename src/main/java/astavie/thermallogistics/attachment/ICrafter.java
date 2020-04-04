package astavie.thermallogistics.attachment;

import astavie.thermallogistics.process.Proposal;
import astavie.thermallogistics.util.RequesterReference;
import astavie.thermallogistics.util.Shared;
import astavie.thermallogistics.util.collection.MissingList;
import astavie.thermallogistics.util.collection.StackList;
import astavie.thermallogistics.util.type.Type;

import java.util.Collection;
import java.util.Set;

public interface ICrafter<I> extends IRequester<I> {

	Collection<RequesterReference<?>> getLinked();

	boolean isLinked(RequesterReference<?> reference);

	void link(ICrafter<?> crafter);

	void unlink(ICrafter<?> crafter);

	void checkLinked();

	void link(RequesterReference<?> reference);

	void unlink(RequesterReference<?> reference);

	boolean isEnabled();

	StackList<I> getOutputs();

	StackList<I> getLeftovers();

	default long amountCrafted(Type<I> type) {
		return getOutputs().amount(type);
	}

	/**
	 * Requests an item. Linked crafters will also be notified.
	 */
	MissingList request(IRequester<I> requester, Type<I> type, Shared<Long> amount);

	boolean requestInternal(Type<I> type, long amount, MissingList missing, Proposal<I> proposal, Set<ICrafter<?>> used, long timeStarted, boolean doLinked);

	void applyProposal(IRequester<I> requester, Proposal<I> proposal);

	void applyLeftovers(StackList<I> leftovers);

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
