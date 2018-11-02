package astavie.thermallogistics.util;

import astavie.thermallogistics.attachment.CrafterFluid;
import astavie.thermallogistics.attachment.CrafterItem;
import astavie.thermallogistics.process.ProcessFluid;
import astavie.thermallogistics.process.ProcessItem;
import cofh.core.util.helpers.FluidHelper;
import cofh.core.util.helpers.InventoryHelper;
import cofh.core.util.helpers.ItemHelper;
import cofh.thermaldynamics.duct.Attachment;
import cofh.thermaldynamics.duct.attachments.servo.ServoItem;
import cofh.thermaldynamics.duct.fluid.DuctUnitFluid;
import cofh.thermaldynamics.duct.fluid.GridFluid;
import cofh.thermaldynamics.duct.item.*;
import cofh.thermaldynamics.duct.tiles.DuctUnit;
import cofh.thermaldynamics.multiblock.MultiBlockGrid;
import cofh.thermaldynamics.multiblock.Route;
import com.google.common.collect.Lists;
import gnu.trove.iterator.TObjectIntIterator;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public class NetworkUtils {

	// ITEM TRANSFER
	private static ItemStack simTransfer(DuctUnitItem duct, int side, ItemStack insertingItem, boolean ignoreProcesses) {
		if (insertingItem.isEmpty())
			return ItemStack.EMPTY;
		DuctUnitItem.Cache cache = duct.tileCache[side];

		if (duct.getGrid() == null || cache == null)
			return insertingItem;
		boolean routeItems = cache.filter.shouldIncRouteItems();
		int maxStock = cache.filter.getMaxStock();

		IItemHandler itemHandler = cache.getItemHandler(side ^ 1);
		if (!routeItems)
			return DuctUnitItem.simulateInsertItemStackIntoInventory(itemHandler, insertingItem, side ^ 1, maxStock);

		StackMap travelingItems = new StackMap();
		StackMap map = duct.getGrid().travelingItems.get(duct.pos().offset(EnumFacing.VALUES[side]));
		if (map != null)
			for (ItemStack stack : map.getItems())
				travelingItems.addItemstack(stack, side);

		if (!ignoreProcesses) {
			Set<ProcessItem> processes = new HashSet<>();
			for (CrafterItem crafter : getAttachments(duct.getGrid(), CrafterItem.class)) {
				processes.addAll(crafter.processes);
				for (Pair<ItemStack, IDestination<DuctUnitItem, ItemStack>> registry : crafter.registry)
					if (registry.getLeft() != null && registry.getRight().getDuct() == duct && registry.getRight().getSide() == side)
						travelingItems.addItemstack(registry.getLeft(), side);
			}
			for (ProcessItem process : processes)
				process.getResult(duct, side, travelingItems);
		}

		if (travelingItems.isEmpty())
			return DuctUnitItem.simulateInsertItemStackIntoInventory(itemHandler, insertingItem, side ^ 1, maxStock);
		if (travelingItems.size() == 1) {
			if (ItemHelper.itemsIdentical(insertingItem, travelingItems.getItems().next())) {
				insertingItem.grow(travelingItems.getItems().next().getCount());
				return DuctUnitItem.simulateInsertItemStackIntoInventory(itemHandler, insertingItem, side ^ 1, maxStock);
			}
		} else {
			int s = 0;
			for (ItemStack travelingItem : travelingItems.getItems()) {
				if (!ItemHelper.itemsIdentical(insertingItem, travelingItem)) {
					s = -1;
					break;
				} else
					s += travelingItem.getCount();
			}
			if (s >= 0) {
				insertingItem.grow(s);
				return DuctUnitItem.simulateInsertItemStackIntoInventory(itemHandler, insertingItem, side ^ 1, maxStock);
			}
		}
		SimulatedInv simulatedInv = SimulatedInv.wrapHandler(itemHandler);

		for (TObjectIntIterator<StackMap.ItemEntry> iterator = travelingItems.iterator(); iterator.hasNext(); ) {
			iterator.advance();
			StackMap.ItemEntry itemEntry = iterator.key();
			if (!InventoryHelper.insertStackIntoInventory(simulatedInv, itemEntry.toItemStack(iterator.value()), false).isEmpty() && ItemHelper.itemsIdentical(insertingItem, itemEntry.toItemStack(iterator.value())))
				return insertingItem;
		}
		return DuctUnitItem.simulateInsertItemStackIntoInventory(simulatedInv, insertingItem, side ^ 1, maxStock);
	}

	public static ItemStack maxTransfer(ItemStack item, DuctUnitItem end, byte endSide, int type, boolean ignoreProcesses) {
		item = ItemHelper.cloneStack(item, Math.min(item.getCount(), ServoItem.maxSize[type]));
		if (ServoItem.multiStack[type] && item.getCount() > item.getMaxStackSize())
			item.setCount(item.getMaxStackSize());
		if (item.isEmpty())
			return item;

		ItemStack remainder = NetworkUtils.simTransfer(end, endSide, item.copy(), ignoreProcesses);
		if (!remainder.isEmpty())
			item.shrink(remainder.getCount());
		return item;
	}

	public static TravelingItem transfer(int slot, DuctUnitItem start, byte startSide, DuctUnitItem end, byte endSide, Route route, int total, int type, boolean ignoreProcesses) {
		IItemHandler inv = start.tileCache[startSide].getItemHandler(startSide ^ 1);
		ItemStack item = inv.getStackInSlot(slot).copy();
		if (total > 0 && total < item.getCount())
			item.setCount(total);

		item = maxTransfer(item, end, endSide, type, ignoreProcesses);
		if (item.isEmpty())
			return null;

		int max = item.getCount();
		item = inv.extractItem(slot, max, false);
		if (item.isEmpty())
			return null;

		// Alright, let's do this!
		route = route.copy();
		route.pathDirections.add(endSide);

		if (ServoItem.multiStack[type] && item.getCount() < max)
			for (; item.getCount() < max && slot < inv.getSlots(); slot++)
				if (ItemHelper.itemsIdentical(inv.getStackInSlot(slot), item)) {
					ItemStack extract = inv.extractItem(slot, max - item.getCount(), false);
					if (!extract.isEmpty())
						item.grow(extract.getCount());
				}

		TravelingItem ti = new TravelingItem(item, start, route, (byte) (startSide ^ 1), ServoItem.speedBoost[type]);
		start.insertNewItem(ti);
		return ti;
	}

	public static boolean isEmpty(IItemHandler handler) {
		for (int i = 0; i < handler.getSlots(); i++)
			if (!handler.extractItem(i, Integer.MAX_VALUE, true).isEmpty())
				return false;
		return true;
	}

	public static boolean isEmpty(IFluidHandler handler) {
		FluidStack stack = handler.drain(Integer.MAX_VALUE, false);
		return stack == null || stack.amount == 0;
	}

	public static ItemStack extract(IItemHandler handler, ItemStack stack) {
		for (int i = 0; i < handler.getSlots(); i++) {
			ItemStack item = handler.extractItem(i, stack.getCount(), true);
			if (ItemHelper.itemsIdentical(item, stack))
				return handler.extractItem(i, item.getCount(), false);
		}
		return ItemStack.EMPTY;
	}

	// TERMINAL UTILS
	public static NonNullList<Triple<ItemStack, Long, Boolean>> getItems(GridItem grid) {
		NonNullList<Triple<ItemStack, Long, Boolean>> output = NonNullList.create();
		for (DuctUnitItem start : grid.nodeSet) {
			for (byte side = 0; side < 6; side++) {
				if ((!start.isInput(side) && !start.isOutput(side)) || !start.parent.getConnectionType(side).allowTransfer)
					continue;

				Attachment attachment = start.parent.getAttachment(side);
				if (attachment != null) {
					if (attachment instanceof CrafterItem) {
						a:
						for (ItemStack out : ((CrafterItem) attachment).outputs) {
							if (out.isEmpty())
								continue;
							for (int i = 0; i < output.size(); i++) {
								Triple<ItemStack, Long, Boolean> stack = output.get(i);
								if (!ItemHelper.itemsIdentical(out, stack.getLeft()))
									continue;
								if (!stack.getRight())
									output.set(i, Triple.of(stack.getLeft(), stack.getMiddle(), true));
								continue a;
							}
							output.add(Triple.of(out, 0L, true));
						}
						continue;
					} else if (!attachment.canSend())
						continue;
				}

				DuctUnitItem.Cache cache = start.tileCache[side];
				if (cache == null)
					continue;

				IItemHandler inv = cache.getItemHandler(side ^ 1);
				if (inv == null)
					continue;

				a:
				for (int slot = 0; slot < inv.getSlots(); slot++) {
					ItemStack extract = inv.extractItem(slot, inv.getSlotLimit(slot), true);
					if (extract.isEmpty())
						continue;

					for (int i = 0; i < output.size(); i++) {
						Triple<ItemStack, Long, Boolean> stack = output.get(i);
						if (!ItemHelper.itemsIdentical(extract, stack.getLeft()))
							continue;
						output.set(i, Triple.of(stack.getLeft(), stack.getMiddle() + extract.getCount(), stack.getRight()));
						continue a;
					}
					output.add(Triple.of(extract, (long) extract.getCount(), false));
				}
			}
		}
		return output;
	}

	public static Set<FluidStack> getFluids(GridFluid grid) {
		Set<FluidStack> output = new HashSet<>();
		for (DuctUnitFluid start : grid.nodeSet) {
			a:
			for (byte k = 0; k < 6; k++) {
				int side = (k + start.internalSideCounter) % 6;
				DuctUnitFluid.Cache cache = start.tileCache[side];
				if (cache == null || (!start.isOutput(side) && !start.isInput(side)))
					continue;

				Attachment attachment = start.parent.getAttachment(side);
				if (attachment != null && !attachment.canSend())
					continue;

				IFluidHandler handler = cache.getHandler(side ^ 1);
				if (handler == null)
					continue;

				FluidStack extract = handler.drain(Integer.MAX_VALUE, false);
				if (extract != null) {
					for (FluidStack stack : output) {
						if (!FluidHelper.isFluidEqual(extract, stack))
							continue;
						stack.amount += extract.amount;
						continue a;
					}
					output.add(extract);
				}
			}
		}
		return output;
	}

	// FLUID TRANSFER
	public static long getFluid(GridFluid grid, FluidStack fluid) {
		if (!grid.hasValidFluid() || !FluidHelper.isFluidEqual(grid.getFluid(), fluid))
			return 0;

		long amt = grid.getFluid().amount;
		for (DuctUnitFluid duct : grid.nodeSet) {
			for (byte k = 0; k < 6; k++) {
				int side = (k + duct.internalSideCounter) % 6;
				DuctUnitFluid.Cache cache = duct.tileCache[side];
				if (cache == null || (!duct.isOutput(side) && !duct.isInput(side)))
					continue;

				Attachment attachment = duct.parent.getAttachment(side);
				if (attachment != null) {
					if (attachment instanceof CrafterFluid) {
						for (ProcessFluid process: ((CrafterFluid) attachment).processes)
							amt -= process.waiting(fluid);
						continue;
					}
					if (!attachment.canSend())
						continue;
				}

				IFluidHandler handler = cache.getHandler(side ^ 1);
				if (handler == null)
					continue;

				FluidStack extract = handler.drain(Integer.MAX_VALUE, false);
				if (FluidHelper.isFluidEqual(extract, fluid))
					amt += extract.amount;
			}
		}
		return amt;
	}

	// MANAGEMENT
	public static List<Route<DuctUnitItem, GridItem>> getRoutes(DuctUnitItem end) {
		Stream<Route<DuctUnitItem, GridItem>> destinations = ServoItem.getRoutesWithDestinations(end.getCache().outputRoutes);
		List<Route<DuctUnitItem, GridItem>> routes = Lists.newLinkedList();
		destinations.forEach(routes::add);
		return routes;
	}

	public static List<Route<DuctUnitItem, GridItem>> getRoutes(DuctUnitItem end, byte side) {
		List<Route<DuctUnitItem, GridItem>> routes = getRoutes(end);
		routes.removeIf(route -> route.endPoint == end && route.getLastSide() == side);
		return routes;
	}

	@SuppressWarnings("unchecked")
	private static <T extends DuctUnit, C extends Attachment> Set<C> getAttachments(MultiBlockGrid<T> grid, Class<C> compare) {
		Set<C> output = new HashSet<>();
		for (T start : grid.nodeSet) {
			for (byte side = 0; side < 6; side++) {
				if ((!start.isInput(side) && !start.isOutput(side)) || !start.parent.getConnectionType(side).allowTransfer)
					continue;

				Attachment attachment = start.parent.getAttachment(side);
				if (compare.isInstance(attachment))
					output.add((C) attachment);
			}
		}
		return output;
	}

}
