package astavie.thermallogistics.client.gui.element;

import cofh.core.gui.GuiContainerCore;
import cofh.core.gui.element.ElementTextField;

public class ElementTextFieldClear extends ElementTextField {

	private boolean rightClick = false;

	public ElementTextFieldClear(GuiContainerCore gui, int posX, int posY, int width, int height) {
		super(gui, posX, posY, width, height);
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
