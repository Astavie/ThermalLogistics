package astavie.thermallogistics.util;

import cofh.thermaldynamics.duct.item.DuctUnitItem;
import cofh.thermaldynamics.duct.item.GridItem;
import cofh.thermaldynamics.duct.item.TravelingItem;
import cofh.thermaldynamics.multiblock.IGridTile;
import cofh.thermaldynamics.multiblock.Route;
import net.minecraft.item.ItemStack;

public class TravelingItemLogistics extends TravelingItem {

	public TravelingItemLogistics(ItemStack theItem, IGridTile<DuctUnitItem, GridItem> start, Route itemPath, byte oldDirection, byte speed) {
		super(theItem, start, itemPath, oldDirection, speed);
	}

}
