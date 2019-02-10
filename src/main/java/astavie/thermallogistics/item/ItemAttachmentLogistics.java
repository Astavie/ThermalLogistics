package astavie.thermallogistics.item;

import astavie.thermallogistics.ThermalLogistics;
import cofh.thermaldynamics.item.ItemAttachment;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraftforge.client.model.ModelLoader;

import javax.annotation.Nonnull;

public abstract class ItemAttachmentLogistics extends ItemAttachment {

	public static final String[] NAMES = {"basic", "hardened", "reinforced", "signalum", "resonant"};
	public static final EnumRarity[] RARITY = {EnumRarity.COMMON, EnumRarity.COMMON, EnumRarity.UNCOMMON, EnumRarity.UNCOMMON, EnumRarity.RARE};

	private final String name;

	public ItemAttachmentLogistics(String name) {
		this.name = name;
		setTranslationKey("logistics." + name);
		setRegistryName(name);
	}

	@Nonnull
	@Override
	public String getTranslationKey(ItemStack stack) {
		return getTranslationKey() + "." + NAMES[stack.getMetadata()];
	}

	@Override
	public void getSubItems(@Nonnull CreativeTabs tab, @Nonnull NonNullList<ItemStack> items) {
		if (isInCreativeTab(tab))
			for (int i = 0; i < NAMES.length; i++)
				items.add(new ItemStack(this, 1, i));
	}

	@Nonnull
	@Override
	public EnumRarity getRarity(ItemStack stack) {
		return RARITY[stack.getMetadata()];
	}

	@Override
	public boolean preInit() {
		return true;
	}

	@Override
	public void registerModels() {
		for (int i = 0; i < NAMES.length; i++) {
			ModelResourceLocation location = new ModelResourceLocation(ThermalLogistics.MOD_ID + ":" + name, "type=" + NAMES[i]);
			ModelLoader.setCustomModelResourceLocation(this, i, location);
		}
	}

}
