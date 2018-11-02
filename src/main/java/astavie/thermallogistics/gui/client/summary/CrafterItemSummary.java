package astavie.thermallogistics.gui.client.summary;

import astavie.thermallogistics.attachment.CrafterItem;
import astavie.thermallogistics.proxy.ProxyClient;
import cofh.core.gui.GuiContainerCore;
import net.minecraft.item.ItemStack;

public class CrafterItemSummary implements ICrafterSummary<CrafterItem> {

	@Override
	public void renderSummary(CrafterItem crafter, GuiContainerCore gui, int x, int y, int textColor) {
		ItemStack input = null, output = null;
		boolean bI = false, bO = false;

		boolean b = false;
		for (ItemStack stack : crafter.getInputs()) {
			if (!stack.isEmpty()) {
				if (input == null)
					input = stack;
				if (b) {
					bI = true;
					break;
				} else b = true;
			}
		}

		b = false;
		for (ItemStack stack : crafter.getOutputs()) {
			if (!stack.isEmpty()) {
				if (output == null)
					output = stack;
				if (b) {
					bI = true;
					break;
				} else b = true;
			}
		}

		if (input != null)
			gui.drawItemStack(input, x, y, true, null);
		if (bI)
			gui.getFontRenderer().drawString("...", x + 19, y + 4, textColor);

		gui.drawIcon(ProxyClient.ICON_ARROW_RIGHT, x + 26, y);

		if (output != null)
			gui.drawItemStack(output, x + 44, y, true, null);
		if (bO)
			gui.getFontRenderer().drawString("...", x + 63, y + 4, textColor);
	}

}
