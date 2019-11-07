package astavie.thermallogistics.util;

import astavie.thermallogistics.attachment.ICrafter;
import cofh.thermaldynamics.duct.Attachment;
import cofh.thermaldynamics.duct.fluid.GridFluid;
import cofh.thermaldynamics.duct.item.DuctUnitItem;
import cofh.thermaldynamics.duct.item.GridItem;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.items.IItemHandler;

import java.util.Collection;

public class Snapshot {

	public static final Snapshot INSTANCE = new Snapshot();

	private Multimap<GridItem, IItemHandler> inventories = HashMultimap.create();

	private Multimap<GridItem, ICrafter<ItemStack>> itemCrafters = HashMultimap.create();
	private Multimap<GridFluid, ICrafter<FluidStack>> fluidCrafters = HashMultimap.create();

	private Snapshot real;
	private long tick;

	private Snapshot() {
	}

	public void startExperiment() {
		real = copy();
	}

	public void endExperiment() {
		real = null;
	}

	public void cancelExperiment() {
		inventories = real.inventories;

		itemCrafters = real.itemCrafters;
		fluidCrafters = real.fluidCrafters;

		real = real.real;
	}

	private Snapshot copy() {
		Snapshot copy = new Snapshot();

		copy.inventories.putAll(inventories);

		copy.itemCrafters.putAll(itemCrafters);
		copy.fluidCrafters.putAll(fluidCrafters);

		copy.real = real;

		return copy;
	}

	private void refresh(GridItem grid) {
		World world = grid.worldGrid.worldObj;

		if (tick < world.getTotalWorldTime()) {
			tick = world.getTotalWorldTime();
		} else return;

		Collection<IItemHandler> handlers = inventories.get(grid);
		handlers.clear();

		Collection<ICrafter<ItemStack>> crafters = itemCrafters.get(grid);
		crafters.clear();

		for (DuctUnitItem duct : grid.nodeSet) {
			for (byte side = 0; side < 6; side++) {
				if ((!duct.isInput(side) && !duct.isOutput(side)) || !duct.parent.getConnectionType(side).allowTransfer)
					continue;

				DuctUnitItem.Cache cache = duct.tileCache[side];
				if (cache == null)
					continue;

				Attachment attachment = duct.parent.getAttachment(side);
				if (attachment != null) {
					if (attachment instanceof ICrafter && !crafters.contains(attachment) && ((ICrafter) attachment).isEnabled())
						//noinspection unchecked
						crafters.add((ICrafter<ItemStack>) attachment);
					if (!attachment.canSend())
						continue;
				}

				if (cache.tile instanceof ICrafter && !crafters.contains(cache.tile) && ((ICrafter) cache.tile).isEnabled())
					//noinspection unchecked
					crafters.add((ICrafter<ItemStack>) cache.tile);

				IItemHandler inv = cache.getItemHandler(side ^ 1);
				if (inv != null && !handlers.contains(inv)) {
					handlers.add(inv);
				}
			}
		}
	}

	private void refresh(GridFluid grid) {
		World world = grid.worldGrid.worldObj;

		if (tick < world.getTotalWorldTime()) {
			tick = world.getTotalWorldTime();
		} else return;

		// TODO:
	}

	public Collection<IItemHandler> getInventories(GridItem grid) {
		refresh(grid);
		return inventories.get(grid);
	}

	public Collection<ICrafter<ItemStack>> getCrafters(GridItem grid) {
		refresh(grid);
		return itemCrafters.get(grid);
	}

	public Collection<ICrafter<FluidStack>> getCrafters(GridFluid grid) {
		refresh(grid);
		return fluidCrafters.get(grid);
	}

}
