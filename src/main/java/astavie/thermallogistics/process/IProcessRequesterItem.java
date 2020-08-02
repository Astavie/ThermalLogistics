package astavie.thermallogistics.process;

import astavie.thermallogistics.util.type.Type;
import net.minecraft.item.ItemStack;

public interface IProcessRequesterItem extends IProcessRequester<ItemStack> {

	int maxSize();

	boolean multiStack();

	byte speedBoost();

	long amountEmpty(Type<ItemStack> type);

}
