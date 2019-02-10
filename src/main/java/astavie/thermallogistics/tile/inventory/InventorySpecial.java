package astavie.thermallogistics.tile.inventory;

import codechicken.lib.inventory.InventorySimple;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class InventorySpecial extends InventorySimple {

	private final Predicate<ItemStack> predicate;
	private final Consumer<ItemStack> consumer;

	public InventorySpecial(int limit, Predicate<ItemStack> predicate, Consumer<ItemStack> consumer) {
		super(1, limit);
		this.predicate = predicate;
		this.consumer = consumer;
	}

	@Override
	public void setInventorySlotContents(int slot, @Nonnull ItemStack stack) {
		super.setInventorySlotContents(slot, stack);
		if (!stack.isEmpty() && consumer != null)
			consumer.accept(stack);
	}

	@Override
	public boolean isItemValidForSlot(int slot, @Nonnull ItemStack stack) {
		return predicate.test(stack);
	}

	public void set(ItemStack stack) {
		super.setInventorySlotContents(0, stack);
	}

	public ItemStack get() {
		return getStackInSlot(0);
	}

}
