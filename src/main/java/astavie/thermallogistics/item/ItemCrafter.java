package astavie.thermallogistics.item;

import astavie.thermallogistics.attachment.CrafterFluid;
import astavie.thermallogistics.attachment.CrafterItem;
import cofh.core.util.helpers.RecipeHelper;
import cofh.core.util.helpers.StringHelper;
import cofh.thermaldynamics.duct.Attachment;
import cofh.thermaldynamics.duct.Duct;
import cofh.thermaldynamics.duct.attachments.filter.FilterLogic;
import cofh.thermaldynamics.duct.attachments.servo.ServoBase;
import cofh.thermaldynamics.duct.attachments.servo.ServoFluid;
import cofh.thermaldynamics.duct.attachments.servo.ServoItem;
import cofh.thermaldynamics.duct.tiles.DuctToken;
import cofh.thermaldynamics.duct.tiles.TileGrid;
import cofh.thermaldynamics.item.ItemRetriever;
import net.minecraft.client.Minecraft;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

public class ItemCrafter extends ItemAttachmentLogistics {

	public ItemCrafter(String name) {
		super(name);
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
	public Attachment getAttachment(EnumFacing side, ItemStack stack, TileGrid tile) {
		int type = stack.getItemDamage();
		if (tile.getDuct(DuctToken.FLUID) != null)
			return new CrafterFluid(tile, (byte) (side.ordinal() ^ 1), type);
		if (tile.getDuct(DuctToken.ITEMS) != null)
			return new CrafterItem(tile, (byte) (side.ordinal() ^ 1), type);
		return null;
	}

	@Override
	public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
		int type = stack.getMetadata();

		if (!StringHelper.isShiftKeyDown()) {
			tooltip.add(StringHelper.getInfoText("item.logistics.crafter.info"));
			if (StringHelper.displayShiftForDetail)
				tooltip.add(StringHelper.shiftForDetails());
			return;
		}

		if (ServoBase.canAlterRS(type))
			tooltip.add(StringHelper.localize("info.thermaldynamics.servo.redstoneInt"));
		else
			tooltip.add(StringHelper.localize("info.thermaldynamics.servo.redstoneExt"));

		// Items
		tooltip.add(StringHelper.YELLOW + StringHelper.localize("info.cofh.items") + StringHelper.END);

		tooltip.add("  " + StringHelper.localizeFormat("info.logistics.inputs", CrafterItem.SIZE[type] * 2));
		tooltip.add("  " + StringHelper.localizeFormat("info.logistics.outputs", CrafterItem.SIZE[type]));

		tooltip.add("  " + StringHelper.localize("info.thermaldynamics.servo.extractRate") + ": " + StringHelper.WHITE + ((ServoItem.tickDelays[type] % 20) == 0 ? Integer.toString(ServoItem.tickDelays[type] / 20) : Float.toString(ServoItem.tickDelays[type] / 20F)) + "s" + StringHelper.END);
		tooltip.add("  " + StringHelper.localize("info.thermaldynamics.servo.maxStackSize") + ": " + StringHelper.WHITE + ServoItem.maxSize[type] + StringHelper.END);
		addFiltering(tooltip, type, Duct.Type.ITEM);

		if (ServoItem.multiStack[type])
			tooltip.add("  " + StringHelper.localize("info.thermaldynamics.servo.slotMulti"));
		else
			tooltip.add("  " + StringHelper.localize("info.thermaldynamics.servo.slotSingle"));

		if (ServoItem.speedBoost[type] != 1)
			tooltip.add("  " + StringHelper.localize("info.thermaldynamics.servo.speedBoost") + ": " + StringHelper.WHITE + ServoItem.speedBoost[type] + "x " + StringHelper.END);

		// Fluids
		tooltip.add(StringHelper.YELLOW + StringHelper.localize("info.cofh.fluids") + StringHelper.END);

		tooltip.add("  " + StringHelper.localizeFormat("info.logistics.inputs", CrafterFluid.SIZE[type] * 2));
		tooltip.add("  " + StringHelper.localizeFormat("info.logistics.outputs", CrafterFluid.SIZE[type]));

		tooltip.add("  " + StringHelper.localize("info.thermaldynamics.servo.extractRate") + ": " + StringHelper.WHITE + (int) (ServoFluid.throttle[type] * 100) + "%" + StringHelper.END);
		addFiltering(tooltip, type, Duct.Type.FLUID);
	}

	@Override
	public boolean initialize() {
		ItemStack crafterBasic = new ItemStack(this, 1, 0);
		ItemStack crafterHardened = new ItemStack(this, 1, 1);
		ItemStack crafterReinforced = new ItemStack(this, 1, 2);
		ItemStack crafterSignalum = new ItemStack(this, 1, 3);
		ItemStack crafterResonant = new ItemStack(this, 1, 4);

		// Basic
		RecipeHelper.addShapedRecipe(crafterBasic, "iCi", "IRI", 'i', "nuggetIron", 'R', ItemRetriever.retrieverBasic, 'I', "ingotIron", 'C', "workbench");

		// Hardened
		RecipeHelper.addShapedRecipe(crafterHardened, "iCi", "IRI", 'i', "nuggetIron", 'R', ItemRetriever.retrieverHardened, 'I', "ingotInvar", 'C', "workbench");
		RecipeHelper.addShapelessRecipe(crafterHardened, crafterBasic, "ingotInvar");

		// Reinforced
		RecipeHelper.addShapedRecipe(crafterReinforced, "iCi", "IRI", 'i', "nuggetIron", 'R', ItemRetriever.retrieverReinforced, 'I', "ingotElectrum", 'C', "workbench");
		RecipeHelper.addShapelessRecipe(crafterReinforced, crafterBasic, "ingotElectrum");
		RecipeHelper.addShapelessRecipe(crafterReinforced, crafterHardened, "ingotElectrum");

		// Signalum
		RecipeHelper.addShapedRecipe(crafterSignalum, "iCi", "IRI", 'i', "nuggetIron", 'R', ItemRetriever.retrieverSignalum, 'I', "ingotSignalum", 'C', "workbench");
		RecipeHelper.addShapelessRecipe(crafterSignalum, crafterBasic, "ingotSignalum");
		RecipeHelper.addShapelessRecipe(crafterSignalum, crafterHardened, "ingotSignalum");
		RecipeHelper.addShapelessRecipe(crafterSignalum, crafterReinforced, "ingotSignalum");

		// Resonant
		RecipeHelper.addShapedRecipe(crafterResonant, "iCi", "IRI", 'i', "nuggetIron", 'R', ItemRetriever.retrieverResonant, 'I', "ingotEnderium", 'C', "workbench");
		RecipeHelper.addShapelessRecipe(crafterResonant, crafterBasic, "ingotEnderium");
		RecipeHelper.addShapelessRecipe(crafterResonant, crafterHardened, "ingotEnderium");
		RecipeHelper.addShapelessRecipe(crafterResonant, crafterReinforced, "ingotEnderium");
		RecipeHelper.addShapelessRecipe(crafterResonant, crafterSignalum, "ingotEnderium");
		return true;
	}

}
