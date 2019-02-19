package astavie.thermallogistics.compat.jei;

import astavie.thermallogistics.attachment.ICrafter;
import astavie.thermallogistics.container.ContainerCrafter;
import astavie.thermallogistics.process.RequestFluid;
import astavie.thermallogistics.process.RequestItem;
import cofh.core.network.PacketHandler;
import cofh.core.network.PacketTileInfo;
import cofh.thermaldynamics.duct.attachments.ConnectionBase;
import mezz.jei.api.gui.IGuiIngredient;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class CrafterHandler implements IRecipeTransferHandler<ContainerCrafter> {

	@Nonnull
	@Override
	public Class<ContainerCrafter> getContainerClass() {
		return ContainerCrafter.class;
	}

	@Nullable
	@Override
	@SuppressWarnings("unchecked")
	public IRecipeTransferError transferRecipe(@Nonnull ContainerCrafter container, @Nonnull IRecipeLayout recipeLayout, @Nonnull EntityPlayer player, boolean maxTransfer, boolean doTransfer) {
		if (doTransfer) {
			if (container.crafter.getItemClass() == ItemStack.class)
				transferItems((ICrafter<ItemStack>) container.crafter, recipeLayout);
			else if (container.crafter.getItemClass() == FluidStack.class)
				transferFluids((ICrafter<FluidStack>) container.crafter, recipeLayout);
		}
		return null;
	}

	private void transferItems(ICrafter<ItemStack> crafter, IRecipeLayout recipeLayout) {
		int index;
		ICrafter.Recipe<ItemStack> recipe = null;

		for (index = 0; index < crafter.getRecipes().size(); index++) {
			recipe = crafter.getRecipes().get(index);
			if (recipe.inputs.stream().allMatch(ItemStack::isEmpty) && recipe.outputs.stream().allMatch(ItemStack::isEmpty)) {
				index++;
				break;
			}
		}

		index--;

		RequestItem inputs = new RequestItem(null);
		RequestItem outputs = new RequestItem(null);

		Map<Integer, ? extends IGuiIngredient<ItemStack>> guiIngredients = recipeLayout.getItemStacks().getGuiIngredients();
		for (Map.Entry<Integer, ? extends IGuiIngredient<ItemStack>> entry : guiIngredients.entrySet()) {
			List<ItemStack> ingredients = entry.getValue().getAllIngredients();
			if (!ingredients.isEmpty()) {
				if (entry.getValue().isInput())
					inputs.addStack(ingredients.get(0));
				else
					outputs.addStack(ingredients.get(0));
			}
		}

		PacketTileInfo packet = crafter.getNewPacket(ConnectionBase.NETWORK_ID.GUI);
		packet.addByte(4);
		packet.addInt(index);

		for (int i = 0; i < recipe.inputs.size(); i++) {
			ItemStack stack = i >= inputs.stacks.size() ? ItemStack.EMPTY : inputs.stacks.get(i);
			recipe.inputs.set(i, stack);
			packet.addItemStack(stack);
		}

		for (int i = 0; i < recipe.outputs.size(); i++) {
			ItemStack stack = i >= outputs.stacks.size() ? ItemStack.EMPTY : outputs.stacks.get(i);
			recipe.outputs.set(i, stack);
			packet.addItemStack(stack);
		}

		// Send to server
		PacketHandler.sendToServer(packet);
	}

	private void transferFluids(ICrafter<FluidStack> crafter, IRecipeLayout recipeLayout) {
		int index;
		ICrafter.Recipe<FluidStack> recipe = null;

		for (index = 0; index < crafter.getRecipes().size(); index++) {
			recipe = crafter.getRecipes().get(index);
			if (recipe.inputs.stream().allMatch(Objects::isNull) && recipe.outputs.stream().allMatch(Objects::isNull)) {
				index++;
				break;
			}
		}

		index--;

		RequestFluid inputs = new RequestFluid(null);
		RequestFluid outputs = new RequestFluid(null);

		Map<Integer, ? extends IGuiIngredient<FluidStack>> guiIngredients = recipeLayout.getFluidStacks().getGuiIngredients();
		for (Map.Entry<Integer, ? extends IGuiIngredient<FluidStack>> entry : guiIngredients.entrySet()) {
			List<FluidStack> ingredients = entry.getValue().getAllIngredients();
			if (!ingredients.isEmpty()) {
				if (entry.getValue().isInput())
					inputs.addStack(ingredients.get(0));
				else
					outputs.addStack(ingredients.get(0));
			}
		}

		PacketTileInfo packet = crafter.getNewPacket(ConnectionBase.NETWORK_ID.GUI);
		packet.addByte(4);
		packet.addInt(index);

		for (int i = 0; i < recipe.inputs.size(); i++) {
			FluidStack stack = i >= inputs.stacks.size() ? null : inputs.stacks.get(i);
			recipe.inputs.set(i, stack);
			packet.addFluidStack(stack);
		}

		for (int i = 0; i < recipe.outputs.size(); i++) {
			FluidStack stack = i >= outputs.stacks.size() ? null : outputs.stacks.get(i);
			recipe.outputs.set(i, stack);
			packet.addFluidStack(stack);
		}

		// Send to server
		PacketHandler.sendToServer(packet);
	}

}
