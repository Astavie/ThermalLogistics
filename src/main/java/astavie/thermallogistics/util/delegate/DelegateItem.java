package astavie.thermallogistics.util.delegate;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public class DelegateItem implements IDelegate<ItemStack> {

	public static final DelegateItem INSTANCE = new DelegateItem();

	@Override
	public boolean isNull(ItemStack stack) {
		return stack == null || stack.isEmpty();
	}

	@Override
	public NBTTagCompound writeStack(ItemStack stack) {
		return stack.writeToNBT(new NBTTagCompound());
	}

	@Override
	public ItemStack readStack(NBTTagCompound tag) {
		return new ItemStack(tag);
	}

}
