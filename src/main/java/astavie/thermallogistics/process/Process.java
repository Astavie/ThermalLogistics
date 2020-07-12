package astavie.thermallogistics.process;

import astavie.thermallogistics.attachment.ICrafter;
import astavie.thermallogistics.attachment.IRequester;
import astavie.thermallogistics.util.RequesterReference;
import astavie.thermallogistics.util.Shared;
import astavie.thermallogistics.util.Snapshot;
import astavie.thermallogistics.util.collection.MissingList;
import astavie.thermallogistics.util.collection.StackList;
import astavie.thermallogistics.util.type.Type;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

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

	/**
	 * Used in crafters and terminals
	 */
	protected abstract boolean updateRetrieval(StackList<I> requests);

	/**
	 * Used in requesters
	 */
	protected abstract boolean updateWants();

	/**
	 * Used in requesters, crafters, and terminals
	 */
	protected abstract boolean attemptPull(ICrafter<I> crafter, StackList<I> stacks);

	/**
	 * Used in terminals: request items
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
			Shared<Long> shared = new Shared<>(amount);
			requestFromCrafters(requests, type, shared, true, true, false, false, false);
			amount = shared.get();
		}

		// WRAP UP

		if (amount > 0) {
			MissingList list = new MissingList();
			list.add(type, amount);
			requests.add(new Request<>(type, amount, 0, list, false));
		}

		return requests;
	}

	/**
	 * Used in crafters: request missing items
	 */
	public long request(Type<I> type, long amount, List<Request<I>> requests, boolean ignoreMod, boolean ignoreOreDict, boolean ignoreMetadata, boolean ignoreNbt) {
		long requested = 0;

		// CHECK FOR STACKS

		StackList<I> stacks = Snapshot.INSTANCE.getStacks(requester.getDuct().getGrid());
		long am = stacks.amount(type);

		long removed = Math.min(am, amount);
		if (removed > 0) {
			stacks.remove(type, removed);
			amount -= removed;

			requests.add(new Request<>(type, removed, new Source<>(requester.getSide()), 0));

			requested += removed;
		}

		// CHECK FOR CRAFTERS

		if (amount > 0) {
			Shared<Long> shared = new Shared<>(amount);
			requestFromCrafters(requests, type, shared, ignoreMod, ignoreOreDict, ignoreMetadata, ignoreNbt, true);
			requested += amount - shared.get();
		}

		return requested;
	}

	public abstract void findCrafter(Predicate<ICrafter<I>> predicate, boolean advanceCursor);

	public ICrafter<I> getCrafter(Type<I> output) {
		Shared<ICrafter<I>> shared = new Shared<>();

		findCrafter(crafter -> {
			if (crafter.getOutputs().types().contains(output)) {
				shared.accept(crafter);
				return true;
			}
			return false;
		}, true);

		return shared.get();
	}

	public Pair<ICrafter<I>, Type<I>> getCrafter(Type<I> output, Set<ICrafter<?>> used, boolean ignoreMod, boolean ignoreOreDict, boolean ignoreMetadata, boolean ignoreNbt) {
		Shared<Pair<ICrafter<I>, Type<I>>> shared = new Shared<>();

		findCrafter(crafter -> {
			if (used.contains(crafter))
				return false;
			for (Type<I> type : crafter.getOutputs().types()) {
				if (type.isIdentical(output, ignoreMod, ignoreOreDict, ignoreMetadata, ignoreNbt)) {
					shared.accept(Pair.of(crafter, type));
					return true;
				}
			}
			return false;
		}, true);

		return shared.get();
	}

	public Triple<ICrafter<I>, Type<I>, Long> getCrafter(Set<ICrafter<?>> used, Function<Type<I>, Long> amountRequired) {
		Shared<Triple<ICrafter<I>, Type<I>, Long>> shared = new Shared<>();

		findCrafter(crafter -> {
			if (used.contains(crafter))
				return false;
			for (Type<I> type : crafter.getOutputs().types()) {
				long a = amountRequired.apply(type);
				if (a > 0) {
					shared.accept(Triple.of(crafter, type, a));
					return true;
				}
			}
			return false;
		}, true);

		return shared.get();
	}

	public Pair<ICrafter<I>, Type<I>> getCrafterWithLeftovers(Type<I> output, Set<ICrafter<?>> used, boolean ignoreMod, boolean ignoreOreDict, boolean ignoreMetadata, boolean ignoreNbt) {
		Shared<Pair<ICrafter<I>, Type<I>>> shared = new Shared<>();

		findCrafter(crafter -> {
			if (used.contains(crafter))
				return false;
			for (Type<I> type : Snapshot.INSTANCE.getLeftovers(crafter.createReference()).types()) {
				if (type.isIdentical(output, ignoreMod, ignoreOreDict, ignoreMetadata, ignoreNbt)) {
					shared.accept(Pair.of(crafter, type));
					return true;
				}
			}
			return false;
		}, false);

		return shared.get();
	}

	public Triple<ICrafter<I>, Type<I>, Long> getCrafterWithLeftovers(Set<ICrafter<?>> used, Function<Type<I>, Long> amountRequired) {
		Shared<Triple<ICrafter<I>, Type<I>, Long>> shared = new Shared<>();

		findCrafter(crafter -> {
			if (used.contains(crafter))
				return false;
			for (Type<I> type : Snapshot.INSTANCE.getLeftovers(crafter.createReference()).types()) {
				long a = amountRequired.apply(type);
				if (a > 0) {
					shared.accept(Triple.of(crafter, type, a));
					return true;
				}
			}
			return false;
		}, false);

		return shared.get();
	}

	/**
	 * Used in terminal: request from crafters
	 */
	private void requestFromCrafters(List<Request<I>> requests, Type<I> type, Shared<Long> amount, boolean ignoreMod, boolean ignoreOreDict, boolean ignoreMetadata, boolean ignoreNbt, boolean applyMissing) {
		// Do the thing
		long a = amount.get();

		MissingList missing = new MissingList();
		Proposal<I> proposal = new Proposal<>(null, null, 0);

		if (!requestFirst(missing, proposal, ignoreMod, ignoreOreDict, ignoreMetadata, ignoreNbt, type, a, applyMissing)) {
			// Complex
			missing = null;
		}

		for (Proposal<I> prop : proposal.children) {
			requests.add(new Request<>(prop.type, prop.amount, new Source<>(requester.getSide(), prop.me), 0));
			a -= prop.amount;
		}

		if (!applyMissing) {
			if (missing == null) {
				// Complex
				requests.add(new Request<>(type, a, 0, null, true));
			} else if (!missing.isEmpty()) {
				// Missing
				requests.add(new Request<>(type, a, 0, missing, false));
			}

			amount.accept(0L);
		} else {
			amount.accept(a);
		}
	}

	public boolean requestInternal(MissingList missing, Proposal<I> proposal, Set<ICrafter<?>> used, long timeStarted, boolean ignoreMod, boolean ignoreOreDict, boolean ignoreMetadata, boolean ignoreNbt, Type<I> subType, long subAmount) {
		StackList<I> network = Snapshot.INSTANCE.getMutatedStacks(requester.getDuct().getGrid());

		// First check existing items
		for (Type<I> compare : network.types()) {
			if (compare.isIdentical(subType, ignoreMod, ignoreOreDict, ignoreMetadata, ignoreNbt)) {
				long remaining = network.remove(compare, subAmount);

				if (remaining < subAmount) {
					proposal.children.add(new Proposal<>(null, compare, subAmount - remaining));
					subAmount = remaining;

					if (subAmount == 0) {
						return true;
					}
				}
			}
		}

		// Then check leftovers
		if (subAmount > 0) {
			Pair<ICrafter<I>, Type<I>> pair = getCrafterWithLeftovers(subType, used, ignoreMod, ignoreOreDict, ignoreMetadata, ignoreNbt);

			if (pair != null) {
				ICrafter<I> crafter = pair.getLeft();
				Type<I> compare = pair.getRight();

				// Request leftover
				StackList<I> leftovers = Snapshot.INSTANCE.getLeftovers(crafter.createReference());
				long remain = leftovers.remove(compare, subAmount);

				if (remain < subAmount) {
					proposal.children.add(new Proposal<>(crafter.createReference(), compare, subAmount - remain));
					subAmount = remain;

					if (subAmount == 0) {
						return true;
					}
				}
			}
		}

		// Then check crafters
		if (subAmount > 0) {
			Pair<ICrafter<I>, Type<I>> pair = getCrafter(subType, used, ignoreMod, ignoreOreDict, ignoreMetadata, ignoreNbt);

			if (pair != null) {
				ICrafter<I> crafter = pair.getLeft();
				Type<I> compare = pair.getRight();

				// Craft
				long qtyPerCraft = crafter.amountCrafted(compare);

				while (subAmount > 0) {
					long remove = Math.min(subAmount, qtyPerCraft);
					Proposal<I> subProposal = new Proposal<>(crafter.createReference(), compare, remove);

					if (!crafter.requestInternal(compare, remove, missing, subProposal, used, timeStarted, true)) {
						// Complex!
						return false;
					}

					proposal.children.add(subProposal);
					subAmount -= remove;
				}
			} else {
				// Not enough items
				proposal.children.add(new Proposal<>(null, subType, subAmount, true));
				missing.add(subType, subAmount);
			}
		}

		return true;
	}

	// TODO: Merge these bottom two

	private boolean requestFirst(MissingList missing, Proposal<I> proposal, boolean ignoreMod, boolean ignoreOreDict, boolean ignoreMetadata, boolean ignoreNbt, Type<I> subType, long subAmount, boolean applyMissing) {
		Set<ICrafter<?>> used = new HashSet<>();

		Snapshot.INSTANCE.clearMutated();

		// First check leftovers
		if (subAmount > 0) {
			Pair<ICrafter<I>, Type<I>> pair = getCrafterWithLeftovers(subType, used, ignoreMod, ignoreOreDict, ignoreMetadata, ignoreNbt);

			if (pair != null) {
				ICrafter<I> crafter = pair.getLeft();
				Type<I> compare = pair.getRight();

				// Request leftover
				StackList<I> leftovers = Snapshot.INSTANCE.getLeftovers(crafter.createReference());
				long remain = leftovers.remove(compare, subAmount);

				if (remain < subAmount) {
					Proposal<I> subProposal = new Proposal<>(crafter.createReference(), compare, subAmount - remain);

					proposal.children.add(subProposal);
					subAmount = remain;

					Snapshot.INSTANCE.applyMutated();
					crafter.applyProposal(requester, subProposal);

					if (subAmount == 0) {
						return true;
					}
				}
			}
		}

		// Then check crafters
		if (subAmount > 0) {
			Pair<ICrafter<I>, Type<I>> pair = getCrafter(subType, used, ignoreMod, ignoreOreDict, ignoreMetadata, ignoreNbt);

			if (pair != null) {
				ICrafter<I> crafter = pair.getLeft();
				Type<I> compare = pair.getRight();

				// Craft
				long qtyPerCraft = crafter.amountCrafted(compare);

				while (subAmount > 0) {
					long remove = Math.min(subAmount, qtyPerCraft);
					Proposal<I> subProposal = new Proposal<>(crafter.createReference(), compare, remove);

					if (!crafter.requestInternal(compare, remove, missing, subProposal, used, System.currentTimeMillis(), true)) {
						// Complex!
						Snapshot.INSTANCE.clearMutated();
						return false;
					}

					subAmount -= remove;

					if (applyMissing || missing.isEmpty()) {
						proposal.children.add(subProposal);

						Snapshot.INSTANCE.applyMutated();
						crafter.applyProposal(requester, subProposal);
					}
				}
			} else {
				// Not enough items
				missing.add(subType, subAmount);
			}
		}

		Snapshot.INSTANCE.clearMutated();
		return true;
	}

	protected boolean requestFirstRequester(Proposal<I> proposal, Function<Type<I>, Long> amountRequired, boolean applyMissing) {
		MissingList missing = new MissingList();
		Set<ICrafter<?>> used = new HashSet<>();

		Snapshot.INSTANCE.clearMutated();

		// First check leftovers
		{
			Triple<ICrafter<I>, Type<I>, Long> pair = getCrafterWithLeftovers(used, amountRequired);

			if (pair != null) {
				long subAmount = pair.getRight();

				ICrafter<I> crafter = pair.getLeft();
				Type<I> compare = pair.getMiddle();

				// Request leftover
				StackList<I> leftovers = Snapshot.INSTANCE.getLeftovers(crafter.createReference());
				long remain = leftovers.remove(compare, subAmount);

				if (remain < subAmount) {
					Proposal<I> subProposal = new Proposal<>(crafter.createReference(), compare, subAmount - remain);

					proposal.children.add(subProposal);

					Snapshot.INSTANCE.applyMutated();
					crafter.applyProposal(requester, subProposal);

					return true;
				}
			}
		}

		// Then check crafters
		{
			Triple<ICrafter<I>, Type<I>, Long> pair = getCrafter(used, amountRequired);

			if (pair != null) {
				long subAmount = pair.getRight();

				ICrafter<I> crafter = pair.getLeft();
				Type<I> compare = pair.getMiddle();

				// Craft
				long qtyPerCraft = crafter.amountCrafted(compare);

				while (subAmount > 0) {
					long remove = Math.min(subAmount, qtyPerCraft);
					Proposal<I> subProposal = new Proposal<>(crafter.createReference(), compare, remove);

					if (!crafter.requestInternal(compare, remove, missing, subProposal, used, System.currentTimeMillis(), true)) {
						// Complex!
						Snapshot.INSTANCE.clearMutated();
						return false;
					}

					subAmount -= remove;

					if (applyMissing || missing.isEmpty()) {
						proposal.children.add(subProposal);

						Snapshot.INSTANCE.applyMutated();
						crafter.applyProposal(requester, subProposal);
					} else {
						break;
					}
				}
			}
		}

		Snapshot.INSTANCE.clearMutated();
		return true;
	}

}
