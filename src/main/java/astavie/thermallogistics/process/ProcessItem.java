package astavie.thermallogistics.process;

import astavie.thermallogistics.attachment.IRequester;
import cofh.core.util.helpers.InventoryHelper;
import cofh.core.util.helpers.ItemHelper;
import cofh.thermaldynamics.duct.Attachment;
import cofh.thermaldynamics.duct.item.DuctUnitItem;
import cofh.thermaldynamics.duct.item.GridItem;
import cofh.thermaldynamics.duct.item.TravelingItem;
import cofh.thermaldynamics.multiblock.Route;
import cofh.thermaldynamics.util.ListWrapper;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.items.IItemHandler;

public class ProcessItem extends Process<ItemStack> {

	public ProcessItem(IRequester<ItemStack> requester) {
		super(requester);
	}

	@Override
	public void tick() {
		TileEntity tile = requester.getCachedTile();
		if (tile == null)
			return;

		IItemHandler handler = InventoryHelper.getItemHandlerCap(tile, EnumFacing.byIndex(requester.getSide() ^ 1));
		if (handler == null)
			return;

		ListWrapper<Route<DuctUnitItem, GridItem>> routes = requester.getRoutes();

		for (Route route : routes) {
			DuctUnitItem endPoint = (DuctUnitItem) route.endPoint;

			int i = route.getLastSide();

			Attachment attachment = endPoint.parent.getAttachment(i);
			if (attachment != null && !attachment.canSend()) {
				continue;
			}

			DuctUnitItem.Cache cache = endPoint.tileCache[i];

			if (cache == null || (!endPoint.isInput(i) && !endPoint.isOutput(i)) || !endPoint.parent.getConnectionType(i).allowTransfer)
				continue;

			IItemHandler inv = cache.getItemHandler(i ^ 1);
			if (inv == null) {
				continue;
			}

			for (int slot = 0; slot < inv.getSlots(); slot++) {
				ItemStack item = inv.getStackInSlot(slot);
				if (item.isEmpty())
					continue;

				item = ItemHelper.cloneStack(item, requester.hasMultiStack() ? Integer.MAX_VALUE : item.getCount());
				if (item.isEmpty())
					continue;

				if (!cache.filter.matchesFilter(item))
					continue;

				int amount = requester.amountRequired(item);
				if (amount == 0)
					continue;

				ItemStack remainder = item.copy();

				if (remainder.getCount() <= amount) {
					remainder = InventoryHelper.insertStackIntoInventory(handler, remainder, true);
				} else {
					ItemStack remaining = InventoryHelper.insertStackIntoInventory(handler, remainder.splitStack(amount), true);
					if (!remaining.isEmpty()) {
						remainder.grow(remaining.getCount());
					}
				}


				if (!remainder.isEmpty())
					item.shrink(remainder.getCount());

				if (item.getCount() <= 0)
					continue;

				int remain = ((DuctUnitItem) requester.getDuct()).canRouteItem(item, requester.getSide());
				if (remain == -1)
					continue;

				item.shrink(remain);

				Route route1 = endPoint.getRoute(requester.getDuct());
				if (route1 == null)
					continue;

				int maxStackSize = item.getCount();
				item = inv.extractItem(slot, maxStackSize, false);
				if (item.isEmpty())
					continue;

				// No turning back now
				route1 = route1.copy();
				route1.pathDirections.add(requester.getSide());

				if (requester.hasMultiStack() && item.getCount() < maxStackSize) {
					for (; item.getCount() < maxStackSize && slot < inv.getSlots(); slot++) {
						if (ItemHelper.itemsEqualWithMetadata(inv.getStackInSlot(slot), item, true)) {
							ItemStack extract = inv.extractItem(slot, maxStackSize - item.getCount(), false);
							if (!extract.isEmpty()) {
								item.grow(extract.getCount());
							}
						}
					}
				}

				endPoint.insertNewItem(new TravelingItem(item, endPoint, route1, (byte) (i ^ 1), requester.getSpeed()));
				routes.advanceCursor();
				return;
			}
		}
	}

}
