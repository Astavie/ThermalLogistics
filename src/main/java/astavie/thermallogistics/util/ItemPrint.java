package astavie.thermallogistics.util;

import cofh.core.util.helpers.ItemHelper;
import net.minecraft.item.ItemStack;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class ItemPrint {

	public final ItemStack compare;

	public ItemPrint(ItemStack compare) {
		this.compare = compare;
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof ItemPrint && ItemHelper.itemsIdentical(compare, ((ItemPrint) obj).compare);
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder().append(compare.getItem()).append(compare.getMetadata()).append(compare.getTagCompound()).build();
	}

}
