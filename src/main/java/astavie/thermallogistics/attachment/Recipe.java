package astavie.thermallogistics.attachment;

import astavie.thermallogistics.process.Process;
import astavie.thermallogistics.process.*;
import astavie.thermallogistics.util.RequesterReference;
import astavie.thermallogistics.util.Shared;
import astavie.thermallogistics.util.Snapshot;
import astavie.thermallogistics.util.collection.*;
import astavie.thermallogistics.util.type.Type;
import cofh.thermaldynamics.duct.attachments.servo.ServoBase;
import cofh.thermaldynamics.duct.attachments.servo.ServoItem;
import cofh.thermaldynamics.duct.item.GridItem;
import cofh.thermaldynamics.duct.item.StackMap;
import cofh.thermaldynamics.duct.tiles.DuctUnit;
import cofh.thermaldynamics.util.ListWrapper;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.function.Supplier;

public abstract class Recipe<I> implements ICrafter<I>, IProcessRequester<I> {

	public final List<RequesterReference<?>> linked = NonNullList.create();

	public boolean enabled = true;
	public int index;

	public List<I> inputs = NonNullList.create();
	public List<I> outputs = NonNullList.create();

	public Map<RequesterReference<I>, StackList<I>> requestOutput = new HashMap<>();
	public StackList<I> leftovers;
	public StackList<I> missing;
	public Map<RequesterReference<I>, StackList<I>> requestInput = new HashMap<>();

	public Process<I> process;

	// Requests
	private ServoBase parent;
	private DuctUnit<?, ?, ?> duct;
	private Supplier<StackList<I>> supplier;

	public Recipe(ServoBase parent, DuctUnit<?, ?, ?> duct, Supplier<StackList<I>> supplier, int index) {
		this.parent = parent;
		this.duct = duct;
		this.supplier = supplier;
		this.leftovers = supplier.get();
		this.missing = supplier.get();
		this.index = index;
	}

	private static <I> void applyLinkedProposal(Proposal<I> linked) {
		((ICrafter<I>) linked.me.get()).applyProposal(null, linked);
	}

	@Override
	public Collection<RequesterReference<?>> getLinked() {
		return linked;
	}

	@Override
	public boolean isLinked(RequesterReference<?> reference) {
		return linked.contains(reference);
	}

	@Override
	public void link(ICrafter<?> crafter) {
		checkLinked();
		RequesterReference<?> reference = crafter.createReference();

		if (!isLinked(reference)) {
			for (RequesterReference<?> link : linked) {
				ICrafter<?> oldie = (ICrafter<?>) link.get();
				oldie.link(reference);
				crafter.link(link);
			}

			linked.add(reference);
			crafter.link(createReference());

			markDirty();
		}
	}

	private void markDirty() {
		parent.baseTile.markChunkDirty();
	}

	@Override
	public void unlink(ICrafter<?> crafter) {
		checkLinked();
		RequesterReference<?> reference = crafter.createReference();

		for (RequesterReference<?> link : linked) {
			ICrafter<?> oldie = (ICrafter<?>) link.get();
			oldie.unlink(reference);
			crafter.unlink(link);
		}

		linked.remove(reference);
		crafter.unlink(createReference());

		markDirty();
	}

	@Override
	public void link(RequesterReference<?> reference) {
		if (!isLinked(reference)) {
			linked.add(reference);
			markDirty();
		}
	}

	@Override
	public void unlink(RequesterReference<?> reference) {
		linked.remove(reference);
		markDirty();
	}

	@Override
	public boolean isEnabled() {
		return enabled && parent.isPowered;
	}

	@Override
	public StackList<I> getOutputs() {
		return getCondensedOutputs();
	}

	public void onDisable() {
		requestOutput.clear();
		requestInput.clear();
		leftovers.clear();
		missing.clear();
	}

