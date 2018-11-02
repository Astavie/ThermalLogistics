package astavie.thermallogistics.item;

import astavie.thermallogistics.ThermalLogistics;
import astavie.thermallogistics.attachment.CrafterFluid;
import astavie.thermallogistics.attachment.CrafterItem;
import cofh.core.util.helpers.StringHelper;
import cofh.thermaldynamics.duct.Attachment;
import cofh.thermaldynamics.duct.Duct;
import cofh.thermaldynamics.duct.attachments.filter.FilterLogic;
import cofh.thermaldynamics.duct.attachments.servo.ServoFluid;
import cofh.thermaldynamics.duct.attachments.servo.ServoItem;
import cofh.thermaldynamics.duct.tiles.DuctToken;
import cofh.thermaldynamics.duct.tiles.TileGrid;
import cofh.thermaldynamics.item.ItemAttachment;
import cofh.thermaldynamics.item.ItemRetriever;
import net.minecraft.client.Minecraft;
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

import static cofh.core.util.helpers.RecipeHelper.addShapedRecipe;
import static cofh.core.util.helpers.RecipeHelper.addShapelessRecipe;

public class ItemCrafter extends ItemAttachment {

	public static final String[] NAMES = {"basic", "hardened", "reinforced", "signalum", "resonant"};
	public static final EnumRarity[] RARITY = {EnumRarity.COMMON, EnumRarity.COMMON, EnumRarity.UNCOMMON, EnumRarity.UNCOMMON, EnumRarity.RARE};

	public static ItemStack crafterBasic, crafterHardened, crafterReinforced, crafterSignalum, crafterResonant;

	public ItemCrafter() {
		setTranslationKey("logistics.crafter");
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
			return new CrafterItem(tile, (byte) (side.ordinal() ^ 1), type);
		else if (tile.getDuct(DuctToken.FLUID) != null)
			return new CrafterFluid(tile, (byte) (side.ordinal() ^ 1), type);
		return null;
	}

	@Override
	public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
		int type = stack.getItemDamage() % 5;

		if (!StringHelper.isShiftKeyDown()) {
			tooltip.add(StringHelper.getInfoText("item.logistics.crafter.info"));
			if (StringHelper.displayShiftForDetail)
				tooltip.add(StringHelper.shiftForDetails());
			return;
		}

		tooltip.add(StringHelper.YELLOW + StringHelper.localize("info.cofh.items") + StringHelper.END);

		tooltip.add("  " + StringHelper.localize("info.thermaldynamics.servo.extractRate") + ": " + StringHelper.WHITE + ((ServoItem.tickDelays[type] % 20) == 0 ? Integer.toString(ServoItem.tickDelays[type] / 20) : Float.toString(ServoItem.tickDelays[type] / 20F)) + "s" + StringHelper.END);
		tooltip.add("  " + StringHelper.localize("info.thermaldynamics.servo.maxStackSize") + ": " + StringHelper.WHITE + ServoItem.maxSize[type] + StringHelper.END);
		addFiltering(tooltip, type, Duct.Type.ITEM);

		if (ServoItem.multiStack[type])
			tooltip.add("  " + StringHelper.localize("info.thermaldynamics.servo.slotMulti"));
		else
			tooltip.add("  " + StringHelper.localize("info.thermaldynamics.servo.slotSingle"));

		if (ServoItem.speedBoost[type] != 1)
			tooltip.add("  " + StringHelper.localize("info.thermaldynamics.servo.speedBoost") + ": " + StringHelper.WHITE + ServoItem.speedBoost[type] + "x " + StringHelper.END);

