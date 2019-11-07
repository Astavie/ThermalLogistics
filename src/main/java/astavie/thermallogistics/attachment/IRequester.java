package astavie.thermallogistics.attachment;

import astavie.thermallogistics.util.RequesterReference;
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
	void cancel(I item);

	/**
	 * @return The icon representing this requester
	 */
	ItemStack getIcon();

	/**
	 * @return The icon representing the block of this requester
	 */
	ItemStack getTileIcon();

}
