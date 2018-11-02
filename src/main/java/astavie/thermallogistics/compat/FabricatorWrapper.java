package astavie.thermallogistics.compat;

import cofh.core.inventory.InventoryCraftingFalse;
import cofh.core.util.helpers.FluidHelper;
import cofh.core.util.helpers.ItemHelper;
import cofh.thermalexpansion.block.machine.TileCrafter;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

@SuppressWarnings("unchecked")
public class FabricatorWrapper implements ICrafterWrapper {

	public static final Class<TileCrafter> CLASS = TileCrafter.class;

	private final TileCrafter tile;

	public FabricatorWrapper(TileCrafter tile) {
		this.tile = tile;
	}

	@Override
	public <T> List<T> getInputs(Class<T> type) {
		if (type == ItemStack.class) {
			List<ItemStack> inputs = new LinkedList<>();
			a:
			for (int i = 0; i < 9; i++) {
				ItemStack stack = tile.inventory[TileCrafter.SLOT_CRAFTING_START + i].copy();
				if (!stack.isEmpty()) {
					for (ItemStack q : inputs) {
						if (ItemHelper.itemsIdentical(stack, q)) {
							q.grow(stack.getCount());
							continue a;
						}
					}
					inputs.add(stack);
				}
			}
			return (List<T>) inputs;
		} else if (type == FluidStack.class) {
			if (tile.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, null)) {
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
						return (List<T>) Collections.singletonList(fluid);
				}
			}
		}
		return Collections.emptyList();
	}

	@Override
	public <T> List<T> getOutputs(Class<T> type) {
		if (type == ItemStack.class) {
			InventoryCrafting craftMatrix = new InventoryCraftingFalse(3, 3);
			for (int i = 0; i < 9; i++)
				craftMatrix.setInventorySlotContents(i, tile.inventory[TileCrafter.SLOT_CRAFTING_START + i]);
			return (List<T>) Collections.singletonList(CraftingManager.findMatchingResult(craftMatrix, tile.getWorld()));
		}
		return Collections.emptyList();
	}

}
