package astavie.thermallogistics.util.type;

import cofh.core.network.PacketBase;
import cofh.core.util.helpers.ItemHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.nbt.NBTTagCompound;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class ItemType implements Type<ItemStack> {

	private final ItemStack compare;

	public ItemType(ItemStack compare) {
		this.compare = ItemHelper.cloneStack(compare, 1);
	}

	public static ItemType readPacket(PacketBase packet) {
		return new ItemType(packet.getItemStack());
	}

	public static void writePacket(ItemType type, PacketBase packet) {
		packet.addItemStack(type.compare);
	}

	public static ItemType readNbt(NBTTagCompound tag) {
		return new ItemType(new ItemStack(tag));
	}

	public static NBTTagCompound writeNbt(ItemType type) {
		return type.compare.writeToNBT(new NBTTagCompound());
	}

	public boolean fits(Ingredient ingredient) {
		return ingredient.apply(compare);
	}

	@Override
	public ItemStack getAsStack() {
		return compare;
	}

	@Override
	public ItemStack withAmount(int amount) {
		return ItemHelper.cloneStack(compare, amount);
	}

	@Override
	public String getDisplayName() {
		return compare.getDisplayName();
	}

	@Override
	public void writePacket(PacketBase packet) {
		writePacket(this, packet);
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof ItemType && ItemHelper.itemsIdentical(compare, ((ItemType) obj).compare);
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder().append(compare.getItem()).append(compare.getMetadata()).append(compare.getTagCompound()).build();
	}

}
