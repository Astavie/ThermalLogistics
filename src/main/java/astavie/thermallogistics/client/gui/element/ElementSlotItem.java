package astavie.thermallogistics.client.gui.element;

import cofh.core.gui.GuiContainerCore;
import cofh.core.util.helpers.ItemHelper;
import cofh.core.util.helpers.RenderHelper;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ElementSlotItem extends ElementSlot<ItemStack> {

	private final Supplier<ItemStack> stack;
	private final Consumer<ItemStack> consumer;

	private final boolean count;

	public ElementSlotItem(GuiContainerCore gui, int posX, int posY, Supplier<ItemStack> stack, Consumer<ItemStack> consumer, boolean count) {
		super(gui, posX, posY);
		this.stack = stack;
		this.consumer = consumer;
		this.count = count;
	}

	@Override
	protected void drawSlot(int mouseX, int mouseY) {
		ItemStack item = stack.get();
		if (!item.isEmpty()) {
			FontRenderer font = null;
			if (!item.isEmpty()) {
				font = item.getItem().getFontRenderer(item);
			}
			if (font == null) {
				font = gui.getFontRenderer();
			}

			RenderHelper.enableGUIStandardItemLighting();
			gui.itemRender.renderItemAndEffectIntoGUI(item, posX + 1, posY + 1);
			gui.itemRender.renderItemOverlayIntoGUI(font, item, posX + 1, posY + 1 - (gui.draggedStack.isEmpty() ? 0 : 8), count ? null : "");
		}
	}

	@Override
	protected void addTooltip(int mouseX, int mouseY, List<String> list) {
		if (!stack.get().isEmpty())
			list.addAll(gui.getItemToolTip(stack.get()));
	}

	@Override
	public Object getIngredient() {
		return stack.get();
	}

	@Override
	public boolean onMousePressed(int mouseX, int mouseY, int mouseButton) {
		if (mouseButton < 2 && intersectsWith(mouseX, mouseY)) {
			ItemStack drag = gui.draggedStack.isEmpty() ? gui.mc.player.inventory.getItemStack() : gui.draggedStack;

			accept(drag, mouseButton);
			return true;
		}
		return false;
	}

	@Override
	public void accept(@Nonnull ItemStack ingredient, int mouse) {
		ItemStack get = stack.get();
		ItemStack stack;

		if (ingredient.isEmpty())
			stack = ItemStack.EMPTY;
		else if (mouse == 0)
			stack = ingredient.copy();
		else if (ItemHelper.itemsIdentical(get, ingredient))
			stack = ItemHelper.cloneStack(get, get.getCount() + 1);
		else
			stack = ItemHelper.cloneStack(ingredient, 1);

		consumer.accept(stack);
	}

	public ItemStack get() {
		return stack.get();
	}

	@Override
	public void accept(@Nonnull ItemStack ingredient) {
		consumer.accept(ingredient);
	}

}
