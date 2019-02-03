package astavie.thermallogistics.util.delegate;

import astavie.thermallogistics.attachment.CrafterItem;
import astavie.thermallogistics.gui.client.element.ElementSlotItem;
import cofh.core.gui.GuiContainerCore;
import cofh.core.gui.element.ElementBase;
import cofh.core.network.PacketHandler;
import cofh.core.util.helpers.RenderHelper;
import net.minecraft.item.ItemStack;

import java.util.List;

public class DelegateClientItem extends DelegateItem implements IDelegateClient<ItemStack, CrafterItem> {

	public static final DelegateClientItem INSTANCE = new DelegateClientItem();

	@Override
	public void drawStack(GuiContainerCore gui, int x, int y, ItemStack stack) {
		if (!stack.isEmpty()) {
			RenderHelper.enableGUIStandardItemLighting();
			gui.itemRender.renderItemAndEffectIntoGUI(stack, x, y);
			gui.itemRender.renderItemOverlayIntoGUI(gui.getFontRenderer(), stack, x, y, null);
		}
	}

	@Override
	public void addTooltip(GuiContainerCore gui, ItemStack stack, List<String> list) {
		list.addAll(gui.getItemToolTip(stack));
	}

	@Override
	public ElementBase createSlot(GuiContainerCore gui, int x, int y, int slot, CrafterItem crafter, boolean input) {
		return new ElementSlotItem(gui, x, y, input ? () -> crafter.inputs[slot] : () -> crafter.outputs[slot], stack -> PacketHandler.sendToServer(crafter.getPacket(stack, input, slot)));
	}

}
