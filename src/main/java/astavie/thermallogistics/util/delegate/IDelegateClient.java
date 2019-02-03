package astavie.thermallogistics.util.delegate;

import astavie.thermallogistics.attachment.Crafter;
import cofh.core.gui.GuiContainerCore;
import cofh.core.gui.element.ElementBase;

import java.util.List;

public interface IDelegateClient<I, C extends Crafter<?, ?, I>> extends IDelegate<I> {

	void drawStack(GuiContainerCore gui, int x, int y, I stack);

	void addTooltip(GuiContainerCore gui, I stack, List<String> list);

	ElementBase createSlot(GuiContainerCore gui, int x, int y, int slot, C crafter, boolean input);

}
