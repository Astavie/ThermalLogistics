package astavie.thermallogistics.process;

import astavie.thermallogistics.attachment.ICrafter;
import astavie.thermallogistics.util.Shared;
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

public class ProcessItem extends Process<ItemStack> {

	public ProcessItem(IProcessRequesterItem requester) {
		super(requester);
	}

	/**
	 * Used for terminals and crafters: pull reserved items from inventories
	 */
	@Override
	protected boolean updateRetrieval(StackList<ItemStack> requests) {
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
			if (inv == null)
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

		ListWrapper<Pair<DuctUnit, Byte>> sources = requester.getSources();

		// Check if there are interesting items

		ItemList stacks = Snapshot.INSTANCE.getItems((GridItem) requester.getDuct().getGrid());
		if (stacks.types().stream().anyMatch(type -> stacks.amount(type) > 0 && requester.amountRequired(type) > 0)) {

			// Try items
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
				if (inv == null)
					continue;

				ItemStack extract = extract(endPoint, side, inv, requester::amountRequired, (DuctUnitItem) requester.getDuct(), (byte) (endSide ^ 1));
				if (!extract.isEmpty()) {
					sources.advanceCursor();
					return true;
				}
			}

		} else {

			// Try crafters

			for (Pair<DuctUnit, Byte> source : sources) {
				DuctUnitItem endPoint = (DuctUnitItem) source.getLeft();
				byte side = source.getRight();

				Attachment attachment = endPoint.parent.getAttachment(side);
				if (attachment != null) {
					if (StackHandler.forEachCrafter(attachment, this::requestFromCrafter)) {
						sources.advanceCursor();
						break;
					}
					if (!attachment.canSend())
						continue;
				}

				DuctUnitItem.Cache cache = endPoint.tileCache[side];
				if (cache == null)
					continue;

				if (StackHandler.forEachCrafter(cache.tile, this::requestFromCrafter)) {
					sources.advanceCursor();
					break;
				}
			}

		}

		return false;
	}

	/**
	 * Used in requester: request from crafter
	 */
	private boolean requestFromCrafter(ICrafter<ItemStack> crafter) {
		for (Type<ItemStack> type : crafter.getOutputs().types()) {
			long amount = Math.min(requester.amountRequired(type), ((IProcessRequesterItem) requester).maxSize());

			if (amount == 0)
				continue;

			// TODO: Check if item fits

			Shared<Long> shared = new Shared<>(amount);

			List<Request<ItemStack>> requests = new LinkedList<>();
			request(requests, crafter, type, shared);

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

	/**
	 * Used in terminal: request from crafters
	 */
	@Override
	protected void requestFromCrafters(List<Request<ItemStack>> requests, Type<ItemStack> type, Shared<Long> amount) {
		ListWrapper<Pair<DuctUnit, Byte>> sources = requester.getSources();
		for (Pair<DuctUnit, Byte> source : sources) {
			DuctUnitItem endPoint = (DuctUnitItem) source.getLeft();
			byte side = source.getRight();

			Attachment attachment = endPoint.parent.getAttachment(side);
			if (attachment != null && StackHandler.forEachCrafter(attachment, (ICrafter<ItemStack> c) -> request(requests, c, type, amount))) {
				sources.advanceCursor();
				break;
			}

			DuctUnitItem.Cache cache = endPoint.tileCache[side];
			if (cache == null) {
				continue;
			}

			if (StackHandler.forEachCrafter(cache.tile, (ICrafter<ItemStack> c) -> request(requests, c, type, amount))) {
				sources.advanceCursor();
				break;
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

			// TODO: Check if item fits

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
