package astavie.thermallogistics.util.type;

import cofh.core.network.PacketBase;
import cofh.core.util.helpers.ItemHelper;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
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
		Item item = tag.hasKey("id", 8) ? Item.getByNameOrId(tag.getString("id")) : Items.AIR;
		int damage = Math.max(0, tag.getShort("Damage"));

		NBTTagCompound nbt = null;
		if (tag.hasKey("tag", 10)) {
			nbt = tag.getCompoundTag("tag");
			if (item != null)
				item.updateItemStackNBT(tag);
		}

		return new ItemType(new ItemStack(item, 1, damage, nbt));
	}

	public static NBTTagCompound writeNbt(ItemType type) {
		NBTTagCompound tag = new NBTTagCompound();

		ResourceLocation resourcelocation = Item.REGISTRY.getNameForObject(type.compare.getItem());
		tag.setString("id", resourcelocation == null ? "minecraft:air" : resourcelocation.toString());
		tag.setShort("Damage", (short) type.compare.getItemDamage());
		if (type.compare.hasTagCompound())
			tag.setTag("tag", type.compare.getTagCompound());

		return tag;
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
	public boolean equals(Object obj) {
		return obj instanceof ItemType && ItemHelper.itemsIdentical(compare, ((ItemType) obj).compare);
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder().append(compare.getItem()).append(compare.getMetadata()).append(compare.getTagCompound()).build();
	}

}
