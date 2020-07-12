package astavie.thermallogistics.client.gui.element;

import astavie.thermallogistics.client.gui.IFocusGui;
import cofh.core.gui.GuiContainerCore;
import cofh.core.gui.element.ElementTextField;
import cofh.core.gui.element.ElementTextFieldLimited;

public class ElementTextFieldAmount extends ElementTextFieldLimited {

	private boolean rightClick = false;
	private boolean permanent;

	public ElementTextFieldAmount(GuiContainerCore gui, int posX, int posY, int width, int height, boolean permanent) {
		super(gui, posX, posY, width, height);
		setFilter("0123456789", false);
		setMaxLength((short) 9);

		this.permanent = permanent;
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
		if (this.rightClick) {
			this.rightClick = false;
		} else if (!permanent) {
			super.onMouseReleased(mouseX, mouseY);
		}
	}

	@Override
	public ElementTextField setFocused(boolean focused) {
		boolean prev = isFocused();

		super.setFocused(focused);

		if (prev == focused)
			return this;

		if (gui instanceof IFocusGui) {
			if (focused)
				((IFocusGui) gui).onFocus(this);
			else
				((IFocusGui) gui).onLeave(this);
		}

		return this;
	}

}
