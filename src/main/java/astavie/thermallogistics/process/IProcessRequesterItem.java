package astavie.thermallogistics.process;

import net.minecraft.item.ItemStack;

public interface IProcessRequesterItem extends IProcessRequester<ItemStack> {

	int maxSize();

	boolean multiStack();

	byte speedBoost();

}
