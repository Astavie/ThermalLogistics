package astavie.thermallogistics.util.delegate;

import cofh.core.network.PacketBase;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public class DelegateItem implements IDelegate<ItemStack> {

	public static final DelegateItem INSTANCE = new DelegateItem();

	@Override
	public boolean isNull(ItemStack stack) {
		return stack == null || stack.isEmpty();
	}

	@Override
	public ItemStack copy(ItemStack stack) {
		return stack.copy();
	}

	@Override
	public NBTTagCompound writeNbt(ItemStack stack) {
		return stack.writeToNBT(new NBTTagCompound());
	}

	@Override
	public void writePacket(PacketBase packet, ItemStack stack) {
		packet.addItemStack(stack);
	}

	@Override
	public ItemStack readNbt(NBTTagCompound tag) {
		return new ItemStack(tag);
	}

	@Override
	public ItemStack readPacket(PacketBase packet) {
		return packet.getItemStack();
	}

}
