package astavie.thermallogistics.gui.client.delegate;

import astavie.thermallogistics.attachment.Crafter;
import cofh.core.gui.GuiContainerCore;
import cofh.core.gui.element.ElementBase;

public interface IDelegate<I, C extends Crafter<?, ?, I>> {

	void drawStack(GuiContainerCore gui, int x, int y, I stack);

	void drawHover(GuiContainerCore gui, int mouseX, int mouseY, I stack);

	boolean isNull(I stack);

	ElementBase createSlot(GuiContainerCore gui, int x, int y, int slot, C crafter, boolean input);

}
