package astavie.thermallogistics.util.collection;

import astavie.thermallogistics.util.type.ItemType;
import astavie.thermallogistics.util.type.Type;
import cofh.core.network.PacketBase;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.nbt.NBTTagCompound;

public class ItemList extends StackList<ItemStack> {

	public boolean remove(Ingredient ingredient) {
		for (Type<ItemStack> type : types()) {
			if (((ItemType) type).fits(ingredient)) {
				remove(type.withAmount(1));
				return true;
			}
		}
		return false;
	}

	@Override
	public Type<ItemStack> getType(ItemStack stack) {
		return new ItemType(stack);
	}

	@Override
	protected int getAmount(ItemStack stack) {
		return stack.getCount();
	}

	@Override
	protected void writeType(Type<ItemStack> type, PacketBase packet) {
		ItemType.writePacket((ItemType) type, packet);
	}

	@Override
	protected Type<ItemStack> readType(PacketBase packet) {
		return ItemType.readPacket(packet);
	}

	@Override
	protected NBTTagCompound writeType(Type<ItemStack> type) {
		return ItemType.writeNbt((ItemType) type);
	}

	@Override
	protected ItemType readType(NBTTagCompound tag) {
		return ItemType.readNbt(tag);
	}

}
