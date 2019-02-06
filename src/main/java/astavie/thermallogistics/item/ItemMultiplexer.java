package astavie.thermallogistics.item;

import astavie.thermallogistics.ThermalLogistics;
import astavie.thermallogistics.attachment.Multiplexer;
import cofh.core.util.helpers.StringHelper;
import cofh.thermaldynamics.duct.Attachment;
import cofh.thermaldynamics.duct.tiles.TileGrid;
import cofh.thermaldynamics.item.ItemAttachment;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.List;

public class ItemMultiplexer extends ItemAttachment {

	public static final String[] NAMES = {"basic", "hardened", "reinforced", "signalum", "resonant"};
	public static final EnumRarity[] RARITY = {EnumRarity.COMMON, EnumRarity.COMMON, EnumRarity.UNCOMMON, EnumRarity.UNCOMMON, EnumRarity.RARE};
	public static final int[] SLOTS = {2, 3, 4, 6, 8};

	public static ItemStack multiplexerBasic, multiplexerHardened, multiplexerReinforced, multiplexerSignalum, multiplexerResonant;

	public ItemMultiplexer() {
		setTranslationKey("logistics.multiplexer");
		setCreativeTab(ThermalLogistics.tab);
	}

	@Override
	public String getTranslationKey(ItemStack stack) {
		return super.getTranslationKey(stack) + "." + NAMES[stack.getItemDamage() % 5];
	}

	@Override
	public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items) {
		if (isInCreativeTab(tab))
			for (int i = 0; i < 5; i++)
				items.add(new ItemStack(this, 1, i));
	}

	@Override
	public EnumRarity getRarity(ItemStack stack) {
		return RARITY[stack.getItemDamage() % 5];
	}

	@Override
	public Attachment getAttachment(EnumFacing side, ItemStack stack, TileGrid tile) {
		return new Multiplexer(tile, (byte) (side.ordinal() ^ 1), stack.getItemDamage() % 5);
	}

	@Override
	public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
		int type = stack.getItemDamage() % 5;

		if (!StringHelper.isShiftKeyDown()) {
			tooltip.add(StringHelper.getInfoText("item.logistics.multiplexer.info"));
			if (StringHelper.displayShiftForDetail)
				tooltip.add(StringHelper.shiftForDetails());
			return;
		}

		tooltip.add(StringHelper.localize("info.logistics.servos") + ": " + SLOTS[type]);
	}

	@Override
	public boolean preInit() {
		ForgeRegistries.ITEMS.register(setRegistryName("multiplexer"));
		ThermalLogistics.proxy.addModelRegister(this);

		multiplexerBasic = new ItemStack(this, 1, 0);
		multiplexerHardened = new ItemStack(this, 1, 1);
		multiplexerReinforced = new ItemStack(this, 1, 2);
		multiplexerSignalum = new ItemStack(this, 1, 3);
		multiplexerResonant = new ItemStack(this, 1, 4);

		return true;
	}

	@Override
	public boolean initialize() {
		// Recipes
		return true;
	}

	@Override
	public void registerModels() {
		for (int i = 0; i < NAMES.length; i++) {
			ModelResourceLocation location = new ModelResourceLocation(ThermalLogistics.MODID + ":multiplexer", "type=" + NAMES[i]);
			ModelLoader.setCustomModelResourceLocation(this, i, location);
		}
	}

}
