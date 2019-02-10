package astavie.thermallogistics.attachment;

import astavie.thermallogistics.util.RequesterReference;
import cofh.thermaldynamics.duct.item.DuctUnitItem;
import cofh.thermaldynamics.duct.item.GridItem;
import cofh.thermaldynamics.multiblock.IGridTile;
import cofh.thermaldynamics.multiblock.Route;
import cofh.thermaldynamics.util.ListWrapper;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;

import java.util.List;

public interface IRequester<I> {

	List<I> getInputFrom(IRequester<I> requester);

	List<I> getOutputTo(IRequester<I> requester);

	boolean isEnabled();

	int amountRequired(I stack);

	IGridTile getDuct();

	TileEntity getTile();

	byte getSide();

	TileEntity getCachedTile();

	ItemStack getIcon();

	void markDirty();

	int tickDelay();

	default ItemStack getTileIcon() {
		TileEntity myTile = getCachedTile();

		//noinspection deprecation
		return myTile == null ? ItemStack.EMPTY : myTile.getBlockType().getItem(myTile.getWorld(), myTile.getPos(), myTile.getWorld().getBlockState(myTile.getPos()));
	}

	default RequesterReference<I> getReference() {
		return new RequesterReference<>(getTile().getWorld().provider.getDimension(), getTile().getPos(), getSide(), getIndex());
	}

	// Crafter
	void onFinishCrafting(IRequester<I> requester, I stack);

	void onFinishCrafting(int index, int recipes);

	int getIndex();

	// Multiplexer
	void setIndex(int index);

	// ItemStack
	int getMaxSend();

	byte getSpeed();

	ListWrapper<Route<DuctUnitItem, GridItem>> getRoutes();

	boolean hasMultiStack();

	// FluidStack
	float getThrottle();

}