	@Override
	public MissingList request(IRequester<I> requester, Type<I> type, Shared<Long> amount, boolean applyMissing) {
		Snapshot.INSTANCE.clearMutated();
		MissingList missing = new MissingList();

		// First check leftovers
		long remain = leftovers.remove(type, amount.get());
		if (remain < amount.get()) {
			applyProposal(requester, new Proposal<>(createReference(), type, amount.get() - remain));
			amount.accept(remain);
		}

		// Then craft
		if (amount.get() > 0) {
			long left = amount.get();
			long timeStarted = System.currentTimeMillis();
			long qtyPerCraft = amountCrafted(type);

			while (left > 0) {
				long remove = Math.min(amount.get(), qtyPerCraft);
				Proposal<I> proposal = new Proposal<>(createReference(), type, remove);

				if (!requestInternal(type, remove, missing, proposal, new HashSet<>(), timeStarted, true)) {
					Snapshot.INSTANCE.clearMutated();
					return null;
				}

				left -= remove;

				if (applyMissing || missing.isEmpty()) {
					// Apply
					amount.accept(amount.get() - remove);

					Snapshot.INSTANCE.applyMutated();
					applyProposal(requester, proposal);
				}
			}
		}

		// Return
		Snapshot.INSTANCE.clearMutated();
		return missing;
	}

	@Override
	public boolean requestInternal(Type<I> type, long amount, MissingList missing, Proposal<I> proposal, Set<ICrafter<?>> used, long timeStarted, boolean doLinked) {
		// TODO: Uncomment this
		//if (System.currentTimeMillis() - timeStarted > ThermalLogistics.INSTANCE.calculationTimeout) {
		//	return false; // Too complex!
		//}

		if (!used.add(this)) {
			return false; // TODO: Different error for recursive
		}

		StackList<I> inputs = getCondensedInputs();
		StackList<I> network = Snapshot.INSTANCE.getMutatedStacks(duct.getGrid());

		boolean ignoreMod = parent.filter.getFlag(4);
		boolean ignoreOreDict = parent.filter.getFlag(3);
		boolean ignoreMetadata = parent.filter.getFlag(1);
		boolean ignoreNbt = parent.filter.getFlag(2);

		for (Type<I> subType : inputs.types()) {
			long subAmount = inputs.amount(subType);

			// First check existing items
			for (Type<I> compare : network.types()) {
				if (compare.isIdentical(subType, ignoreMod, ignoreOreDict, ignoreMetadata, ignoreNbt)) {
					long remaining = network.remove(compare, subAmount);

					if (remaining < subAmount) {
						proposal.children.add(new Proposal<>(null, compare, subAmount - remaining));
						subAmount = remaining;

						if (subAmount == 0) {
							break;
						}
					}
				}
			}

			// Then check crafters
			if (subAmount > 0) {
				Pair<ICrafter<I>, Type<I>> pair = process.getCrafter(subType, ignoreMod, ignoreOreDict, ignoreMetadata, ignoreNbt);

				if (pair != null) {
					ICrafter<I> crafter = pair.getLeft();
					Type<I> compare = pair.getRight();

					// First check leftovers
					StackList<I> leftovers = Snapshot.INSTANCE.getLeftovers(crafter.createReference(), supplier);
					long remain = leftovers.remove(compare, subAmount);

					if (remain < subAmount) {
						proposal.children.add(new Proposal<>(crafter.createReference(), compare, subAmount - remain));
						subAmount = remain;

						if (subAmount == 0) {
							break;
						}
					}

					// Then craft
					if (subAmount > 0) {
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
					}
				} else {
					// Not enough items
					proposal.children.add(new Proposal<>(null, subType, subAmount, true));
					missing.add(subType, subAmount);
				}
			}
		}

		// Also activate linked crafters
		if (doLinked) {
			checkLinked();
			for (RequesterReference<?> reference : linked) {
				if (!requestLinked(reference, missing, proposal, used, timeStarted)) {
					// Complex
					return false;
				}
			}
		}

		// We did it! Time to register the outputs
		used.remove(this);

		StackList<I> leftovers = Snapshot.INSTANCE.getLeftovers(createReference(), supplier);
		leftovers.addAll(getCondensedOutputs());
		if (type != null) {
			leftovers.remove(type, amount);
		}
		return true;
	}

