package astavie.thermallogistics.gui.client.delegate;

import astavie.thermallogistics.attachment.CrafterItem;
import astavie.thermallogistics.gui.client.element.ElementSlotItem;
import cofh.core.gui.GuiContainerCore;
import cofh.core.gui.element.ElementBase;
import cofh.core.network.PacketHandler;
import cofh.core.util.helpers.RenderHelper;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.client.config.GuiUtils;

public class DelegateItemStack implements IDelegate<ItemStack, CrafterItem> {

	public static final DelegateItemStack INSTANCE = new DelegateItemStack();

	@Override
	public void drawStack(GuiContainerCore gui, int x, int y, ItemStack stack) {
		if (!stack.isEmpty()) {
			RenderHelper.enableGUIStandardItemLighting();
			gui.itemRender.renderItemAndEffectIntoGUI(stack, x, y);
			gui.itemRender.renderItemOverlayIntoGUI(gui.getFontRenderer(), stack, x, y, null);
		}
	}

	@Override
	public void drawHover(GuiContainerCore gui, int mouseX, int mouseY, ItemStack stack) {
		if (!stack.isEmpty()) {
			FontRenderer font = stack.getItem().getFontRenderer(stack);
			if (font == null)
				font = gui.getFontRenderer();
			GuiUtils.drawHoveringText(stack, gui.getItemToolTip(stack), mouseX, mouseY, gui.width, gui.height, -1, font);
		}
	}

	@Override
	public boolean isNull(ItemStack stack) {
		return stack == null || stack.isEmpty();
	}

	@Override
	public ElementBase createSlot(GuiContainerCore gui, int x, int y, int slot, CrafterItem crafter, boolean input) {
		return new ElementSlotItem(gui, x, y, input ? () -> crafter.inputs[slot] : () -> crafter.outputs[slot], stack -> PacketHandler.sendToServer(crafter.getPacket(stack, input, slot)));
	}

}
