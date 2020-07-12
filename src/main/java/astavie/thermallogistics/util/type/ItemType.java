package astavie.thermallogistics.util.type;

import cofh.core.network.PacketBase;
import cofh.core.util.helpers.ItemHelper;
import cofh.core.util.helpers.StringHelper;
import com.google.common.primitives.Ints;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.oredict.OreDictionary;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.Collections;

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
	public String getMissingLocalization(long amount) {
		return StringHelper.localizeFormat("info.logistics.manager.e.1", amount, getDisplayName());
	}

	@Override
	public void writePacket(PacketBase packet) {
		writePacket(this, packet);
	}

	@Override
	public int getPacketId() {
		return 0;
	}

	@Override
	public NBTTagCompound writeNbt() {
		return writeNbt(this);
	}

	@Override
	public boolean references(ItemStack stack) {
		return ItemHelper.itemsIdentical(compare, stack);
	}

	@Override
	public int maxSize() {
		return compare.getMaxStackSize();
	}

	@Override
	public boolean isNothing() {
		return compare.isEmpty();
	}

	@Override
	public boolean isIdentical(Type<ItemStack> other, boolean ignoreMod, boolean ignoreOreDict, boolean ignoreMetadata, boolean ignoreNbt) {
		if (!ignoreMod && compare.getItem().getRegistryName().getNamespace().equals(other.getAsStack().getItem().getRegistryName().getNamespace()))
			return true; // Same mod
		if (!ignoreOreDict && !Collections.disjoint(Ints.asList(OreDictionary.getOreIDs(compare)), Ints.asList(OreDictionary.getOreIDs(other.getAsStack()))))
			return true; // Same oredict

		// Same item
		return compare.getItem() == other.getAsStack().getItem() &&
				(ignoreMetadata || compare.getMetadata() == other.getAsStack().getMetadata()) &&
				(ignoreNbt || ItemStack.areItemStackTagsEqual(compare, other.getAsStack()));
	}

	@Override
	public int normalSize() {
		return 1;
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
