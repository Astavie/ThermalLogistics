package astavie.thermallogistics.util.delegate;

import astavie.thermallogistics.attachment.Crafter;
import cofh.core.gui.GuiContainerCore;
import cofh.core.gui.element.ElementBase;

public interface IDelegateClient<I, C extends Crafter<?, ?, I>> extends IDelegate<I> {

	void drawStack(GuiContainerCore gui, int x, int y, I stack);

	void drawHover(GuiContainerCore gui, int mouseX, int mouseY, I stack);

	ElementBase createSlot(GuiContainerCore gui, int x, int y, int slot, C crafter, boolean input);

}
