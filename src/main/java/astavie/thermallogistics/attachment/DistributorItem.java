package astavie.thermallogistics.attachment;

import astavie.thermallogistics.ThermalLogistics;
import astavie.thermallogistics.client.TLTextures;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.vec.Translation;
import codechicken.lib.vec.Vector3;
import codechicken.lib.vec.uv.IconTransformation;
import cofh.thermaldynamics.duct.Attachment;
import cofh.thermaldynamics.duct.attachments.ConnectionBase;
import cofh.thermaldynamics.duct.attachments.servo.ServoItem;
import cofh.thermaldynamics.duct.item.DuctUnitItem;
import cofh.thermaldynamics.duct.item.GridItem;
import cofh.thermaldynamics.duct.item.TravelingItem;
import cofh.thermaldynamics.duct.tiles.TileGrid;
import cofh.thermaldynamics.multiblock.Route;
import cofh.thermaldynamics.render.RenderDuct;
import cofh.thermaldynamics.util.ListWrapper;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.items.IItemHandler;

public class DistributorItem extends ServoItem {

	public static final ResourceLocation ID = new ResourceLocation(ThermalLogistics.MOD_ID, "distributor_item");

	public DistributorItem(TileGrid tile, byte side, int type) {
		super(tile, side, type);
	}

	public DistributorItem(TileGrid tile, byte side) {
		super(tile, side);
	}

	@Override
	public String getInfo() {
		return "tab." + ThermalLogistics.MOD_ID + ".distributorItem";
	}

	@Override
	public ResourceLocation getId() {
		return ID;
	}

	@Override
	public ItemStack getPickBlock() {
		return new ItemStack(ThermalLogistics.Items.distributor, 1, type);
	}

	@Override
	public String getName() {
		return getPickBlock().getTranslationKey() + ".name";
	}

	@Override
	public boolean render(IBlockAccess world, BlockRenderLayer layer, CCRenderState ccRenderState) {
		if (layer != BlockRenderLayer.SOLID)
			return false;
		Translation trans = Vector3.fromTileCenter(baseTile).translation();
		RenderDuct.modelConnection[isPowered ? 1 : 2][side].render(ccRenderState, trans, new IconTransformation(TLTextures.DISTRIBUTOR[stuffed ? 1 : 0][type]));
		return true;
	}

	public static TravelingItem findRouteForItem(ItemStack item, ListWrapper<Route<DuctUnitItem, GridItem>> routes, DuctUnitItem duct, int side, int maxRange, byte speed) {
		if (item.isEmpty() || item.getCount() == 0)
			return null;

		item = item.copy();

		// First check filters
		for (Route<DuctUnitItem, GridItem> outputRoute : routes) {
			if (outputRoute.pathDirections.size() <= maxRange) {
				Attachment attachment = outputRoute.endPoint.parent.getAttachment(outputRoute.getLastSide());
				if (!(attachment instanceof ConnectionBase) || !((ConnectionBase) attachment).isFilter())
					continue;

				int amountRemaining = outputRoute.endPoint.canRouteItem(item, outputRoute.getLastSide());
				if (amountRemaining != -1) {
					int stackSize = item.getCount() - amountRemaining;
					if (stackSize <= 0)
						continue;

					Route itemRoute = outputRoute.copy();
					item.setCount(stackSize);
					return new TravelingItem(item, duct, itemRoute, (byte) (side ^ 1), speed);
				}
			}
		}

		// Then check everything
		for (Route<DuctUnitItem, GridItem> outputRoute : routes) {
			if (outputRoute.pathDirections.size() <= maxRange) {
				int amountRemaining = outputRoute.endPoint.canRouteItem(item, outputRoute.getLastSide());
				if (amountRemaining != -1) {
					int stackSize = item.getCount() - amountRemaining;
					if (stackSize <= 0)
						continue;

					Route itemRoute = outputRoute.copy();
					item.setCount(stackSize);
					return new TravelingItem(item, duct, itemRoute, (byte) (side ^ 1), speed);
				}
			}
		}

		return null;
	}

	@Override
	public TravelingItem getRouteForItem(ItemStack item) {
		if (!verifyCache() || item.isEmpty() || item.getCount() == 0)
			return null;

		item = item.copy();

		IItemHandler handler = getCachedInv();

		// First check filters
		for (Route<DuctUnitItem, GridItem> outputRoute : routesWithInsertSideList) {
			if (outputRoute.pathDirections.size() <= getMaxRange()) {
				DuctUnitItem.Cache c = outputRoute.endPoint.tileCache[outputRoute.getLastSide()];
				if (c == null)
					continue;

				IItemHandler i = c.getItemHandler(outputRoute.getLastSide() ^ 1);
				if (i == handler)
					continue;

				Attachment attachment = outputRoute.endPoint.parent.getAttachment(outputRoute.getLastSide());
				if (!(attachment instanceof ConnectionBase) || !((ConnectionBase) attachment).isFilter())
					continue;

				int amountRemaining = outputRoute.endPoint.canRouteItem(item, outputRoute.getLastSide());
				if (amountRemaining != -1) {
					int stackSize = item.getCount() - amountRemaining;
					if (stackSize <= 0)
						continue;

					Route itemRoute = outputRoute.copy();
					item.setCount(stackSize);
					return new TravelingItem(item, itemDuct, itemRoute, (byte) (side ^ 1), getSpeed());
				}
			}
		}

		// Then check everything
		for (Route<DuctUnitItem, GridItem> outputRoute : routesWithInsertSideList) {
			if (outputRoute.pathDirections.size() <= getMaxRange()) {
				DuctUnitItem.Cache c = outputRoute.endPoint.tileCache[outputRoute.getLastSide()];
				if (c == null)
					continue;

				IItemHandler i = c.getItemHandler(outputRoute.getLastSide() ^ 1);
				if (i == handler)
					continue;

				int amountRemaining = outputRoute.endPoint.canRouteItem(item, outputRoute.getLastSide());
				if (amountRemaining != -1) {
					int stackSize = item.getCount() - amountRemaining;
					if (stackSize <= 0)
						continue;

					Route itemRoute = outputRoute.copy();
					item.setCount(stackSize);
					return new TravelingItem(item, itemDuct, itemRoute, (byte) (side ^ 1), getSpeed());
				}
			}
		}

		return null;
	}

}
