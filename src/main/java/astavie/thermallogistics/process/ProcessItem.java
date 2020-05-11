package astavie.thermallogistics.process;

import astavie.thermallogistics.attachment.ICrafter;
import astavie.thermallogistics.util.Snapshot;
import astavie.thermallogistics.util.StackHandler;
import astavie.thermallogistics.util.collection.ItemList;
import astavie.thermallogistics.util.collection.StackList;
import astavie.thermallogistics.util.type.ItemType;
import astavie.thermallogistics.util.type.Type;
import cofh.thermaldynamics.duct.Attachment;
import cofh.thermaldynamics.duct.item.DuctUnitItem;
import cofh.thermaldynamics.duct.item.GridItem;
import cofh.thermaldynamics.duct.item.TravelingItem;
import cofh.thermaldynamics.duct.tiles.DuctUnit;
import cofh.thermaldynamics.multiblock.Route;
import cofh.thermaldynamics.util.ListWrapper;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import org.apache.commons.lang3.tuple.Pair;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

public class ProcessItem extends Process<ItemStack> {

	public ProcessItem(IProcessRequesterItem requester) {
		super(requester);
	}

	/**
	 * Used for terminals and crafters: pull reserved items from inventories
	 */
	@Override
	protected boolean updateRetrieval(StackList<ItemStack> requests) {
		DuctUnitItem.Cache ownCache = ((DuctUnitItem) requester.getDuct()).tileCache[requester.getSide() ^ 1];
		if (ownCache == null)
			return false;

		IItemHandler ownHandler = ownCache.getItemHandler(requester.getSide());
		if (ownHandler == null)
			return false;

		ListWrapper<Pair<DuctUnit, Byte>> sources = requester.getSources();
		for (Pair<DuctUnit, Byte> source : sources) {
			DuctUnitItem endPoint = (DuctUnitItem) source.getLeft();
			byte side = source.getRight();

			Attachment attachment = endPoint.parent.getAttachment(side);
			if (attachment != null && !attachment.canSend())
				continue;

			DuctUnitItem.Cache cache = endPoint.tileCache[side];
			if (cache == null)
				continue;

			if ((!endPoint.isInput(side) && !endPoint.isOutput(side)) || !endPoint.parent.getConnectionType(side).allowTransfer)
				continue;

			IItemHandler inv = cache.getItemHandler(side ^ 1);
			if (inv == null || inv.equals(ownHandler))
				continue;

			ItemStack extract = extract(endPoint, side, inv, requests::amount, (DuctUnitItem) requester.getDuct(), (byte) (requester.getSide() ^ 1));
			if (!extract.isEmpty()) {
				sources.advanceCursor();
				return true;
			}
		}

		return false;
	}

	/**
	 * Used for requesters: pull unreserved items from inventories
	 */
	@Override
	protected boolean updateWants() {
		byte endSide = requester.getSide();

		// Check if there are interesting items

		ItemList stacks = Snapshot.INSTANCE.getItems((GridItem) requester.getDuct().getGrid());
		if (stacks.types().stream().anyMatch(type -> stacks.amount(type) > 0 && requester.amountRequired(type) > 0)) {

			// Try items
			DuctUnitItem.Cache ownCache = ((DuctUnitItem) requester.getDuct()).tileCache[requester.getSide() ^ 1];
			if (ownCache == null)
				return false;

			IItemHandler ownHandler = ownCache.getItemHandler(requester.getSide());
			if (ownHandler == null)
				return false;

			ListWrapper<Pair<DuctUnit, Byte>> sources = requester.getSources();

			for (Pair<DuctUnit, Byte> source : sources) {
				DuctUnitItem endPoint = (DuctUnitItem) source.getLeft();
				byte side = source.getRight();

				Attachment attachment = endPoint.parent.getAttachment(side);
				if (attachment != null && !attachment.canSend())
					continue;

				DuctUnitItem.Cache cache = endPoint.tileCache[side];
				if (cache == null)
					continue;

				if ((!endPoint.isInput(side) && !endPoint.isOutput(side)) || !endPoint.parent.getConnectionType(side).allowTransfer)
					continue;

				IItemHandler inv = cache.getItemHandler(side ^ 1);
				if (inv == null || inv.equals(ownHandler))
					continue;

				ItemStack extract = extract(endPoint, side, inv, requester::amountRequired, (DuctUnitItem) requester.getDuct(), (byte) (endSide ^ 1));
				if (!extract.isEmpty()) {
					sources.advanceCursor();
					return true;
				}
			}

		} else {

			// Try crafters
			Proposal<ItemStack> proposal = new Proposal<>(null, null, 0);
			requestFirstRequester(proposal, type -> Math.min(requester.amountRequired(type), ((IProcessRequesterItem) requester).maxSize()), false);

			List<Request<ItemStack>> requests = new LinkedList<>();
			for (Proposal<ItemStack> prop : proposal.children) {
				requests.add(new Request<>(prop.type, prop.amount, new Source<>(requester.getSide(), prop.me), 0));
			}

			if (requests.size() > 0) {
				for (Request<ItemStack> request : requests) {
					requester.addRequest(request);
				}
				return true;
			}

		}

		return false;
	}

