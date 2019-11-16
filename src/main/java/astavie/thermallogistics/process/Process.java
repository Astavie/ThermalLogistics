package astavie.thermallogistics.process;

import astavie.thermallogistics.attachment.ICrafter;
import astavie.thermallogistics.attachment.IRequester;
import astavie.thermallogistics.util.Snapshot;
import astavie.thermallogistics.util.collection.StackList;
import astavie.thermallogistics.util.type.Type;
import cofh.thermaldynamics.multiblock.MultiBlockGrid;
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

	public boolean update() {
		Map<Source<I>, StackList<I>> requests = requester.getRequests();

		for (Source<I> source : requests.keySet()) {
			if (!source.isCrafter()) {
				if (updateRetrieval(source.side, requests.get(source))) return true;
				continue;
			}

			StackList<I> list = requests.get(source);
			IRequester<I> r = source.crafter.get();

			if (!(r instanceof ICrafter)) {
				// Crafter got removed
				for (Type<I> type : list.types()) {
					requester.onFail(null, source.crafter, type, list.amount(type));
				}
				return true;
			}

			ICrafter<I> crafter = (ICrafter<I>) r;

			if (!crafter.isEnabled() || !crafter.hasRouteTo(requester)) {
				// Crafter got disabled or disconnected
				for (Type<I> type : list.types()) {
					requester.onFail(null, source.crafter, type, list.amount(type));
				}
				return true;
			}

			boolean b = false;
			for (Type<I> type : list.types()) {
				long remove = list.amount(type) - crafter.reserved(requester, type);
				if (remove > 0) {
					// Crafter cancelled without telling us
					requester.onFail(null, source.crafter, type, remove);
					b = true;
				}
			}
			if (b) return true;

			if (attemptPull(crafter, list)) {
				return true;
			}
		}

		return false;
	}

	protected abstract boolean updateRetrieval(byte side, StackList<I> requests);

	protected abstract boolean attemptPull(ICrafter<I> crafter, StackList<I> stacks);

	public List<Request<I>> request(MultiBlockGrid<?> grid, Type<I> type, long amount, @Nullable Function<Type<I>, Long> func) {
		List<Request<I>> requests = new LinkedList<>();

		// CHECK FOR STACKS

		StackList<I> stacks = Snapshot.INSTANCE.getStacks(grid);

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

			requests.add(new Request<>(type, removed, new Source<>(requester.getSide(grid)), 0));
		}

		// TODO: CHECK FOR CRAFTERS

		if (amount > 0) {
			requests.add(new Request<>(type, amount, 0, Collections.singletonList(Collections.singletonList(Pair.of(type, amount)))));
		}

		return requests;
	}

}
