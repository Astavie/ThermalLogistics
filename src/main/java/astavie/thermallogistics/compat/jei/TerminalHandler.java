package astavie.thermallogistics.compat.jei;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import astavie.thermallogistics.client.gui.GuiTerminalItem;
import astavie.thermallogistics.client.gui.element.ElementSlot;
import astavie.thermallogistics.container.ContainerTerminalItem;
import astavie.thermallogistics.util.Shared;
import cofh.core.gui.element.tab.TabBase;
import cofh.core.util.helpers.ItemHelper;
import mezz.jei.api.gui.IAdvancedGuiHandler;
import mezz.jei.api.gui.IGhostIngredientHandler;
import mezz.jei.api.gui.IGuiIngredient;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.NonNullList;
import net.minecraftforge.oredict.OreDictionary;

public class TerminalHandler implements IRecipeTransferHandler<ContainerTerminalItem>, IAdvancedGuiHandler<GuiTerminalItem>, IGhostIngredientHandler<GuiTerminalItem> {

	@Nonnull
	@Override
	public Class<ContainerTerminalItem> getContainerClass() {
		return ContainerTerminalItem.class;
	}

	@Nullable
	@Override
	public IRecipeTransferError transferRecipe(@Nonnull ContainerTerminalItem container, @Nonnull IRecipeLayout recipeLayout, @Nonnull EntityPlayer player, boolean maxTransfer, boolean doTransfer) {
		if (doTransfer) {
			if (!container.gui.tabCrafting.open) {
				container.gui.tabCrafting.toggleOpen();
			}

			ItemStack[][] stacks = new ItemStack[9][];

			// Shamelessly copied from Logistics Pipes
			Map<Integer, ? extends IGuiIngredient<ItemStack>> ingredients = recipeLayout.getItemStacks().getGuiIngredients();
			for (Map.Entry<Integer, ? extends IGuiIngredient<ItemStack>> ps : ingredients.entrySet()) {
				if (!ps.getValue().isInput())
					continue;

				int slot = ps.getKey() - 1;
				if (slot >= 9)
					continue;

				List<ItemStack> list = new ArrayList<>(ps.getValue().getAllIngredients());
				if (list.isEmpty())
					continue;

				Iterator<ItemStack> iterator = list.iterator();
				while (iterator.hasNext()) {
					ItemStack item = iterator.next();
					if (item.getMetadata() == OreDictionary.WILDCARD_VALUE) {
						iterator.remove();
						NonNullList<ItemStack> sub = NonNullList.create();
						item.getItem().getSubItems(item.getItem().getCreativeTab(), sub);
						list.addAll(sub);
						iterator = list.iterator();
					}
				}
				stacks[slot] = list.toArray(new ItemStack[0]);
			}

			for (int i = 0; i < 9; i++)
				container.gui.tile.shared[i] = new SharedJEI(stacks[i]);
		}
		return null;
	}

	@Nonnull
	@Override
	public Class<GuiTerminalItem> getGuiContainerClass() {
		return GuiTerminalItem.class;
	}

	@Nullable
	@Override
	public Object getIngredientUnderMouse(GuiTerminalItem gui, int mouseX, int mouseY) {
		return gui.getStackAt(mouseX - gui.getGuiLeft(), mouseY - gui.getGuiTop());
	}

	@Nonnull
	@Override
	@SuppressWarnings("unchecked")
	public <I> List<Target<I>> getTargets(@Nonnull GuiTerminalItem gui, @Nonnull I ingredient, boolean doStart) {
		if (ingredient instanceof ItemStack && gui.tabCrafting.isFullyOpened())
			return Arrays.stream(gui.tabCrafting.grid).map(slot -> (Target<I>) new TabTarget(gui.tabCrafting, slot)).collect(Collectors.toList());

		return Collections.emptyList();
	}

	@Override
	public void onComplete() {
	}

	private static class SharedJEI extends Shared.Item {

		private final ItemStack[] stacks;

		private SharedJEI(ItemStack[] stacks) {
			this.stacks = stacks;
		}

		@Override
		public ItemStack get() {
			if (super.get() != null)
				return super.get();
			if (stacks == null)
				return ItemStack.EMPTY;
			return stacks[(int) ((Minecraft.getMinecraft().world.getTotalWorldTime() / 20) % stacks.length)];
		}

		@Override
		public boolean test(ItemStack stack) {
			if (super.get() != null)
				return super.test(stack);
			if (stacks == null)
				return stack.isEmpty();
			return Arrays.stream(stacks).anyMatch(item -> ItemHelper.itemsIdentical(item, stack));
		}

		@Override
		public Ingredient asIngredient() {
			if (super.get() != null)
				return super.asIngredient();
			if (stacks == null)
				return Ingredient.EMPTY;
			return Ingredient.fromStacks(stacks);
		}

		@Override
		public ItemStack getDisplayStack() {
			if (super.get() != null)
				return super.getDisplayStack();
			if (stacks == null)
				return ItemStack.EMPTY;
			return stacks[0];
		}

	}

	private static class TabTarget implements Target<ItemStack> {

		private final TabBase tab;
		private final ElementSlot<ItemStack> target;

		public TabTarget(TabBase tab, ElementSlot<ItemStack> target) {
			this.tab = tab;
			this.target = target;
		}

		@Nonnull
		@Override
		public Rectangle getArea() {
			Rectangle rectangle = target.getArea();
			rectangle.translate(tab.side == TabBase.LEFT ? tab.getPosX() - tab.currentWidth : tab.getPosX(), tab.getPosY());
			return rectangle;
		}

		@Override
		public void accept(@Nonnull ItemStack ingredient) {
			target.accept(ingredient);
		}
	}

}
