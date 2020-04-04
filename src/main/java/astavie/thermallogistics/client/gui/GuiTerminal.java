package astavie.thermallogistics.client.gui;

import astavie.thermallogistics.client.gui.element.ElementStack;
import astavie.thermallogistics.client.gui.element.ElementTextFieldAmount;
import astavie.thermallogistics.client.gui.element.ElementTextFieldClear;
import astavie.thermallogistics.compat.jei.CompatJEI;
import astavie.thermallogistics.process.Request;
import astavie.thermallogistics.tile.TileTerminal;
import astavie.thermallogistics.util.Shared;
import astavie.thermallogistics.util.StackHandler;
import astavie.thermallogistics.util.type.Type;
import cofh.core.gui.element.ElementBase;
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
import net.minecraftforge.fml.common.Loader;
import org.apache.commons.lang3.tuple.Triple;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public abstract class GuiTerminal<I> extends GuiOverlay implements IFocusGui {

	protected final List<Triple<Type<I>, Long, Boolean>> filter = NonNullList.create();
	protected final TileTerminal<I> tile;

	public Shared<ElementTextField> search = new Shared<>();

	protected ElementSlider slider;
	protected Shared<ElementTextField> amount = new Shared<>(new ElementTextFieldAmount(this, 20, 4, 57, 10, true));

	protected Type<I> selected;

	protected int rows = 2;
	protected int split = 27;
	protected int size;

	private int bar;

	private String cache = "";

	public GuiTerminal(TileTerminal<I> tile, Container container, ResourceLocation texture) {
		super(container, texture);
		this.tile = tile;
	}

	protected void request(Type<I> stack, long amount) {
		tile.terminal.remove(stack, amount);
		updateFilter();

		PacketTileInfo packet = PacketTileInfo.newPacket(tile);
		packet.addByte(0);
		stack.writePacket(packet);
		packet.addLong(amount);
		PacketHandler.sendToServer(packet);
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

		String prev = search.get() == null ? "" : search.get().getText();

		search.accept(new ElementTextFieldClear(this, 80, 5, 88, 10, true) {
			@Override
			protected void onCharacterEntered(boolean success) {
				if (Loader.isModLoaded("jei")) {
					CompatJEI.synchronize(getText());
				}
			}
		});
		slider = new SliderVertical(this, 174, 18, 12, 34, 0);

		if (!prev.isEmpty()) {
			search.get().setText(prev);
		}

		addElement(search.get());
		addElement(slider);

		search.get().setFocused(true);

		slider.setLimits(0, Math.max((filter.size() - 1) / 9 - rows + 1, 0));
		slider.setEnabled(filter.size() > rows * 9);
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

		bar = 20 + rows * 18;
	}

	@Override
	public void updateScreen() {
		super.updateScreen();

		if (tile.refresh || !search.get().getText().equals(cache)) {
			tile.refresh = false;
			cache = search.get().getText();

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

	public Object getStackAt(int mouseX, int mouseY) {
		int i = slider.getValue() * 9;

		a:
		for (int y = 0; y < rows; y++) {
			for (int x = 0; x < 9; x++) {
				int slot = i + x + y * 9;
				if (slot >= filter.size())
					break a;

				int posX = 8 + x * 18;
				int posY = 18 + y * 18;

				if (mouseX >= posX - 1 && mouseX < posX + 17 && mouseY >= posY - 1 && mouseY < posY + 17) {
					return filter.get(slot).getLeft().getAsStack();
				}
			}
		}

		int max = Math.min(8, tile.requests.size());
		for (int j = 0; j < max; j++) {
			Request<?> request = tile.requests.get(tile.requests.size() - j - 1);
			if (mouseX >= 25 + j * 18 && mouseX < 43 + j * 18 && mouseY >= bar - 1 && mouseY < bar + 17) {
				return request.type.getAsStack();
			}
		}

		return null;
	}

	@Override
	public void addTooltips(List<String> tooltip) {
		super.addTooltips(tooltip);

		if (!withinOverlay(mouseX, mouseY)) {
			int i = slider.getValue() * 9;

			a:
			for (int y = 0; y < rows; y++) {
				for (int x = 0; x < 9; x++) {
					int slot = i + x + y * 9;
					if (slot >= filter.size())
						break a;

					int posX = 8 + x * 18;
					int posY = 18 + y * 18;

					if (mouseX >= posX - 1 && mouseX < posX + 17 && mouseY >= posY - 1 && mouseY < posY + 17)
						tooltip.addAll(filter.get(slot).getLeft().getTooltip(this));
				}
			}
		}

		int max = Math.min(8, tile.requests.size());
		for (int j = 0; j < max; j++) {
			Request<I> request = tile.requests.get(tile.requests.size() - j - 1);

			if (mouseX >= 25 + j * 18 && mouseX < 43 + j * 18 && mouseY >= bar - 1 && mouseY < bar + 17) {
				tooltip.addAll(request.type.getTooltip(this));
				if (request.isError()) {
					tooltip.add("");
					if (request.complex) {
						tooltip.add(StringHelper.localize("gui.logistics.terminal.complex"));
					} else {
						tooltip.add(StringHelper.localize("gui.logistics.terminal.missing"));
						for (Map.Entry<Type<?>, Long> entry : request.missing.map.entrySet()) {
							tooltip.add("§7" + StringHelper.localizeFormat("info.logistics.manager.e.1", entry.getValue(), entry.getKey().getDisplayName()));
						}
					}
				}
				tooltip.add("");
				tooltip.add("§7" + StringHelper.localize("gui.logistics.terminal.cancel"));
			}
		}
	}

	@Override
	protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
		RenderHelper.enableGUIStandardItemLighting();

		int i = slider.getValue() * 9;

		a:
		for (int y = 0; y < rows; y++) {
			for (int x = 0; x < 9; x++) {
				int slot = i + x + y * 9;
				if (slot >= filter.size())
					break a;

				int posX = 8 + x * 18;
				int posY = 18 + y * 18;

				Triple<Type<I>, Long, Boolean> triple = filter.get(slot);

				GlStateManager.enableDepth();
				StackHandler.render(this, posX, posY, triple.getLeft().getAsStack(), triple.getMiddle() == 0L ? "Craft" : StringHelper.getScaledNumber(triple.getMiddle()));
			}
		}

		int max = Math.min(8, tile.requests.size());
		for (int j = 0; j < max; j++) {
			Request<?> request = tile.requests.get(tile.requests.size() - j - 1);

			if (request.isError()) {
				int l = j == 0 ? 0 : -1;
				int r = j == 7 ? 0 : +1;

				GlStateManager.disableLighting();
				GlStateManager.disableDepth();
				drawGradientRect(l + 26 + j * 18, bar, r + 42 + j * 18, bar + 16, 0xFFFF4444, 0xFFFF4444);
				GlStateManager.enableLighting();
				GlStateManager.enableDepth();
			}

			GlStateManager.enableDepth();
			StackHandler.render(this, 26 + j * 18, bar, request.type.getAsStack(), StringHelper.getScaledNumber(request.amount));
		}

		GlStateManager.translate(0, 0, 100);
		super.drawGuiContainerForegroundLayer(mouseX, mouseY);
	}

	@Override
	protected void mouseClicked(int mX, int mY, int mouseButton) throws IOException {
		if ((mouseButton == 0 || mouseButton == 1) && requester().getHasStack() && !withinOverlay(mX - guiLeft, mY - guiTop)) {
			int mouseX = mX - guiLeft - 7;
			int mouseY = mY - guiTop - 17;

			if (mouseX >= 0 && mouseX < 9 * 18 && mouseY >= 0 && mouseY < rows * 18) {
				int posX = mouseX / 18;
				int posY = mouseY / 18;

				int slot = slider.getValue() * 9 + posX + posY * 9;
				if (slot < filter.size()) {
					selected = filter.get(slot).getLeft();

					if (mouseButton == 1) {
						amount.get().setText("");
					} else {
						if (filter.get(slot).getRight()) {
							amount.get().setText(Long.toString(StringHelper.isShiftKeyDown() ? selected.maxSize() : selected.normalSize()));
						} else {
							amount.get().setText(Long.toString(Math.min(StringHelper.isShiftKeyDown() ? selected.maxSize() : selected.normalSize(), filter.get(slot).getMiddle())));
						}
					}

					Overlay overlay = new Overlay(this, posX * 18 + 7, posY * 18 + 17, 81, 18);
					overlay.elements.add(new ElementStack(this, 0, 0, selected, false));
					overlay.elements.add(amount.get());

					setOverlay(overlay);
				} else {
					setOverlay(null);
				}

				return;
			} else if (mouseButton == 1 && mouseX >= 18 && mouseX < 9 * 18 && mouseY >= bar - 17 && mouseY < bar + 1) {
				int pos = tile.requests.size() - mouseX / 18;
				if (pos >= 0) {
					Request<?> request = tile.requests.remove(pos);
					int start = request.index;
					int end = tile.requests.size() <= pos ? Integer.MAX_VALUE : tile.requests.get(pos).index;

					PacketTileInfo packet = PacketTileInfo.newPacket(tile);
					packet.addByte(1);
					packet.addInt(start);
					packet.addInt(end);
					PacketHandler.sendToServer(packet);

					playClickSound(1.0F);
					return;
				}
			}
		}

		super.mouseClicked(mX, mY, mouseButton);
	}

	@Override
	protected void keyTyped(char characterTyped, int keyPressed) throws IOException {
		if (Loader.isModLoaded("jei") && CompatJEI.checkKey(keyPressed)) {
			return;
		}

		if (keyPressed == 1) {
			if (selected != null) {
				setOverlay(null);
			} else {
				mc.player.closeScreen();
			}
		} else if (keyPressed == 28 && amount.get().isFocused()) {
			request(selected, amount.get().getText().isEmpty() ? 1 : Long.parseLong(amount.get().getText()));
			setOverlay(null);
		} else super.keyTyped(characterTyped, keyPressed);
	}

	@Override
	public void setOverlay(Overlay overlay) {
		super.setOverlay(overlay);
		if (overlay == null) {
			selected = null;
			amount.get().setFocused(false);
		} else {
			amount.get().onMousePressed(1000, 0, 0);
		}
	}

	@Override
	public void onFocus(ElementTextField text) {
		if (!search.test(text)) {
			search.get().setFocused(false);
		}
	}

	@Override
	public void onLeave(ElementTextField text) {
		if (!search.test(text)) {
			search.get().onMousePressed(1000, 0, 0);
		}
	}

	protected abstract void updateFilter();

	@Override
	protected boolean onMouseWheel(int mouseX, int mouseY, int wheelMovement) {
		return mouseX >= 7 && mouseX < 169 && mouseY >= 17 && mouseY < 71 && slider.onMouseWheel(mouseX, mouseY, wheelMovement);
	}

	@Override
	protected void mouseReleased(int mX, int mY, int mouseButton) {
		mX -= guiLeft;
		mY -= guiTop;

		if (mouseButton >= 0 && mouseButton <= 2) {
			for (TabBase tab : tabs) {
				if (tab.isFullyOpened()) {
					tab.onMouseReleased(mouseX, mouseY);
					break;
				}
			}
		}

		mX += guiLeft;
		mY += guiTop;

		super.mouseReleased(mX, mY, mouseButton);
	}

	@Override
	protected int getCenteredOffset(String string) {
		return 8;
	}

}