	public boolean updateMissing() {
		boolean done = false;

		List<Request<I>> requests = new LinkedList<>();

		boolean ignoreMod = parent.filter.getFlag(4);
		boolean ignoreOreDict = parent.filter.getFlag(3);
		boolean ignoreMetadata = parent.filter.getFlag(1);
		boolean ignoreNbt = parent.filter.getFlag(2);

		for (Type<I> type : missing.types()) {
			long requested = process.request(type, missing.amount(type), requests, ignoreMod, ignoreOreDict, ignoreMetadata, ignoreNbt);
			if (requested > 0) {
				done = true;
				missing.remove(type, requested);
				markDirty();
			}
		}

		for (Request<I> request : requests) {
			if (!request.isError()) {
				requestInput.computeIfAbsent(request.source.crafter, r -> supplier.get());
				requestInput.get(request.source.crafter).add(request.type, request.amount);
			}
		}

		return done;
	}

	private <O> boolean requestLinked(RequesterReference<O> reference, MissingList missing, Proposal<I> proposal, Set<ICrafter<?>> used, long timeStarted) {
		Proposal<O> linked = new Proposal<>(reference, null, 0);
		ICrafter<O> crafter = ((ICrafter<O>) reference.get());

		if (!crafter.requestInternal(null, 0, missing, linked, used, timeStarted, false)) {
			// Complex
			return false;
		}

		proposal.linked.add(linked);
		return true;
	}

	@Override
	public void applyProposal(IRequester<I> requester, Proposal<I> proposal) {
		if (requester != null) {
			RequesterReference<I> reference = requester.createReference();
			requestOutput.computeIfAbsent(reference, r -> supplier.get());
			requestOutput.get(reference).add(proposal.type, proposal.amount);
		}

		for (Proposal<I> child : proposal.children) {
			if (child.missing) {
				missing.add(child.type, child.amount);
			} else {
				requestInput.computeIfAbsent(child.me, r -> supplier.get());
				requestInput.get(child.me).add(child.type, child.amount);
			}

			if (child.me != null) {
				((ICrafter<I>) child.me.get()).applyProposal(this, child);
			}
		}

		for (Proposal<?> linked : proposal.linked) {
			applyLinkedProposal(linked);
		}

		markDirty();
	}

	@Override
	public void applyLeftovers(StackList<I> leftovers) {
		this.leftovers = leftovers;
		markDirty();
	}

	public void check() {
		checkLinked();
		checkOutput();
		balanceLeftovers();
	}

	public void checkOutput() {
		// Check if requesters cancelled on us without telling
		for (Iterator<Map.Entry<RequesterReference<I>, StackList<I>>> iterator = requestOutput.entrySet().iterator(); iterator.hasNext(); ) {
			Map.Entry<RequesterReference<I>, StackList<I>> entry = iterator.next();
			IRequester<I> requester = entry.getKey().get();

			if (requester == null) {
				leftovers.addAll(entry.getValue());
				iterator.remove();
				continue;
			}

			StackList<I> stacks = requester.getRequestedStacks(this);
			for (Type<I> type : entry.getValue().types()) {
				long dif = entry.getValue().amount(type) - stacks.amount(type);
				if (dif > 0) {
					leftovers.add(type, dif);
					entry.getValue().remove(type, dif);
					if (entry.getValue().isEmpty())
						iterator.remove();
				}
			}
		}
	}

