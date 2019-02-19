package astavie.thermallogistics.item;

import astavie.thermallogistics.attachment.RequesterFluid;
import astavie.thermallogistics.attachment.RequesterItem;
import cofh.core.util.helpers.RecipeHelper;
import cofh.core.util.helpers.StringHelper;
import cofh.thermaldynamics.duct.Attachment;
import cofh.thermaldynamics.duct.Duct;
import cofh.thermaldynamics.duct.attachments.servo.ServoBase;
import cofh.thermaldynamics.duct.attachments.servo.ServoFluid;
import cofh.thermaldynamics.duct.attachments.servo.ServoItem;
import cofh.thermaldynamics.duct.tiles.DuctToken;
import cofh.thermaldynamics.duct.tiles.TileGrid;
import cofh.thermaldynamics.item.ItemRetriever;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

public class ItemRequester extends ItemAttachmentLogistics {

	public ItemRequester(String name) {
		super(name);
	}

	@Override
	public Attachment getAttachment(EnumFacing side, ItemStack stack, TileGrid tile) {
		int type = stack.getItemDamage();
		if (tile.getDuct(DuctToken.FLUID) != null)
			return new RequesterFluid(tile, (byte) (side.ordinal() ^ 1), type);
		if (tile.getDuct(DuctToken.ITEMS) != null)
			return new RequesterItem(tile, (byte) (side.ordinal() ^ 1), type);
		return null;
	}

	@Override
	public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
		int type = stack.getMetadata();

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
	public boolean initialize() {
		ItemStack requesterBasic = new ItemStack(this, 1, 0);
		ItemStack requesterHardened = new ItemStack(this, 1, 1);
		ItemStack requesterReinforced = new ItemStack(this, 1, 2);
		ItemStack requesterSignalum = new ItemStack(this, 1, 3);
		ItemStack requesterResonant = new ItemStack(this, 1, 4);

		// Basic
		RecipeHelper.addShapedRecipe(requesterBasic, "iCi", "IRI", 'i', "nuggetIron", 'R', ItemRetriever.retrieverBasic, 'I', "ingotIron", 'C', Items.COMPARATOR);

		// Hardened
		RecipeHelper.addShapedRecipe(requesterHardened, "iCi", "IRI", 'i', "nuggetIron", 'R', ItemRetriever.retrieverHardened, 'I', "ingotInvar", 'C', Items.COMPARATOR);
		RecipeHelper.addShapelessRecipe(requesterHardened, requesterBasic, "ingotInvar");

		// Reinforced
		RecipeHelper.addShapedRecipe(requesterReinforced, "iCi", "IRI", 'i', "nuggetIron", 'R', ItemRetriever.retrieverReinforced, 'I', "ingotElectrum", 'C', Items.COMPARATOR);
		RecipeHelper.addShapelessRecipe(requesterReinforced, requesterBasic, "ingotElectrum");
		RecipeHelper.addShapelessRecipe(requesterReinforced, requesterHardened, "ingotElectrum");

		// Signalum
		RecipeHelper.addShapedRecipe(requesterSignalum, "iCi", "IRI", 'i', "nuggetIron", 'R', ItemRetriever.retrieverSignalum, 'I', "ingotSignalum", 'C', Items.COMPARATOR);
		RecipeHelper.addShapelessRecipe(requesterSignalum, requesterBasic, "ingotSignalum");
		RecipeHelper.addShapelessRecipe(requesterSignalum, requesterHardened, "ingotSignalum");
		RecipeHelper.addShapelessRecipe(requesterSignalum, requesterReinforced, "ingotSignalum");

		// Resonant
		RecipeHelper.addShapedRecipe(requesterResonant, "iCi", "IRI", 'i', "nuggetIron", 'R', ItemRetriever.retrieverResonant, 'I', "ingotEnderium", 'C', Items.COMPARATOR);
		RecipeHelper.addShapelessRecipe(requesterResonant, requesterBasic, "ingotEnderium");
		RecipeHelper.addShapelessRecipe(requesterResonant, requesterHardened, "ingotEnderium");
		RecipeHelper.addShapelessRecipe(requesterResonant, requesterReinforced, "ingotEnderium");
		RecipeHelper.addShapelessRecipe(requesterResonant, requesterSignalum, "ingotEnderium");
		return true;
	}

}
