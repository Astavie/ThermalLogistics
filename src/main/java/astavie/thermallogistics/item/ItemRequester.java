package astavie.thermallogistics.item;

import astavie.thermallogistics.ThermalLogistics;
import astavie.thermallogistics.attachment.RequesterFluid;
import astavie.thermallogistics.attachment.RequesterItem;
import cofh.core.util.helpers.StringHelper;
import cofh.thermaldynamics.duct.Attachment;
import cofh.thermaldynamics.duct.Duct;
import cofh.thermaldynamics.duct.attachments.servo.ServoBase;
import cofh.thermaldynamics.duct.attachments.servo.ServoFluid;
import cofh.thermaldynamics.duct.attachments.servo.ServoItem;
import cofh.thermaldynamics.duct.tiles.DuctToken;
import cofh.thermaldynamics.duct.tiles.TileGrid;
import cofh.thermaldynamics.item.ItemAttachment;
import cofh.thermaldynamics.item.ItemRetriever;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Items;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.List;

import static cofh.core.util.helpers.RecipeHelper.addShapedRecipe;
import static cofh.core.util.helpers.RecipeHelper.addShapelessRecipe;

public class ItemRequester extends ItemAttachment {

	public static final String[] NAMES = {"basic", "hardened", "reinforced", "signalum", "resonant"};
	public static final EnumRarity[] RARITY = {EnumRarity.COMMON, EnumRarity.COMMON, EnumRarity.UNCOMMON, EnumRarity.UNCOMMON, EnumRarity.RARE};

	public static ItemStack requesterBasic, requesterHardened, requesterReinforced, requesterSignalum, requesterResonant;

	public ItemRequester() {
		setTranslationKey("logistics.requester");
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
		int type = stack.getItemDamage() % 5;
		if (tile.getDuct(DuctToken.ITEMS) != null)
			return new RequesterItem(tile, (byte) (side.ordinal() ^ 1), type);
		if (tile.getDuct(DuctToken.FLUID) != null)
			return new RequesterFluid(tile, (byte) (side.ordinal() ^ 1), type);
		return null;
	}

	@Override
	public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
		int type = stack.getItemDamage() % 5;

		if (!StringHelper.isShiftKeyDown()) {
			tooltip.add(StringHelper.getInfoText("item.logistics.requester.info"));
			if (StringHelper.displayShiftForDetail)
				tooltip.add(StringHelper.shiftForDetails());
			return;
		}
		if (ServoBase.canAlterRS(type))
			tooltip.add(StringHelper.localize("info.thermaldynamics.servo.redstoneInt"));
		else
			tooltip.add(StringHelper.localize("info.thermaldynamics.servo.redstoneExt"));

		tooltip.add(StringHelper.YELLOW + StringHelper.localize("info.cofh.items") + StringHelper.END);

		tooltip.add("  " + StringHelper.localize("info.thermaldynamics.servo.extractRate") + ": " + StringHelper.WHITE + ((ServoItem.tickDelays[type] % 20) == 0 ? Integer.toString(ServoItem.tickDelays[type] / 20) : Float.toString(ServoItem.tickDelays[type] / 20F)) + "s" + StringHelper.END);
		tooltip.add("  " + StringHelper.localize("info.thermaldynamics.servo.maxStackSize") + ": " + StringHelper.WHITE + ServoItem.maxSize[type] + StringHelper.END);
		ItemRetriever.addFiltering(tooltip, type, Duct.Type.ITEM);

		if (ServoItem.multiStack[type])
			tooltip.add("  " + StringHelper.localize("info.thermaldynamics.servo.slotMulti"));
		else
			tooltip.add("  " + StringHelper.localize("info.thermaldynamics.servo.slotSingle"));

		if (ServoItem.speedBoost[type] != 1)
			tooltip.add("  " + StringHelper.localize("info.thermaldynamics.servo.speedBoost") + ": " + StringHelper.WHITE + ServoItem.speedBoost[type] + "x " + StringHelper.END);

		tooltip.add(StringHelper.YELLOW + StringHelper.localize("info.cofh.fluids") + StringHelper.END);
		tooltip.add("  " + StringHelper.localize("info.thermaldynamics.servo.extractRate") + ": " + StringHelper.WHITE + (int) (ServoFluid.throttle[type] * 100) + "%" + StringHelper.END);
		ItemRetriever.addFiltering(tooltip, type, Duct.Type.FLUID);
	}

	@Override
	public boolean preInit() {
		ForgeRegistries.ITEMS.register(setRegistryName("requester"));
		ThermalLogistics.proxy.addModelRegister(this);

		requesterBasic = new ItemStack(this, 1, 0);
		requesterHardened = new ItemStack(this, 1, 1);
		requesterReinforced = new ItemStack(this, 1, 2);
		requesterSignalum = new ItemStack(this, 1, 3);
		requesterResonant = new ItemStack(this, 1, 4);

		return true;
	}

	@Override
	public boolean initialize() {
		addShapedRecipe(requesterBasic, "iCi", "IRI", 'i', "nuggetIron", 'R', ItemRetriever.retrieverBasic, 'I', "ingotIron", 'C', Items.COMPARATOR);

		addShapedRecipe(requesterHardened, "iCi", "IRI", 'i', "nuggetIron", 'R', ItemRetriever.retrieverHardened, 'I', "ingotInvar", 'C', Items.COMPARATOR);
		addShapelessRecipe(requesterHardened, requesterBasic, "ingotInvar");

		addShapedRecipe(requesterReinforced, "iCi", "IRI", 'i', "nuggetIron", 'R', ItemRetriever.retrieverReinforced, 'I', "ingotElectrum", 'C', Items.COMPARATOR);
		addShapelessRecipe(requesterReinforced, requesterBasic, "ingotElectrum");
		addShapelessRecipe(requesterReinforced, requesterHardened, "ingotElectrum");

		addShapedRecipe(requesterSignalum, "iCi", "IRI", 'i', "nuggetIron", 'R', ItemRetriever.retrieverSignalum, 'I', "ingotSignalum", 'C', Items.COMPARATOR);
		addShapelessRecipe(requesterSignalum, requesterBasic, "ingotSignalum");
		addShapelessRecipe(requesterSignalum, requesterHardened, "ingotSignalum");
		addShapelessRecipe(requesterSignalum, requesterReinforced, "ingotSignalum");

		addShapedRecipe(requesterResonant, "iCi", "IRI", 'i', "nuggetIron", 'R', ItemRetriever.retrieverResonant, 'I', "ingotEnderium", 'C', Items.COMPARATOR);
		addShapelessRecipe(requesterResonant, requesterBasic, "ingotEnderium");
		addShapelessRecipe(requesterResonant, requesterHardened, "ingotEnderium");
		addShapelessRecipe(requesterResonant, requesterReinforced, "ingotEnderium");
		addShapelessRecipe(requesterResonant, requesterSignalum, "ingotEnderium");
		return true;
	}

	@Override
	public void registerModels() {
		String[] names = {"basic", "hardened", "reinforced", "signalum", "resonant"};
		for (int i = 0; i < names.length; i++) {
			ModelResourceLocation location = new ModelResourceLocation(ThermalLogistics.MODID + ":requester", "type=" + names[i]);
			ModelLoader.setCustomModelResourceLocation(this, i, location);
		}
	}

}
