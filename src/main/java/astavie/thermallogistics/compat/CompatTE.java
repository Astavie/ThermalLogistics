package astavie.thermallogistics.compat;

import astavie.thermallogistics.attachment.Recipe;
import astavie.thermallogistics.util.collection.ItemList;
import cofh.core.block.TileReconfigurable;
import cofh.core.inventory.InventoryCraftingFalse;
import cofh.core.util.core.SideConfig;
import cofh.core.util.helpers.FluidHelper;
import cofh.thermalexpansion.block.machine.TileCrafter;
import cofh.thermalexpansion.item.ItemFrame;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;

import java.lang.reflect.Field;
import java.util.List;

public class CompatTE implements ICrafterWrapper<TileCrafter> {

	public static final Class<TileCrafter> TILE = TileCrafter.class;
	public static final ItemStack MACHINE_FRAME = ItemFrame.frameMachine;

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
	public <I> void populate(TileCrafter tile, byte side, Recipe<I> recipe, Class<I> itemClass) {
		if (itemClass == ItemStack.class)
			populateItems(tile, side, (Recipe<ItemStack>) recipe);
		else if (itemClass == FluidStack.class)
			populateFluids(tile, side, (Recipe<FluidStack>) recipe);
	}

	private void populateItems(TileCrafter tile, byte side, Recipe<ItemStack> recipe) {
		SideConfig sides = null;
		try {
			sides = (SideConfig) sideConfig.get(tile);
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}

		// Inputs
		if (SideConfig.allowInsertion(sides.sideTypes[tile.sideCache[side]])) {
			// Get fluid type
			FluidStack fluid = null;
			if (tile.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, null)) {
				for (int i = 0; i < 9; i++) {
					ItemStack stack = tile.inventory[TileCrafter.SLOT_CRAFTING_START + i];
					if (FluidHelper.isFluidHandler(stack)) {
						fluid = FluidHelper.getFluidForFilledItem(stack);
						break;
					}
				}
			}

			// Get items excluding fluid containers
			ItemList request = new ItemList();
			for (int i = 0; i < 9; i++) {
				ItemStack stack = tile.inventory[TileCrafter.SLOT_CRAFTING_START + i];
				if (fluid != null && FluidHelper.isFluidEqual(fluid, FluidHelper.getFluidForFilledItem(stack)))
					continue;
				request.add(stack);
			}

			List<ItemStack> stacks = request.stacks();
			for (int i = 0; i < recipe.inputs.size(); i++) {
				if (i == stacks.size())
					break;
				recipe.inputs.set(i, stacks.get(i));
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

	private void populateFluids(TileCrafter tile, byte side, Recipe<FluidStack> recipe) {
		SideConfig sides = null;
		try {
			sides = (SideConfig) sideConfig.get(tile);
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}

		// Input
		if (SideConfig.allowInsertion(sides.sideTypes[tile.sideCache[side]]) && tile.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, null)) {
			// Get fluid type
			FluidStack fluid = null;
			for (int i = 0; i < 9; i++) {
				ItemStack stack = tile.inventory[TileCrafter.SLOT_CRAFTING_START + i];
				if (FluidHelper.isFluidHandler(stack)) {
					fluid = FluidHelper.getFluidForFilledItem(stack);
					break;
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
