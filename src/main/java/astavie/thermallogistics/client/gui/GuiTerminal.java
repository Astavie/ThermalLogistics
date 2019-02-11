package astavie.thermallogistics.client.gui;

import astavie.thermallogistics.tile.TileTerminal;
import astavie.thermallogistics.util.StackHandler;
import cofh.core.gui.GuiContainerCore;
import cofh.core.gui.element.ElementButtonManaged;
import cofh.core.gui.element.ElementSlider;
import cofh.core.gui.element.ElementTextField;
import cofh.core.gui.element.ElementTextFieldLimited;
import cofh.core.gui.element.listbox.SliderVertical;
import cofh.core.network.PacketHandler;
import cofh.core.network.PacketTileInfo;
import cofh.core.util.helpers.RenderHelper;
import cofh.core.util.helpers.StringHelper;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.lang3.tuple.Triple;

import java.io.IOException;
import java.util.List;

public abstract class GuiTerminal<I> extends GuiContainerCore {

	protected final ElementTextField search = new ElementTextField(this, 80, 5, 88, 10);
	protected final ElementSlider slider = new SliderVertical(this, 174, 18, 12, 52, 0);

	protected final ElementTextField amount = new ElementTextFieldLimited(this, 44, 77, 70, 10).setFilter("0123456789", false);

	protected final List<Triple<I, Long, Boolean>> filter = NonNullList.create();
	protected final TileTerminal<I> tile;

	protected I selected;

	protected final ElementButtonManaged button = new ElementButtonManaged(this, 117, 74, 50, 16, "") {
		@Override
		public void onClick() {
			PacketTileInfo packet = PacketTileInfo.newPacket(tile);
			packet.addByte(0);
			StackHandler.writePacket(packet, selected, tile.getItemClass(), true);
			packet.addInt(Integer.parseInt(amount.getText()));
			PacketHandler.sendToServer(packet);
		}
	};

	private String cache = "";

	public GuiTerminal(TileTerminal<I> tile, Container container, ResourceLocation texture) {
		super(container, texture);
		this.tile = tile;
	}

	private Slot requester() {
		return inventorySlots.inventorySlots.get(0);
	}

	@Override
	public void initGui() {
		super.initGui();
		name = tile.customName;
		if (name.isEmpty())
			name = StringHelper.localize(tile.getTileName());

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
		amount.setVisible(visible && selected != null);
		button.setVisible(visible);

		if (!visible)
			selected = null;

		button.setEnabled(false);

		if (amount.isEnabled() && !amount.getText().isEmpty()) {
			for (Triple<I, Long, Boolean> stack : tile.terminal) {
				if (isSelected(stack.getLeft())) {
					long parse = Long.parseLong(amount.getText());
					button.setEnabled(parse <= Integer.MAX_VALUE && (stack.getRight() || stack.getMiddle() >= parse));
					break;
				}
			}
		}

		if (tile.refresh || !search.getText().equals(cache)) {
			tile.refresh = false;
			cache = search.getText();

			updateFilter();

			slider.setLimits(0, Math.max((filter.size() - 1) / 9 - 2, 0));
			slider.setEnabled(filter.size() > 27);
		}
	}

	@Override
	public void addTooltips(List<String> tooltip) {
		super.addTooltips(tooltip);

		if (selected != null && button.isVisible() && mouseX >= 25 && mouseX < 43 && mouseY >= 73 && mouseY < 91)
			tooltip.addAll(StackHandler.getTooltip(this, selected));

		int i = slider.getValue() * 9;

		for (int y = 0; y < 3; y++) {
			for (int x = 0; x < 9; x++) {
				int slot = i + x + y * 9;
				if (slot >= filter.size())
					return;

				int posX = 8 + x * 18;
				int posY = 18 + y * 18;

				if (mouseX >= posX - 1 && mouseX < posX + 17 && mouseY >= posY - 1 && mouseY < posY + 17)
					tooltip.addAll(StackHandler.getTooltip(this, filter.get(slot).getLeft()));
			}
		}
	}

	@Override
	protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
		super.drawGuiContainerForegroundLayer(mouseX, mouseY);

		RenderHelper.enableGUIStandardItemLighting();
		if (selected != null && button.isVisible())
			StackHandler.render(this, 26, 74, selected, false);

		int i = slider.getValue() * 9;

		for (int y = 0; y < 3; y++) {
			for (int x = 0; x < 9; x++) {
				int slot = i + x + y * 9;
				if (slot >= filter.size())
					return;

				int posX = 8 + x * 18;
				int posY = 18 + y * 18;

				Triple<I, Long, Boolean> triple = filter.get(slot);

				if (isSelected(triple.getLeft())) {
					GlStateManager.disableLighting();
					GlStateManager.disableDepth();
					drawGradientRect(posX, posY, posX + 16, posY + 16, 0xFFC5C5C5, 0xFFC5C5C5);
					GlStateManager.enableLighting();
					GlStateManager.enableDepth();
				}

				GlStateManager.enableDepth();
				StackHandler.render(this, posX, posY, triple.getLeft(), triple.getMiddle() == 0L ? "Craft" : StringHelper.getScaledNumber(triple.getMiddle()));
			}
		}
	}

	@Override
	protected void mouseClicked(int mX, int mY, int mouseButton) throws IOException {
		if (button.isVisible()) {
			int mouseX = mX - guiLeft - 7;
			int mouseY = mY - guiTop - 17;

			if (mouseX >= 0 && mouseX < 9 * 18 && mouseY >= 0 && mouseY < 3 * 18) {
				int posX = mouseX / 18;
				int posY = mouseY / 18;

				int slot = slider.getValue() * 9 + posX + posY * 9;
				if (slot < filter.size()) {
					selected = filter.get(slot).getLeft();
					updateAmount(filter.get(slot));
				} else selected = null;

				return;
			}
		}

		super.mouseClicked(mX, mY, mouseButton);
	}

	@Override
	protected void keyTyped(char characterTyped, int keyPressed) throws IOException {
		if (keyPressed == 28 && amount.isFocused())
			button.onMousePressed(0, 0, 0);
		else super.keyTyped(characterTyped, keyPressed);
	}

	protected abstract boolean isSelected(I stack);

	protected abstract void updateFilter();

	protected abstract void updateAmount(Triple<I, Long, Boolean> stack);

	@Override
	protected boolean onMouseWheel(int mouseX, int mouseY, int wheelMovement) {
		return mouseX >= 7 && mouseX < 169 && mouseY >= 17 && mouseY < 71 && slider.onMouseWheel(mouseX, mouseY, wheelMovement);
	}

	@Override
	protected int getCenteredOffset(String string) {
		return 8;
	}

}
