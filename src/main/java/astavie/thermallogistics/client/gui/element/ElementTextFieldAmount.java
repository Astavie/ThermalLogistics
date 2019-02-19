package astavie.thermallogistics.client.gui.element;

import cofh.core.gui.GuiContainerCore;
import cofh.core.gui.element.ElementTextFieldLimited;

public class ElementTextFieldAmount extends ElementTextFieldLimited {

	private boolean rightClick = false;

	public ElementTextFieldAmount(GuiContainerCore gui, int posX, int posY, int width, int height) {
		super(gui, posX, posY, width, height);
		setFilter("0123456789", false);
	}

	@Override
	public boolean onMousePressed(int mouseX, int mouseY, int mouseButton) {
		if (mouseButton == 1) {
			rightClick = true;
			this.setText("");
			this.setFocused(true);
			return true;
		}
		return super.onMousePressed(mouseX, mouseY, mouseButton);
	}

	public void onMouseReleased(int mouseX, int mouseY) {
		if (this.rightClick)
			this.rightClick = false;
		else
			super.onMouseReleased(mouseX, mouseY);
	}

}
