package astavie.thermallogistics.attachment;

import astavie.thermallogistics.util.RequesterReference;
import astavie.thermallogistics.util.collection.StackList;
import astavie.thermallogistics.util.type.Type;
import cofh.thermaldynamics.duct.tiles.DuctUnit;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;

public interface IRequester<I> {

	/**
	 * @return If the reference is pointing to this requester
	 */
	boolean referencedBy(RequesterReference<?> reference);

	/**
	 * @return A reference pointing to this
	 */
	RequesterReference<I> createReference();

	/**
	 * @return The icon representing this requester
	 */
	ItemStack getIcon();

	/**
	 * @return The icon representing the block of this requester
	 */
	ItemStack getTileIcon();

	/**
	 * @return The block where the items should be going to
	 */
	BlockPos getDestination();

	DuctUnit<?, ?, ?> getDuct();

	byte getSide();

	/**
	 * @return The stacks this requester is currently waiting on, <strong>excluding</strong> stacks from crafters
	 */
	StackList<I> getRequestedStacks();

	/**
	 * A requested item isn't available anymore
	 */
	void onFail(Type<I> type, long amount);

}
