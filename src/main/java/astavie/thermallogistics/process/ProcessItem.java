package astavie.thermallogistics.process;

import astavie.thermallogistics.attachment.ICrafter;
import astavie.thermallogistics.util.collection.StackList;
import astavie.thermallogistics.util.type.ItemType;
import cofh.thermaldynamics.duct.Attachment;
import cofh.thermaldynamics.duct.item.DuctUnitItem;
import cofh.thermaldynamics.duct.item.TravelingItem;
import cofh.thermaldynamics.duct.tiles.DuctUnit;
import cofh.thermaldynamics.multiblock.MultiBlockGrid;
import cofh.thermaldynamics.multiblock.Route;
import cofh.thermaldynamics.util.ListWrapper;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import org.apache.commons.lang3.tuple.Pair;

public class ProcessItem extends Process<ItemStack> {

	public ProcessItem(IProcessRequester<ItemStack> requester) {
		super(requester);
	}

	@Override
	protected boolean updateRetrieval(byte endSide, StackList<ItemStack> requests) {
		ListWrapper<Pair<DuctUnit, Byte>> sources = requester.getSources(endSide);
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

			ItemStack extract = extract(endPoint, side, inv, requests, (DuctUnitItem) requester.getDuct(endSide), (byte) (endSide ^ 1));
			if (!extract.isEmpty()) {
				sources.advanceCursor();
				return true;
			}
		}

		return false;
	}

	@Override
	public boolean attemptPull(ICrafter<ItemStack> crafter, StackList<ItemStack> list) {
		for (MultiBlockGrid<?> grid : requester.getGrids()) {
			DuctUnitItem duct = (DuctUnitItem) crafter.getDuct(grid);
			if (duct == null)
				continue;

			byte side = crafter.getSide(grid);

			DuctUnitItem.Cache cache = duct.tileCache[side];
			if (cache == null)
				continue;

			IItemHandler inv = cache.getItemHandler(side ^ 1);
			if (inv == null)
				continue;

			byte endSide = requester.getSide(grid);

			ItemStack extract = extract(duct, side, inv, list, (DuctUnitItem) requester.getDuct(grid), endSide);
			if (!extract.isEmpty()) {
				crafter.finish(requester, new ItemType(extract), extract.getCount());
				requester.onCrafterSend(crafter, new ItemType(extract), extract.getCount(), endSide);
			}
		}

		return false;
	}

	private ItemStack extract(DuctUnitItem duct, byte side, IItemHandler inv, StackList<ItemStack> list, DuctUnitItem end, byte endSide) {
		Route route = duct.getRoute(end);
		if (route == null)
			return ItemStack.EMPTY;

		int maxPull = 64; // TODO:
		boolean multiStack = true; // TODO:
		byte speed = 1; // TODO:

		for (int slot = 0; slot < inv.getSlots(); slot++) {
			ItemStack item = inv.getStackInSlot(slot);
			ItemType type = new ItemType(item);
			if (!list.types().contains(type))
				continue;

			item = type.withAmount((int) Math.min(maxPull, list.amount(type)));
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
