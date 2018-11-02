package astavie.thermallogistics.tile;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.wrapper.InvWrapper;

import javax.annotation.Nonnull;

public class InvWrapperInsert extends InvWrapper {

	public InvWrapperInsert(IInventory inv) {
		super(inv);
	}

	@Nonnull
	@Override
	public ItemStack extractItem(int slot, int amount, boolean simulate) {
		return ItemStack.EMPTY;
	}

}