	private void balanceLeftovers() {
		// Get amount of recipes leftover
		long output = Long.MAX_VALUE;

		StackList<I> co = getCondensedOutputs();
		if (co.isEmpty()) {
			output = 0;
		} else for (Type<I> type : co.types()) {
			output = Math.min(output, leftovers.amount(type) / co.amount(type));
		}

		if (output == 0) {
			return;
		}

		// Get amount of recipes being requested
		long input = Long.MAX_VALUE;

		boolean ignoreMod = parent.filter.getFlag(4);
		boolean ignoreOreDict = parent.filter.getFlag(3);
		boolean ignoreMetadata = parent.filter.getFlag(1);
		boolean ignoreNbt = parent.filter.getFlag(2);

		StackList<I> ci = getCondensedInputs();
		if (ci.isEmpty()) {
			input = 0;
		} else for (Type<I> type : ci.types()) {
			long i = missing.amount(type);

			for (StackList<I> in : getRequests().values()) { // getRequests() so traveling items are removed
				i += in.amount(type, ignoreMod, ignoreOreDict, ignoreMetadata, ignoreNbt);
			}

			input = Math.min(input, i / ci.amount(type));
		}

		if (input == 0) {
			return;
		}

		// Subtract from leftovers
		long subtract = Math.min(output, input);

		for (Type<I> type : co.types()) {
			leftovers.remove(type, co.amount(type) * subtract);
		}

		// Subtract from requested
		for (Type<I> type : ci.types()) {
			long amount = ci.amount(type) * subtract;

			amount = missing.remove(type, amount);
			if (amount == 0)
				continue;

			for (Iterator<Map.Entry<RequesterReference<I>, StackList<I>>> iterator = requestInput.entrySet().iterator(); iterator.hasNext() && amount > 0; ) {
				Map.Entry<RequesterReference<I>, StackList<I>> entry = iterator.next();

				for (Type<I> compare : entry.getValue().types()) {
					if (compare.isIdentical(type, ignoreMod, ignoreOreDict, ignoreMetadata, ignoreNbt)) {
						long remain = entry.getValue().remove(compare, amount);
						long subtracted = amount - remain;

						if (subtracted > 0) {
							if (entry.getValue().isEmpty())
								iterator.remove();

							amount = remain;

							if (entry.getKey() != null) {
								IRequester<I> requester = entry.getKey().get();
								if (requester instanceof ICrafter) {
									((ICrafter<I>) entry.getKey().get()).cancel(this, compare, subtracted);
								}
							}

							if (amount == 0) {
								break;
							}
						}
					}
				}
			}
		}

		markDirty();
	}

	private StackList<I> getCondensedInputs() {
		StackList<I> list = supplier.get();
		for (I stack : inputs)
			list.add(stack);
		return list;
	}

	private StackList<I> getCondensedOutputs() {
		StackList<I> list = supplier.get();
		for (I stack : outputs)
			list.add(stack);
		return list;
	}

	@Override
	public void cancel(IRequester<I> requester, Type<I> type, long amount) {
		RequesterReference<I> reference = requester.createReference();
		if (requestOutput.containsKey(reference)) {
			StackList<I> list = requestOutput.get(reference);

			if (list != null) {
				amount -= list.remove(type, amount);

				if (list.isEmpty()) {
					requestOutput.remove(reference);
					markDirty();
				}

				leftovers.add(type, amount);
				markDirty();
			}
		}

		// TODO: Maybe this is too performance-heavy ?
		balanceLeftovers();
	}

	@Override
	public void onFail(Type<I> type, long amount) {
		onFail(null, type, amount); // Same thing
	}

	@Override
	public void onFail(RequesterReference<I> reference, Type<I> type, long amount) {
		if (requestInput.containsKey(reference)) {
			StackList<I> list = requestInput.get(reference);

			if (list != null) {
				long remain = list.remove(type, amount);
				amount -= remain;
				markDirty();

				if (list.isEmpty()) {
					requestInput.remove(reference);
				}
			}
		}

		if (amount == 0) {
			return;
		}

		boolean ignoreMod = parent.filter.getFlag(4);
		boolean ignoreOreDict = parent.filter.getFlag(3);
		boolean ignoreMetadata = parent.filter.getFlag(1);
		boolean ignoreNbt = parent.filter.getFlag(2);

		for (Type<I> t : getCondensedInputs().types()) {
			if (t.isIdentical(type, ignoreMod, ignoreOreDict, ignoreMetadata, ignoreNbt)) { // Revert missing back to input type
				missing.add(t, amount);
				break;
			}
		}
	}

	@Override
	public long reserved(IRequester<I> requester, Type<I> type) {
		return requestOutput.getOrDefault(requester.createReference(), EmptyList.getInstance()).amount(type);
	}

	@Override
	public void finish(IRequester<I> requester, Type<I> type, long amount) {
		requestOutput.getOrDefault(requester.createReference(), EmptyList.getInstance()).remove(type, amount);
		markDirty();
	}

	@Override
	public boolean referencedBy(RequesterReference<?> reference) {
		return reference.dim == parent.baseTile.world().provider.getDimension() && reference.pos.equals(parent.baseTile.getPos()) && reference.side == parent.side && reference.index == index;
	}

	@Override
	public RequesterReference<I> createReference() {
		return new RequesterReference<>(parent.baseTile.world().provider.getDimension(), parent.baseTile.getPos(), parent.side, index);
	}

	@Override
	public ItemStack getIcon() {
		return parent.getPickBlock();
	}

