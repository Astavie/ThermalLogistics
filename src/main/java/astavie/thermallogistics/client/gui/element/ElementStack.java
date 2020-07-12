package astavie.thermallogistics.client.gui.element;

import astavie.thermallogistics.util.type.Type;
import cofh.core.gui.GuiContainerCore;
import cofh.core.gui.element.ElementBase;

import java.util.List;

public class ElementStack extends ElementBase {

	private final Type<?> type;
	private final boolean tooltip;

	public ElementStack(GuiContainerCore gui, int posX, int posY, Type<?> type, boolean tooltip) {
		super(gui, posX, posY, 18, 18);
		this.type = type;
		this.tooltip = tooltip;
	}

	@Override
	public void drawBackground(int mouseX, int mouseY, float gameTicks) {
	}

	@Override
	public void drawForeground(int mouseX, int mouseY) {
		type.render(gui, posX + 1, posY + 1);
	}

	@Override
	public void addTooltip(List<String> list) {
		if (tooltip) {
			list.addAll(type.getTooltip(gui));
		}
	}

}