	/**
	 * Attempts to pull out requested items from crafters
	 */
	@Override
	public boolean attemptPull(ICrafter<ItemStack> crafter, StackList<ItemStack> list) {
		DuctUnitItem duct = (DuctUnitItem) crafter.getDuct();
		if (duct == null)
			return false;

		byte side = crafter.getSide();

		DuctUnitItem.Cache cache = duct.tileCache[side ^ 1];
		if (cache == null)
			return false;

		IItemHandler inv = cache.getItemHandler(side);
		if (inv == null)
			return false;

		ItemStack extract = extract(duct, (byte) (side ^ 1), inv, list::amount, (DuctUnitItem) requester.getDuct(), (byte) (requester.getSide() ^ 1));
		if (!extract.isEmpty()) {
			crafter.finish(requester, new ItemType(extract), extract.getCount());
			requester.onCrafterSend(crafter, new ItemType(extract), extract.getCount());

			return true;
		}

		return false;
	}

	@Override
	public void findCrafter(Predicate<ICrafter<ItemStack>> predicate, boolean advanceCursor) {
		ListWrapper<Pair<DuctUnit, Byte>> sources = requester.getSources();
		for (Pair<DuctUnit, Byte> source : sources) {
			DuctUnitItem endPoint = (DuctUnitItem) source.getLeft();
			byte side = source.getRight();

			Attachment attachment = endPoint.parent.getAttachment(side);
			if (attachment != null && StackHandler.forEachCrafter(attachment, predicate)) {
				if (advanceCursor) {
					sources.advanceCursor();
				}
				break;
			}

			DuctUnitItem.Cache cache = endPoint.tileCache[side];
			if (cache == null) {
				if (advanceCursor) {
					sources.advanceCursor();
				}
				continue;
			}

			if (StackHandler.forEachCrafter(cache.tile, predicate)) {
				if (advanceCursor) {
					sources.advanceCursor();
				}
				break;
			}

			if (advanceCursor) {
				// Always advance the cursor so Round-Robin works well
				sources.advanceCursor();
			}
		}
	}

	private ItemStack extract(DuctUnitItem duct, byte side, IItemHandler inv, Function<Type<ItemStack>, Long> required, DuctUnitItem end, byte endSide) {
		Route route = duct.getRoute(end);
		if (route == null)
			return ItemStack.EMPTY;

		int maxPull = ((IProcessRequesterItem) requester).maxSize();
		boolean multiStack = ((IProcessRequesterItem) requester).multiStack();
		byte speed = ((IProcessRequesterItem) requester).speedBoost();

		for (int slot = 0; slot < inv.getSlots(); slot++) {
			ItemStack item = inv.getStackInSlot(slot);
			ItemType type = new ItemType(item);

			long req = required.apply(type);
			if (req == 0)
				continue;

			item = type.withAmount((int) Math.min(maxPull, req));
			int left = StackHandler.canRouteItem(end, item, endSide, null);
			item.shrink(left);

			if (item.isEmpty())
				continue;

			maxPull = item.getCount();
			item = inv.extractItem(slot, maxPull, false);
			if (item.isEmpty())
				continue;

			// No turning back now
			route = route.copy();
			route.pathDirections.add(endSide);

			if (multiStack && item.getCount() < maxPull) {
				for (; item.getCount() < maxPull && slot < inv.getSlots(); slot++) {
					if (type.references(inv.getStackInSlot(slot))) {
						ItemStack extract = inv.extractItem(slot, maxPull - item.getCount(), false);
						if (!extract.isEmpty()) {
							item.grow(extract.getCount());
						}
					}
				}
			}

			duct.insertNewItem(new TravelingItem(item, duct, route, (byte) (side ^ 1), speed));
			return item;
		}

		return ItemStack.EMPTY;
	}

}
