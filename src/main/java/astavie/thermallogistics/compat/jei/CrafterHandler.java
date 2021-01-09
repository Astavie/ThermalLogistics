package astavie.thermallogistics.compat.jei;

import astavie.thermallogistics.attachment.IAttachmentCrafter;
import astavie.thermallogistics.attachment.Recipe;
import astavie.thermallogistics.client.gui.GuiCrafter;
import astavie.thermallogistics.client.gui.element.ElementSlot;
import astavie.thermallogistics.client.gui.tab.TabFluid;
import astavie.thermallogistics.container.ContainerCrafter;
import astavie.thermallogistics.util.collection.FluidList;
import astavie.thermallogistics.util.collection.ItemList;
import cofh.core.network.PacketHandler;
import cofh.core.network.PacketTileInfo;
import cofh.thermaldynamics.duct.attachments.ConnectionBase;
import mezz.jei.api.gui.IAdvancedGuiHandler;
import mezz.jei.api.gui.IGhostIngredientHandler;
import mezz.jei.api.gui.IGuiIngredient;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class CrafterHandler implements IRecipeTransferHandler<ContainerCrafter>, IAdvancedGuiHandler<GuiCrafter>, IGhostIngredientHandler<GuiCrafter> {

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
				transferItems((IAttachmentCrafter<ItemStack>) container.crafter, recipeLayout);
			else if (container.crafter.getItemClass() == FluidStack.class)
				transferFluids((IAttachmentCrafter<FluidStack>) container.crafter, recipeLayout);
		}
		return null;
	}

	private void transferItems(IAttachmentCrafter<ItemStack> crafter, IRecipeLayout recipeLayout) {
		int index;
		Recipe<ItemStack> recipe = null;

		for (index = 0; index < crafter.getRecipes().size(); index++) {
			recipe = crafter.getRecipes().get(index);
			if (recipe.inputs.stream().allMatch(ItemStack::isEmpty) && recipe.outputs.stream().allMatch(ItemStack::isEmpty)) {
				index++;
				break;
			}
		}

		index--;

		ItemList inputs = new ItemList();
		ItemList outputs = new ItemList();

		Map<Integer, ? extends IGuiIngredient<ItemStack>> guiIngredients = recipeLayout.getItemStacks().getGuiIngredients();
		for (Map.Entry<Integer, ? extends IGuiIngredient<ItemStack>> entry : guiIngredients.entrySet()) {
			List<ItemStack> ingredients = entry.getValue().getAllIngredients();
			if (!ingredients.isEmpty()) {
				if (entry.getValue().isInput())
					inputs.add(ingredients.get(0));
				else
					outputs.add(ingredients.get(0));
			}
		}

		PacketTileInfo packet = ((ConnectionBase) crafter).getNewPacket(ConnectionBase.NETWORK_ID.GUI);
		packet.addByte(4);
		packet.addInt(index);

		List<ItemStack> inputStacks = inputs.stacks();
		List<ItemStack> outputStacks = outputs.stacks();

		for (int i = 0; i < recipe.inputs.size(); i++) {
			ItemStack stack = i >= inputStacks.size() ? ItemStack.EMPTY : inputStacks.get(i);
			recipe.inputs.set(i, stack);
			packet.addItemStack(stack);
		}

		for (int i = 0; i < recipe.outputs.size(); i++) {
			ItemStack stack = i >= outputStacks.size() ? ItemStack.EMPTY : outputStacks.get(i);
			recipe.outputs.set(i, stack);
			packet.addItemStack(stack);
		}

		// Send to server
		PacketHandler.sendToServer(packet);
	}

	private void transferFluids(IAttachmentCrafter<FluidStack> crafter, IRecipeLayout recipeLayout) {
		int index;
		Recipe<FluidStack> recipe = null;

		for (index = 0; index < crafter.getRecipes().size(); index++) {
			recipe = crafter.getRecipes().get(index);
			if (recipe.inputs.stream().allMatch(Objects::isNull) && recipe.outputs.stream().allMatch(Objects::isNull)) {
				index++;
				break;
			}
		}

		index--;

		FluidList inputs = new FluidList();
		FluidList outputs = new FluidList();

		Map<Integer, ? extends IGuiIngredient<FluidStack>> guiIngredients = recipeLayout.getFluidStacks().getGuiIngredients();
		for (Map.Entry<Integer, ? extends IGuiIngredient<FluidStack>> entry : guiIngredients.entrySet()) {
			List<FluidStack> ingredients = entry.getValue().getAllIngredients();
			if (!ingredients.isEmpty()) {
				if (entry.getValue().isInput())
					inputs.add(ingredients.get(0));
				else
					outputs.add(ingredients.get(0));
			}
		}

		PacketTileInfo packet = ((ConnectionBase) crafter).getNewPacket(ConnectionBase.NETWORK_ID.GUI);
		packet.addByte(4);
		packet.addInt(index);

		List<FluidStack> inputStacks = inputs.stacks();
		List<FluidStack> outputStacks = outputs.stacks();

		for (int i = 0; i < recipe.inputs.size(); i++) {
			FluidStack stack = i >= inputStacks.size() ? null : inputStacks.get(i);
			recipe.inputs.set(i, stack);
			packet.addFluidStack(stack);
		}

		for (int i = 0; i < recipe.outputs.size(); i++) {
			FluidStack stack = i >= outputStacks.size() ? null : outputStacks.get(i);
			recipe.outputs.set(i, stack);
			packet.addFluidStack(stack);
		}

		// Send to server
		PacketHandler.sendToServer(packet);
	}

	@Nonnull
	@Override
	@SuppressWarnings("unchecked")
	public <I> List<Target<I>> getTargets(@Nonnull GuiCrafter gui, @Nonnull I ingredient, boolean doStart) {
		List<Target<I>> list = new LinkedList<>();

		if ((ingredient instanceof ItemStack && gui.crafter.getItemClass() == ItemStack.class) || (ingredient instanceof FluidStack && gui.crafter.getItemClass() == FluidStack.class))
			for (ElementSlot<?> slot : gui.slots)
				list.add(new ElementSlotTarget<I>((ElementSlot<I>) slot));

		if (ingredient instanceof FluidStack && gui.tab != null && gui.tab.isFullyOpened())
			list.add((Target<I>) new TabFluidTarget(gui.tab));

		return list;
	}

	@Override
	public void onComplete() {
	}

	@Nonnull
	@Override
	public Class<GuiCrafter> getGuiContainerClass() {
		return GuiCrafter.class;
	}

	@Nullable
	@Override
	public Object getIngredientUnderMouse(GuiCrafter gui, int mouseX, int mouseY) {
		mouseX -= gui.getGuiLeft();
		mouseY -= gui.getGuiTop();

		for (ElementSlot<?> slot : gui.slots)
			if (slot.intersectsWith(mouseX, mouseY))
				return slot.getIngredient();

		if (gui.tab != null && gui.tab.slot.intersectsWith(mouseX - gui.tab.posX(), mouseY - gui.tab.getPosY()))
			return gui.tab.slot.getIngredient();

		return null;
	}

	private static class TabFluidTarget implements Target<FluidStack> {

		private final TabFluid tab;

		private TabFluidTarget(TabFluid tab) {
			this.tab = tab;
		}

		@Nonnull
		@Override
		public Rectangle getArea() {
			return new Rectangle(tab.getContainerScreen().getGuiLeft() + tab.posX() + tab.slot.getPosX() + 1, tab.getContainerScreen().getGuiTop() + tab.getPosY() + tab.slot.getPosY() + 1, 16, 16);
		}

		@Override
		public void accept(@Nonnull FluidStack ingredient) {
			tab.slot.accept(ingredient, 2);
		}

	}

	private static class ElementSlotTarget<I> implements Target<I> {

		private final ElementSlot<I> slot;

		private ElementSlotTarget(ElementSlot<I> slot) {
			this.slot = slot;
		}

		@Nonnull
		@Override
		public Rectangle getArea() {
			return slot.getArea();
		}

		@Override
		public void accept(@Nonnull I ingredient) {
			slot.accept(ingredient, 2);
		}

	}

}
