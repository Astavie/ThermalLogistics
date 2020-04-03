package astavie.thermallogistics.process;

import astavie.thermallogistics.attachment.ICrafter;
import astavie.thermallogistics.attachment.IRequester;
import astavie.thermallogistics.util.RequesterReference;
import astavie.thermallogistics.util.Shared;
import astavie.thermallogistics.util.Snapshot;
import astavie.thermallogistics.util.collection.StackList;
import astavie.thermallogistics.util.type.Type;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public abstract class Process<I> {

	protected final IProcessRequester<I> requester;

	public Process(IProcessRequester<I> requester) {
		this.requester = requester;
	}

	public boolean update(boolean onlyCheck) {
		Map<RequesterReference<I>, StackList<I>> requests = requester.getRequests();

		// Update requests

		for (RequesterReference<I> source : requests.keySet()) {
			if (source == null) {
				if (!onlyCheck && updateRetrieval(requests.get(null))) return true;
				continue;
			}

			StackList<I> list = requests.get(source);
			IRequester<I> r = source.get();

			if (!(r instanceof ICrafter)) {
				// Crafter got removed
				for (Type<I> type : list.types()) {
					requester.onFail(source, type, list.amount(type));
				}
				return true;
			}

			ICrafter<I> crafter = (ICrafter<I>) r;

			if (!crafter.isEnabled() || !crafter.hasRouteTo(requester)) {
				// Crafter got disabled or disconnected
				for (Type<I> type : list.types()) {
					requester.onFail(source, type, list.amount(type));
				}
				return true;
			}

			boolean b = false;
			for (Type<I> type : list.types()) {
				long remove = list.amount(type) - crafter.reserved(requester, type);
				if (remove > 0) {
					// Crafter cancelled without telling us D:<
					requester.onFail(source, type, remove);
					b = true;
				}
			}
			if (b) return true;

			if (!onlyCheck && attemptPull(crafter, list)) {
				return true;
			}
		}

		// Add requests

		return !onlyCheck && requester.hasWants() && updateWants();
	}

	protected abstract boolean updateRetrieval(StackList<I> requests);

	protected abstract boolean updateWants();

	protected abstract boolean attemptPull(ICrafter<I> crafter, StackList<I> stacks);

	/**
	 * Used in terminals and crafters: request items
	 */
	public List<Request<I>> request(Type<I> type, long amount, @Nullable Function<Type<I>, Long> func) {
		List<Request<I>> requests = new LinkedList<>();

		// CHECK FOR STACKS

		StackList<I> stacks = Snapshot.INSTANCE.getStacks(requester.getDuct().getGrid());

		long am;
		if (func == null) {
			am = stacks.amount(type);
		} else {
			am = func.apply(type);
		}

		long removed = Math.min(am, amount);
		if (removed > 0) {
			stacks.remove(type, removed);
			amount -= removed;

			requests.add(new Request<>(type, removed, new Source<>(requester.getSide()), 0));
		}

		// CHECK FOR CRAFTERS

		if (amount > 0) {
			amount = requestFromCrafters(requests, type, amount);
		}

		// WRAP UP

		if (amount > 0) {
			requests.add(new Request<>(type, amount, 0, Collections.singletonList(Collections.singletonList(Pair.of(type, amount)))));
		}

		return requests;
	}

	protected abstract long requestFromCrafters(List<Request<I>> requests, Type<I> type, long amount);

	/**
	 * Request item from crafter
	 */
	protected boolean request(List<Request<I>> requests, ICrafter<I> crafter, Type<I> type, Shared<Long> amount) {
		long max = amount.get();

		// Request one by one TODO: Come up with a better system
		for (int i = 1; i <= max; i++) {
			if (crafter.request(requester, type, 1)) {
				amount.accept(max - i);
			} else {
				if (i > 1) {
					requests.add(new Request<>(type, i - 1, new Source<>(requester.getSide(), crafter.createReference()), 0));
				}
				break;
			}
		}

		return amount.get() == 0;
	}

}
