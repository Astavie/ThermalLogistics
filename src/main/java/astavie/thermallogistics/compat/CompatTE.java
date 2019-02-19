package astavie.thermallogistics.compat;

import astavie.thermallogistics.attachment.ICrafter;
import astavie.thermallogistics.process.RequestItem;
import cofh.core.block.TileReconfigurable;
import cofh.core.inventory.InventoryCraftingFalse;
import cofh.core.util.core.SideConfig;
import cofh.core.util.helpers.FluidHelper;
import cofh.thermalexpansion.block.machine.TileCrafter;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;

import java.lang.reflect.Field;

public class CompatTE implements ICrafterWrapper<TileCrafter> {

	public static final Class<TileCrafter> TILE = TileCrafter.class;

	private static final Field sideConfig;

	static {
		Field field = null;
		try {
			field = TileReconfigurable.class.getDeclaredField("sideConfig");
			field.setAccessible(true);
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		}
		sideConfig = field;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <I> void populate(TileCrafter tile, byte side, ICrafter.Recipe<I> recipe, Class<I> itemClass) {
		if (itemClass == ItemStack.class)
			populateItems(tile, side, (ICrafter.Recipe<ItemStack>) recipe);
		else if (itemClass == FluidStack.class)
			populateFluids(tile, side, (ICrafter.Recipe<FluidStack>) recipe);
	}

	private void populateItems(TileCrafter tile, byte side, ICrafter.Recipe<ItemStack> recipe) {
		SideConfig sides = null;
		try {
			sides = (SideConfig) sideConfig.get(tile);
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}

		// Inputs
		if (SideConfig.allowInsertion(sides.sideTypes[tile.sideCache[side]])) {
			RequestItem request = new RequestItem(null);
			for (int i = 0; i < 9; i++)
				request.addStack(tile.inventory[TileCrafter.SLOT_CRAFTING_START + i]);

			for (int i = 0; i < recipe.inputs.size(); i++) {
				if (i == request.stacks.size())
					break;
				recipe.inputs.set(i, request.stacks.get(i));
			}
		}

		boolean[] extraction = {false, false, true, true, true, false, false, true, true};

		// Output
		if (extraction[sides.sideTypes[tile.sideCache[side]]]) {
			InventoryCrafting matrix = new InventoryCraftingFalse(3, 3);
			for (int i = 0; i < 9; i++)
				matrix.setInventorySlotContents(i, tile.inventory[TileCrafter.SLOT_CRAFTING_START + i]);
			recipe.outputs.set(0, CraftingManager.findMatchingResult(matrix, tile.getWorld()));
		}
	}

	private void populateFluids(TileCrafter tile, byte side, ICrafter.Recipe<FluidStack> recipe) {
		SideConfig sides = null;
		try {
			sides = (SideConfig) sideConfig.get(tile);
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}

		// Input
		if (SideConfig.allowInsertion(sides.sideTypes[tile.sideCache[side]]) && tile.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, null)) {
			// Get fluid type
			FluidStack fluid = tile.getTankFluid();
			if (fluid == null || fluid.amount == 0) {
				for (int i = 0; i < 9; i++) {
					ItemStack stack = tile.inventory[TileCrafter.SLOT_CRAFTING_START + i];
					if (FluidHelper.isFluidHandler(stack)) {
						fluid = FluidHelper.getFluidForFilledItem(stack);
						break;
					}
				}
			}

			// Get amount of fluid
			if (fluid != null && fluid.amount != 0) {
				fluid = fluid.copy();
				fluid.amount = 0;
				for (int i = 0; i < 9; i++) {
					ItemStack stack = tile.inventory[TileCrafter.SLOT_CRAFTING_START + i];
					if (FluidHelper.isFluidHandler(stack)) {
						FluidStack f = FluidHelper.getFluidForFilledItem(stack);
						if (FluidHelper.isFluidEqual(fluid, f))
							fluid.amount += f.amount;
					}
				}
				if (fluid.amount > 0)
					recipe.inputs.set(0, fluid);
			}
		}
	}

}
