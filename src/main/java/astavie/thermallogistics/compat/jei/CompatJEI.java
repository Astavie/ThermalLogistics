package astavie.thermallogistics.compat.jei;

import astavie.thermallogistics.container.ContainerTerminalItem;
import astavie.thermallogistics.util.Shared;
import cofh.core.util.helpers.ItemHelper;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.JEIPlugin;
import mezz.jei.api.gui.IGuiIngredient;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.recipe.VanillaRecipeCategoryUid;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.NonNullList;
import net.minecraftforge.oredict.OreDictionary;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

@JEIPlugin
public class CompatJEI implements IModPlugin, IRecipeTransferHandler<ContainerTerminalItem> {

	private IRecipeTransferHandlerHelper helper;

	@Override
	public void register(IModRegistry registry) {
		helper = registry.getJeiHelpers().recipeTransferHandlerHelper();
		registry.getRecipeTransferRegistry().addRecipeTransferHandler(this, VanillaRecipeCategoryUid.CRAFTING);
	}

	@Nonnull
	@Override
	public Class<ContainerTerminalItem> getContainerClass() {
		return ContainerTerminalItem.class;
	}

	@Nullable
	@Override
	public IRecipeTransferError transferRecipe(@Nonnull ContainerTerminalItem container, @Nonnull IRecipeLayout recipeLayout, @Nonnull EntityPlayer player, boolean maxTransfer, boolean doTransfer) {
		if (!container.gui.tabCrafting.isFullyOpened())
			return helper.createInternalError();

		if (doTransfer) {
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
				container.gui.shared[i] = new SharedJEI(stacks[i]);
		}
		return null;
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

	}

}