	@Override
	public ItemStack getTileIcon() {
		// Sorry cpw, but I need some reflection
		TileEntity tile = ReflectionHelper.getPrivateValue(ServoBase.class, parent, "myTile");
		return tile == null ? ItemStack.EMPTY : tile.getBlockType().getItem(tile.getWorld(), tile.getPos(), tile.getWorld().getBlockState(tile.getPos()));
	}

	@Override
	public BlockPos getDestination() {
		return parent.baseTile.getPos().offset(EnumFacing.byIndex(parent.side));
	}

	@Override
	public DuctUnit<?, ?, ?> getDuct() {
		return duct;
	}

	@Override
	public byte getSide() {
		return (byte) (parent.side ^ 1);
	}

	@Override
	public StackList<I> getRequestedStacks() {
		return requestInput.getOrDefault(null, supplier.get());
	}

	@Override
	public StackList<I> getRequestedStacks(ICrafter<I> crafter) {
		return requestInput.getOrDefault(crafter.createReference(), supplier.get());
	}

	@Override
	public void checkLinked() {
		for (Iterator<RequesterReference<?>> iterator = linked.iterator(); iterator.hasNext(); ) {
			IRequester<?> requester = iterator.next().get();
			if (!(requester instanceof ICrafter)) {
				iterator.remove();
				markDirty();
			} else {
				ICrafter<?> crafter = (ICrafter<?>) requester;
				if (!crafter.isLinked(createReference())) {
					iterator.remove();
					markDirty();
				}
			}
		}
	}

	@Override
	public Map<RequesterReference<I>, StackList<I>> getRequests() {
		Map<RequesterReference<I>, StackList<I>> copy = new HashMap<>();

		for (Map.Entry<RequesterReference<I>, StackList<I>> entry : requestInput.entrySet())
			copy.put(entry.getKey(), entry.getValue().copy());

		return copy;
	}

	@Override
	public void onCrafterSend(ICrafter<I> crafter, Type<I> type, long amount) {
		long remain = requestInput.getOrDefault(crafter.createReference(), supplier.get()).remove(type, amount);
		long subtracted = amount - remain;

		requestInput.computeIfAbsent(null, n -> supplier.get());
		requestInput.get(null).add(type, subtracted);

		markDirty();
	}

	// NOT APPLICABLE

	@Override
	public void addRequest(Request<I> request) {
	}

	@Override
	public boolean hasWants() {
		return false;
	}

	@Override
	public long amountRequired(Type<I> type) {
		return 0;
	}

	@Override
	public StackList<I> getLeftovers() {
		return leftovers;
	}

	public static class Item extends Recipe<ItemStack> implements IProcessRequesterItem {

		private final CrafterItem parent;

		public Item(CrafterItem parent, int index) {
			super(parent, parent.itemDuct, ItemList::new, index);
			this.parent = parent;
			this.process = new ProcessItem(this);
		}

		@Override
		public int maxSize() {
			return parent.getMaxSend();
		}

		@Override
		public boolean multiStack() {
			return ServoItem.multiStack[parent.type];
		}

		@Override
		public byte speedBoost() {
			return parent.getSpeed();
		}

		@Override
		public ListWrapper<Pair<DuctUnit, Byte>> getSources() {
			return new ListWrapperWrapper<>(parent.routesWithInsertSideList, r -> Pair.of(r.endPoint, r.getLastSide()));
		}

		@Override
		public StackList<ItemStack> getRequestedStacks() {
			StackList<ItemStack> list = super.getRequestedStacks().copy();

			StackMap map = ((GridItem) getDuct().getGrid()).travelingItems.getOrDefault(getDestination(), new StackMap());
			for (ItemStack item : map.getItems())
				list.remove(item);

			return list;
		}

		@Override
		public Map<RequesterReference<ItemStack>, StackList<ItemStack>> getRequests() {
			Map<RequesterReference<ItemStack>, StackList<ItemStack>> requests = super.getRequests();

			if (requests.containsKey(null)) {
				// Remove traveling items
				StackList<ItemStack> list = requests.get(null);

				StackMap map = ((GridItem) getDuct().getGrid()).travelingItems.getOrDefault(getDestination(), new StackMap());
				for (ItemStack item : map.getItems())
					list.remove(item);
			}

			return requests;
		}

	}

}
