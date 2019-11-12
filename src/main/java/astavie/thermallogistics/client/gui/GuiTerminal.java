package astavie.thermallogistics.client.gui;

import astavie.thermallogistics.client.gui.element.ElementTextFieldAmount;
import astavie.thermallogistics.client.gui.element.ElementTextFieldClear;
import astavie.thermallogistics.client.gui.tab.TabRequest;
import astavie.thermallogistics.tile.TileTerminal;
import astavie.thermallogistics.util.StackHandler;
import astavie.thermallogistics.util.type.Type;
import cofh.core.gui.GuiContainerCore;
import cofh.core.gui.element.ElementBase;
import cofh.core.gui.element.ElementButtonManaged;
import cofh.core.gui.element.ElementSlider;
import cofh.core.gui.element.ElementTextField;
import cofh.core.gui.element.listbox.SliderVertical;
import cofh.core.gui.element.tab.TabBase;
import cofh.core.network.PacketHandler;
import cofh.core.network.PacketTileInfo;
import cofh.core.util.helpers.MathHelper;
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

	protected ElementTextField search;
	protected ElementSlider slider;

	protected ElementTextField amount;
	protected ElementButtonManaged button;

	protected final List<Triple<Type<I>, Long, Boolean>> filter = NonNullList.create();
	protected final TileTerminal<I> tile;

	protected TabRequest tabRequest;

	protected Type<I> selected;

	protected int rows = 2;
	protected int split = 27;
	protected int size;

	private String cache = "";

	protected void request(I stack, int amount) {
		PacketTileInfo packet = PacketTileInfo.newPacket(tile);
		packet.addByte(0);
		StackHandler.writePacket(packet, stack, tile.getItemClass(), true);
		packet.addInt(amount);
		PacketHandler.sendToServer(packet);
	}

	public GuiTerminal(TileTerminal<I> tile, Container container, ResourceLocation texture) {
		super(container, texture);
		this.tile = tile;
	}

	protected Slot requester() {
		return inventorySlots.inventorySlots.get(0);
	}

	@Override
	public void initGui() {
		super.initGui();

		recalculateSize();

		name = tile.customName;
		if (name.isEmpty())
			name = StringHelper.localize(tile.getTileName());

		search = new ElementTextFieldClear(this, 80, 5, 88, 10);
		slider = new SliderVertical(this, 174, 18, 12, 34, 0);

		addElement(search);
		addElement(slider);

		slider.setLimits(0, Math.max((filter.size() - 1) / 9 - rows + 1, 0));
		slider.setEnabled(filter.size() > rows * 9);

		amount = new ElementTextFieldAmount(this, 44, 59, 71, 10);
		button = new ElementButtonManaged(this, 118, 56, 50, 16, "") {
			@Override
			public void onClick() {
				request(selected.getAsStack(), amount.getText().isEmpty() ? 1 : Integer.parseInt(amount.getText()));
			}
		};

		boolean visible = requester().getHasStack();
		amount.setVisible(visible);
		button.setVisible(visible);

		button.setText(StringHelper.localize("gui.logistics.terminal.request"));
		addElement(amount);
		addElement(button);
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTick, int x, int y) {
		GlStateManager.color(1, 1, 1, 1);
		bindTexture(texture);

		drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, split);
		for (int i = 0; i < rows - 1; i++) {
			drawTexturedModalRect(guiLeft, guiTop + split + 18 * i, 0, split, xSize, 18);
		}
		drawTexturedModalRect(guiLeft, guiTop + split + 18 * (rows - 1), 0, split + 18, xSize, size - split - 18);

		mouseX = x - guiLeft;
		mouseY = y - guiTop;

		GlStateManager.pushMatrix();
		GlStateManager.translate(guiLeft, guiTop, 0.0F);
		drawElements(partialTick, false);
		drawTabs(partialTick, false);
		GlStateManager.popMatrix();
	}

	protected void recalculateSize() {
		int prev = rows;
		rows = MathHelper.clamp((height - size) / 18 + 2, 1, 9);

		ySize -= prev * 18;
		ySize += rows * 18;
		guiTop = (height - ySize) / 2;

		for (Slot slot : inventorySlots.inventorySlots) {
			if (slot.yPos > split) {
				slot.yPos -= prev * 18;
				slot.yPos += rows * 18;
			}
		}
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

		long count = 1L;
		if (!amount.getText().isEmpty())
			count = Long.parseLong(amount.getText());

		if (amount.isEnabled()) {
			for (Type<I> item : tile.terminal.types()) {
				if (isSelected(item)) {
					button.setEnabled(count <= Integer.MAX_VALUE && (tile.terminal.craftable(item) || tile.terminal.amount(item) >= count));
					break;
				}
			}
		}

		if (tile.refresh || !search.getText().equals(cache)) {
			tile.refresh = false;
			cache = search.getText();

			tabRequest.setList(tile.requests.stacks());

			updateFilter();


			slider.setLimits(0, Math.max((filter.size() - 1) / 9 - rows + 1, 0));
			slider.setEnabled(filter.size() > rows * 9);
		}
	}

	@Override
	public ElementBase addElement(ElementBase element) {
		if (element.getPosY() > split) {
			element.setPosition(element.getPosX(), element.getPosY() + (rows - 2) * 18);
		} else if (element.getPosY() + element.getHeight() > split) {
			element.setSize(element.getWidth(), element.getHeight() + (rows - 2) * 18);
		}
		return super.addElement(element);
	}

	@Override
	public TabBase addTab(TabBase tab) {
		tab.setOffsets(getTabOffsetX(), getTabOffsetY() + (rows - 2) * 18);
		tab.setCurrentShift(0, 0);
		return super.addTab(tab);
	}

	protected abstract int getTabOffsetX();

	protected abstract int getTabOffsetY();

	@Override
	public void addTooltips(List<String> tooltip) {
		super.addTooltips(tooltip);

		if (selected != null && button.isVisible() && mouseX >= 25 && mouseX < 43 && mouseY >= 38 + (rows - 1) * 18 && mouseY < 38 + rows * 18)
			tooltip.addAll(StackHandler.getTooltip(this, selected.getAsStack()));

		int i = slider.getValue() * 9;

		for (int y = 0; y < rows; y++) {
			for (int x = 0; x < 9; x++) {
				int slot = i + x + y * 9;
				if (slot >= filter.size())
					return;

				int posX = 8 + x * 18;
				int posY = 18 + y * 18;

				if (mouseX >= posX - 1 && mouseX < posX + 17 && mouseY >= posY - 1 && mouseY < posY + 17)
					tooltip.addAll(StackHandler.getTooltip(this, filter.get(slot).getLeft().getAsStack()));
			}
		}
	}

	@Override
	protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
		super.drawGuiContainerForegroundLayer(mouseX, mouseY);

		RenderHelper.enableGUIStandardItemLighting();
		if (selected != null && button.isVisible())
			StackHandler.render(this, 26, 38 + (rows - 1) * 18, selected.getAsStack(), false);

		int i = slider.getValue() * 9;

		for (int y = 0; y < rows; y++) {
			for (int x = 0; x < 9; x++) {
				int slot = i + x + y * 9;
				if (slot >= filter.size())
					return;

				int posX = 8 + x * 18;
				int posY = 18 + y * 18;

				Triple<Type<I>, Long, Boolean> triple = filter.get(slot);

				if (isSelected(triple.getLeft())) {
					GlStateManager.disableLighting();
					GlStateManager.disableDepth();
					drawGradientRect(posX, posY, posX + 16, posY + 16, 0xFFC5C5C5, 0xFFC5C5C5);
					GlStateManager.enableLighting();
					GlStateManager.enableDepth();
				}

				GlStateManager.enableDepth();
				StackHandler.render(this, posX, posY, triple.getLeft().getAsStack(), triple.getMiddle() == 0L ? "Craft" : StringHelper.getScaledNumber(triple.getMiddle()));
			}
		}
	}

	@Override
	protected void mouseClicked(int mX, int mY, int mouseButton) throws IOException {
		if (button.isVisible()) {
			int mouseX = mX - guiLeft - 7;
			int mouseY = mY - guiTop - 17;

			if (mouseX >= 0 && mouseX < 9 * 18 && mouseY >= 0 && mouseY < rows * 18) {
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

	protected abstract boolean isSelected(Type<I> stack);

	protected abstract void updateFilter();

	protected abstract void updateAmount(Triple<Type<I>, Long, Boolean> stack);

	@Override
	protected boolean onMouseWheel(int mouseX, int mouseY, int wheelMovement) {
		return mouseX >= 7 && mouseX < 169 && mouseY >= 17 && mouseY < 71 && slider.onMouseWheel(mouseX, mouseY, wheelMovement);
	}

	@Override
	protected int getCenteredOffset(String string) {
		return 8;
	}

}
