package astavie.thermallogistics.attachment;

import astavie.thermallogistics.util.RequesterReference;
import cofh.thermaldynamics.duct.item.DuctUnitItem;
import cofh.thermaldynamics.duct.item.GridItem;
import cofh.thermaldynamics.multiblock.IGridTileRoute;
import cofh.thermaldynamics.multiblock.Route;
import cofh.thermaldynamics.util.ListWrapper;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;

import java.util.List;

public interface IRequester<I> {

	List<I> getInputFrom(IRequester<I> requester);

	List<I> getOutputTo(IRequester<I> requester);

	int amountRequired(I stack);

	int getMaxSend();

	IGridTileRoute getDuct();

	TileEntity getTile();

	byte getSide();

	byte getSpeed();

	ListWrapper<Route<DuctUnitItem, GridItem>> getRoutes();

	boolean hasMultiStack();

	TileEntity getCachedTile();

	ItemStack getIcon();

	void onFinishCrafting(IRequester<I> requester, I stack);

	void onExtract(I stack);

	default ItemStack getTileIcon() {
		TileEntity myTile = getCachedTile();

		//noinspection deprecation
		return myTile == null ? ItemStack.EMPTY : myTile.getBlockType().getItem(myTile.getWorld(), myTile.getPos(), myTile.getWorld().getBlockState(myTile.getPos()));
	}

	default RequesterReference<I> getReference() {
		return new RequesterReference<>(getTile().getWorld().provider.getDimension(), getTile().getPos(), getSide());
	}

}