		tooltip.add(StringHelper.YELLOW + StringHelper.localize("info.cofh.fluids") + StringHelper.END);
		tooltip.add("  " + StringHelper.localize("info.thermaldynamics.servo.extractRate") + ": " + StringHelper.WHITE + Integer.toString((int) (ServoFluid.throttle[type] * 100)) + "%" + StringHelper.END);
		addFiltering(tooltip, type, Duct.Type.FLUID);
	}

	private static void addFiltering(List<String> list, int type, Duct.Type duct) {
		StringBuilder b = new StringBuilder();

		b.append(StringHelper.localize("info.thermaldynamics.filter.options")).append(": ").append(StringHelper.WHITE);
		boolean flag = false;
		for (int i = 1; i < FilterLogic.flagTypes.length; i++) { // Skip whitelist
			if (FilterLogic.canAlterFlag(duct, type, i)) {
				if (flag)
					b.append(", ");
				else
					flag = true;
				b.append(StringHelper.localize("info.thermaldynamics.filter." + FilterLogic.flagTypes[i]));
			}
		}
		flag = false;
		for (String s : Minecraft.getMinecraft().fontRenderer.listFormattedStringToWidth(b.toString(), 140)) {
			if (flag)
				s = "  " + StringHelper.WHITE + s;
			flag = true;
			list.add("  " + s + StringHelper.END);
		}
	}

	@Override
	public boolean preInit() {
		ForgeRegistries.ITEMS.register(setRegistryName("crafter"));
		ThermalLogistics.proxy.addModelRegister(this);

		crafterBasic = new ItemStack(this, 1, 0);
		crafterHardened = new ItemStack(this, 1, 1);
		crafterReinforced = new ItemStack(this, 1, 2);
		crafterSignalum = new ItemStack(this, 1, 3);
		crafterResonant = new ItemStack(this, 1, 4);

		return true;
	}

	@Override
	public boolean initialize() {
		addShapedRecipe(crafterBasic, "iCi", "IRI", 'i', "nuggetIron", 'R', ItemRetriever.retrieverBasic, 'I', "ingotIron", 'C', "workbench");

		addShapedRecipe(crafterHardened, "iCi", "IRI", 'i', "nuggetIron", 'R', ItemRetriever.retrieverHardened, 'I', "ingotInvar", 'C', "workbench");
		addShapelessRecipe(crafterHardened, crafterBasic, "ingotInvar");

		addShapedRecipe(crafterReinforced, "iCi", "IRI", 'i', "nuggetIron", 'R', ItemRetriever.retrieverReinforced, 'I', "ingotElectrum", 'C', "workbench");
		addShapelessRecipe(crafterReinforced, crafterBasic, "ingotElectrum");
		addShapelessRecipe(crafterReinforced, crafterHardened, "ingotElectrum");

		addShapedRecipe(crafterSignalum, "iCi", "IRI", 'i', "nuggetIron", 'R', ItemRetriever.retrieverSignalum, 'I', "ingotSignalum", 'C', "workbench");
		addShapelessRecipe(crafterSignalum, crafterBasic, "ingotSignalum");
		addShapelessRecipe(crafterSignalum, crafterHardened, "ingotSignalum");
		addShapelessRecipe(crafterSignalum, crafterReinforced, "ingotSignalum");

		addShapedRecipe(crafterResonant, "iCi", "IRI", 'i', "nuggetIron", 'R', ItemRetriever.retrieverResonant, 'I', "ingotEnderium", 'C', "workbench");
		addShapelessRecipe(crafterResonant, crafterBasic, "ingotEnderium");
		addShapelessRecipe(crafterResonant, crafterHardened, "ingotEnderium");
		addShapelessRecipe(crafterResonant, crafterReinforced, "ingotEnderium");
		addShapelessRecipe(crafterResonant, crafterSignalum, "ingotEnderium");
		return true;
	}

	@Override
	public void registerModels() {
		for (int i = 0; i < NAMES.length; i++) {
			ModelResourceLocation location = new ModelResourceLocation(ThermalLogistics.MODID + ":crafter", "type=" + NAMES[i]);
			ModelLoader.setCustomModelResourceLocation(this, i, location);
		}
	}

}
