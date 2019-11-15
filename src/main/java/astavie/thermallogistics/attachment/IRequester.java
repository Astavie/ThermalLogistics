package astavie.thermallogistics.attachment;

import astavie.thermallogistics.util.RequesterReference;
import astavie.thermallogistics.util.collection.StackList;
import astavie.thermallogistics.util.type.Type;
import cofh.thermaldynamics.multiblock.MultiBlockGrid;
import net.minecraft.item.ItemStack;

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
	 * A request has failed and an item is not able to be crafted
	 */
	void cancel(MultiBlockGrid<?> grid, Type<I> type, long amount);

	/**
	 * @return The icon representing this requester
	 */
	ItemStack getIcon();

	/**
	 * @return The icon representing the block of this requester
	 */
	ItemStack getTileIcon();

	/**
	 * @return The stacks this requester is currently waiting on, <strong>excluding</strong> stacks from crafters
	 */
	StackList<I> getRequestedStacks(MultiBlockGrid<?> grid);

}
