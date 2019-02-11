package astavie.thermallogistics.client.gui.element;

import cofh.core.gui.GuiContainerCore;
import cofh.core.util.helpers.ItemHelper;
import cofh.core.util.helpers.RenderHelper;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.item.ItemStack;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ElementSlotItem extends ElementSlot {

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
	public boolean onMousePressed(int mouseX, int mouseY, int mouseButton) {
		if (mouseButton < 2 && intersectsWith(mouseX, mouseY)) {
			ItemStack get = stack.get();
			ItemStack drag = gui.draggedStack.isEmpty() ? gui.mc.player.inventory.getItemStack() : gui.draggedStack;
			ItemStack stack;

			if (drag.isEmpty())
				stack = ItemStack.EMPTY;
			else if (mouseButton == 0)
				stack = drag.copy();
			else if (ItemHelper.itemsIdentical(get, drag))
				stack = ItemHelper.cloneStack(get, get.getCount() + 1);
			else
				stack = ItemHelper.cloneStack(drag, 1);

			consumer.accept(stack);
			return true;
		}
		return false;
	}

	public ItemStack get() {
		return stack.get();
	}

}
