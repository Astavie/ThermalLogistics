package astavie.thermallogistics.gui.client;

import astavie.thermallogistics.ThermalLogistics;
import astavie.thermallogistics.gui.container.ContainerTerminal;
import cofh.core.gui.GuiContainerCore;
import cofh.core.gui.element.ElementButtonManaged;
import cofh.core.gui.element.ElementSlider;
import cofh.core.gui.element.ElementTextField;
import cofh.core.gui.element.ElementTextFieldLimited;
import cofh.core.gui.element.listbox.SliderVertical;
import cofh.core.util.helpers.StringHelper;
import net.minecraft.inventory.Slot;
import net.minecraft.util.ResourceLocation;

public abstract class GuiTerminal extends GuiContainerCore {

	protected final ElementTextField search = new ElementTextField(this, 80, 5, 88, 10);
	protected final ElementSlider slider = new SliderVertical(this, 174, 18, 12, 52, 0);

	protected final ElementTextField amount = new ElementTextFieldLimited(this, 44, 77, 70, 10).setFilter("0123456789", false);
	protected final ElementButtonManaged button = new ElementButtonManaged(this, 117, 74, 50, 16, "") {
		@Override
		public void onClick() {
			request();
		}
	};

	protected final ContainerTerminal terminal;

	public GuiTerminal(ContainerTerminal container) {
		super(container, new ResourceLocation(ThermalLogistics.MODID, "textures/gui/terminal.png"));
		this.terminal = container;
	}

	private Slot requester() {
		return inventorySlots.inventorySlots.get(0);
	}

	protected abstract void request();

	@Override
	public void initGui() {
		super.initGui();
		name = terminal.tile.customName;
		if (name.isEmpty())
			name = StringHelper.localize(terminal.tile.getTileName());

		elements.add(search);
		elements.add(slider);

		boolean visible = requester().getHasStack();
		amount.setVisible(visible);
		button.setVisible(visible);

		button.setText(StringHelper.localize("gui.logistics.terminal.request"));
		elements.add(amount);
		elements.add(button);
	}

	@Override
	public void updateScreen() {
		super.updateScreen();
		boolean visible = requester().getHasStack();
		amount.setVisible(visible);
		button.setVisible(visible);
	}

	@Override
	protected boolean onMouseWheel(int mouseX, int mouseY, int wheelMovement) {
		return mouseX >= 7 && mouseX < 169 && mouseY >= 17 && mouseY < 71 && slider.onMouseWheel(mouseX, mouseY, wheelMovement);
	}

	@Override
	protected int getCenteredOffset(String string) {
		return 8;
	}

}
