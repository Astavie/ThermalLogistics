package astavie.thermallogistics.attachment;

import astavie.thermallogistics.util.RequesterReference;
import astavie.thermallogistics.util.collection.StackList;
import astavie.thermallogistics.util.type.Type;
import cofh.thermaldynamics.duct.tiles.DuctUnit;
import cofh.thermaldynamics.multiblock.MultiBlockGrid;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;

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
	void onFail(@Nullable MultiBlockGrid<?> grid, @Nullable RequesterReference<I> crafter, Type<I> type, long amount);

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

	/**
	 * A crafter has sent this stack toward this requester
	 */
	void onCrafterSend(ICrafter<I> crafter, Type<I> type, long amount, byte side);

	/**
	 * @return The block where the items should be going to
	 */
	BlockPos getDestination();

	@Nullable
	DuctUnit<?, ?, ?> getDuct(MultiBlockGrid<?> grid);

	byte getSide(MultiBlockGrid<?> grid);

}
