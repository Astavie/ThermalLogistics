package astavie.thermallogistics.item;

import astavie.thermallogistics.attachment.DistributorFluid;
import astavie.thermallogistics.attachment.DistributorItem;
import cofh.core.util.helpers.RecipeHelper;
import cofh.core.util.helpers.StringHelper;
import cofh.thermaldynamics.duct.Attachment;
import cofh.thermaldynamics.duct.Duct;
import cofh.thermaldynamics.duct.attachments.servo.ServoBase;
import cofh.thermaldynamics.duct.attachments.servo.ServoFluid;
import cofh.thermaldynamics.duct.attachments.servo.ServoItem;
import cofh.thermaldynamics.duct.tiles.DuctToken;
import cofh.thermaldynamics.duct.tiles.TileGrid;
import cofh.thermaldynamics.item.ItemServo;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

public class ItemDistributor extends ItemAttachmentLogistics {

	public ItemDistributor(String name) {
		super(name);
	}

	@Override
	public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
		int type = stack.getMetadata();

		if (!StringHelper.isShiftKeyDown()) {
			tooltip.add(StringHelper.getInfoText("item.logistics.distributor.info"));
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

		tooltip.add("  " + StringHelper.localize("info.thermaldynamics.servo.extractRate") + ": " + StringHelper.WHITE + ((ServoItem.tickDelays[type] % 20) == 0 ? Integer.toString(ServoItem.tickDelays[type] / 20) : Float.toString(ServoItem.tickDelays[type] / 20F)) + "s" + StringHelper.END);
		tooltip.add("  " + StringHelper.localize("info.thermaldynamics.servo.maxStackSize") + ": " + StringHelper.WHITE + ServoItem.maxSize[type] + StringHelper.END);
		ItemServo.addFiltering(tooltip, type, Duct.Type.ITEM);

		if (ServoItem.multiStack[type])
			tooltip.add("  " + StringHelper.localize("info.thermaldynamics.servo.slotMulti"));
		else
			tooltip.add("  " + StringHelper.localize("info.thermaldynamics.servo.slotSingle"));

		if (ServoItem.speedBoost[type] != 1)
			tooltip.add("  " + StringHelper.localize("info.thermaldynamics.servo.speedBoost") + ": " + StringHelper.WHITE + ServoItem.speedBoost[type] + "x " + StringHelper.END);

		// Fluids
		tooltip.add(StringHelper.YELLOW + StringHelper.localize("info.cofh.fluids") + StringHelper.END);
		tooltip.add("  " + StringHelper.localize("info.thermaldynamics.servo.extractRate") + ": " + StringHelper.WHITE + (int) (ServoFluid.throttle[type] * 100) + "%" + StringHelper.END);
		ItemServo.addFiltering(tooltip, type, Duct.Type.FLUID);
	}

	@Override
	public Attachment getAttachment(EnumFacing side, ItemStack stack, TileGrid tile) {
		int type = stack.getItemDamage();
		if (tile.getDuct(DuctToken.FLUID) != null)
			return new DistributorFluid(tile, (byte) (side.ordinal() ^ 1), type);
		if (tile.getDuct(DuctToken.ITEMS) != null)
			return new DistributorItem(tile, (byte) (side.ordinal() ^ 1), type);
		return null;
	}

	@Override
	public boolean initialize() {
		ItemStack distributorBasic = new ItemStack(this, 1, 0);
		ItemStack distributorHardened = new ItemStack(this, 1, 1);
		ItemStack distributorReinforced = new ItemStack(this, 1, 2);
		ItemStack distributorSignalum = new ItemStack(this, 1, 3);
		ItemStack distributorResonant = new ItemStack(this, 1, 4);

		// Basic
		RecipeHelper.addShapedRecipe(distributorBasic, "iCi", "IRI", 'i', "nuggetIron", 'R', ItemServo.servoBasic, 'I', "ingotIron", 'C', Items.COMPARATOR);

		// Hardened
		RecipeHelper.addShapedRecipe(distributorHardened, "iCi", "IRI", 'i', "nuggetIron", 'R', ItemServo.servoHardened, 'I', "ingotInvar", 'C', Items.COMPARATOR);
		RecipeHelper.addShapelessRecipe(distributorHardened, distributorBasic, "ingotInvar");

		// Reinforced
		RecipeHelper.addShapedRecipe(distributorReinforced, "iCi", "IRI", 'i', "nuggetIron", 'R', ItemServo.servoReinforced, 'I', "ingotElectrum", 'C', Items.COMPARATOR);
		RecipeHelper.addShapelessRecipe(distributorReinforced, distributorBasic, "ingotElectrum");
		RecipeHelper.addShapelessRecipe(distributorReinforced, distributorHardened, "ingotElectrum");

		// Signalum
		RecipeHelper.addShapedRecipe(distributorSignalum, "iCi", "IRI", 'i', "nuggetIron", 'R', ItemServo.servoSignalum, 'I', "ingotSignalum", 'C', Items.COMPARATOR);
		RecipeHelper.addShapelessRecipe(distributorSignalum, distributorBasic, "ingotSignalum");
		RecipeHelper.addShapelessRecipe(distributorSignalum, distributorHardened, "ingotSignalum");
		RecipeHelper.addShapelessRecipe(distributorSignalum, distributorReinforced, "ingotSignalum");

		// Resonant
		RecipeHelper.addShapedRecipe(distributorResonant, "iCi", "IRI", 'i', "nuggetIron", 'R', ItemServo.servoResonant, 'I', "ingotEnderium", 'C', Items.COMPARATOR);
		RecipeHelper.addShapelessRecipe(distributorResonant, distributorBasic, "ingotEnderium");
		RecipeHelper.addShapelessRecipe(distributorResonant, distributorHardened, "ingotEnderium");
		RecipeHelper.addShapelessRecipe(distributorResonant, distributorReinforced, "ingotEnderium");
		RecipeHelper.addShapelessRecipe(distributorResonant, distributorSignalum, "ingotEnderium");
		return true;
	}

}
