package astavie.thermallogistics.gui.client;

import astavie.thermallogistics.ThermalLogistics;
import astavie.thermallogistics.attachment.Crafter;
import astavie.thermallogistics.attachment.CrafterFluid;
import astavie.thermallogistics.compat.ICrafterWrapper;
import astavie.thermallogistics.gui.client.tab.TabFluid;
import astavie.thermallogistics.gui.client.tab.TabLink;
import astavie.thermallogistics.gui.client.tab.TabRequests;
import astavie.thermallogistics.gui.container.ContainerCrafter;
import astavie.thermallogistics.util.delegate.IDelegateClient;
import cofh.core.gui.GuiContainerCore;
import cofh.core.gui.element.ElementBase;
import cofh.core.gui.element.ElementButton;
import cofh.core.gui.element.tab.TabInfo;
import cofh.core.util.helpers.BlockHelper;
import cofh.core.util.helpers.StringHelper;
import cofh.thermaldynamics.duct.attachments.filter.FilterLogic;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;

import java.io.IOException;

public class GuiCrafter extends GuiContainerCore {

	private static final String TEX_PATH = "thermaldynamics:textures/gui/connection.png";
	private static final ResourceLocation TEXTURE = new ResourceLocation(TEX_PATH);

	private static final int[][] flagButtonsPos = {{176, 0}, {176, 60}, {216, 0}, {216, 60}, {176, 120}, {216, 120}, {176, 180}, {216, 180}};

	public final Crafter<?, ?, ?> crafter;
	public final ContainerCrafter container;
	private final ICrafterWrapper wrapper;
	private final ElementButton[] flags = new ElementButton[FilterLogic.flagTypes.length];
	private final int buttonSize = 20;

	private TabFluid tab;

	public GuiCrafter(InventoryPlayer inventory, Crafter<?, ?, ?> crafter) {
		super(new ContainerCrafter(inventory, crafter), TEXTURE);
		this.crafter = crafter;
		this.container = (ContainerCrafter) inventorySlots;
		this.name = crafter.getName();
		this.ySize = 204;

		TileEntity tile = BlockHelper.getAdjacentTileEntity(crafter.baseTile, crafter.side);
		if (tile == null)
			this.wrapper = null;
		else
			this.wrapper = ThermalLogistics.getWrapper(tile);

		generateInfo("tab.logistics.crafter");
	}

	@Override
	public void initGui() {
		super.initGui();

		for (int x = 0; x < container.inputWidth; x++)
			for (int y = 0; y < container.inputHeight; y++)
				addElement(createSlot(container.input + x * 18, container.y + container.inputOffset + y * 18, x + y * container.inputWidth, true));
		for (int i = 0; i < crafter.getOutputs().length; i++)
			addElement(createSlot(container.output + i * 18, container.y + (int) (18 * 2.5), i, false));

		addTab(new TabLink(this));
		//noinspection unchecked
		addTab(new TabRequests(this, crafter.getClientDelegate(), crafter.requests, crafter::sendRequestsPacket));

		if (crafter instanceof CrafterFluid) {
			tab = new TabFluid(this);
			addTab(tab);
		}
		if (!myInfo.isEmpty()) {
			myInfo += "\n\n" + StringHelper.localize("tab.thermaldynamics.conChange");
			addTab(new TabInfo(this, myInfo));
		}

		int offset = 6;
		int buttonOffset = buttonSize + offset;

		if (wrapper != null) {
			ElementButton input = new ElementButton(this, container.input - 29, container.y + 9 + (container.inputOffset / 2) - 2, "input", 0, 204, 0, 224, 20, 20, TEX_PATH);
			input.setToolTip("info.logistics.input");
			addElement(input);

			ElementButton output = new ElementButton(this, container.input - 29, container.y + (int) (18 * 2.5) - 2, "output", 0, 204, 0, 224, 20, 20, TEX_PATH);
			output.setToolTip("info.logistics.output");
			addElement(output);
		}

		int x = 89 - (int) (buttonSize * (crafter.flags.size() / 2.0)) - (int) (offset * (crafter.flags.size() / 2.0 - 0.5));
		int y = container.y + (int) (3.5 * 18 + (offset / 2.0));
		for (int i = 0; i < crafter.flags.size(); i++) {
			int flag = crafter.flags.get(i);
			flags[flag] = new ElementButton(this, x - 1 + buttonOffset * i, y, FilterLogic.flagTypes[flag], flagButtonsPos[flag][0], flagButtonsPos[flag][1], flagButtonsPos[flag][0], flagButtonsPos[flag][1] + buttonSize, flagButtonsPos[flag][0], flagButtonsPos[flag][1] + buttonSize * 2, buttonSize, buttonSize, TEX_PATH);
			addElement(flags[flag]);
		}
		setButtons();
	}

	private <C extends Crafter<?, ?, ?>, D extends IDelegateClient<?, C>> ElementBase createSlot(int x, int y, int slot, boolean input) {
		//noinspection unchecked
		return ((D) crafter.getClientDelegate()).createSlot(this, x, y, slot, (C) crafter, input);
	}

	@Override
	protected boolean hasClickedOutside(int x, int y, int left, int top) {
		boolean yes = super.hasClickedOutside(x, y, left, top);
		if (yes && tab != null) {
			x -= left;
			y -= top;
			int x0 = tab.posX() + tab.slot.getPosX();
			int y0 = tab.getPosY() + tab.slot.getPosY();
			if (x >= x0 && y >= y0 && x < x0 + 16 && y < y0 + 16)
				return false;
		}
		return yes;
	}

	@Override
	protected void keyTyped(char characterTyped, int keyPressed) throws IOException {
		if (tab != null && tab.isFullyOpened() && tab.onKeyTyped(characterTyped, keyPressed))
			return;
		super.keyTyped(characterTyped, keyPressed);
	}

	private void setButtons() {
		for (int i = 0; i < flags.length; i++) {
			if (flags[i] != null) {
				boolean b = crafter.values[i];
				int x = flagButtonsPos[i][0] + (b ? buttonSize : 0);
				flags[i].setSheetX(x);
				flags[i].setHoverX(x);
				flags[i].setToolTip("info.thermaldynamics.filter." + flags[i].getName() + (b ? ".on" : ".off"));
			}
		}
	}

	@Override
	protected void updateElementInformation() {
		setButtons();
	}

	@Override
	public void handleElementButtonClick(String buttonName, int mouseButton) {
		for (int i = 0; i < flags.length; i++) {
			ElementButton button = flags[i];
			if (button != null && button.getName().equals(buttonName)) {
				if (crafter.swapFlag(i)) {
					if (crafter.values[i])
						playClickSound(0.8F);
					else
						playClickSound(0.6F);
				}
				setButtons();
				return;
			}
		}
		if (buttonName.equals("input")) {
			crafter.autoInput();
			playClickSound(0.8F);
		} else if (buttonName.equals("output")) {
			crafter.autoOutput();
			playClickSound(0.8F);
		}
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTick, int x, int y) {
		super.drawGuiContainerBackgroundLayer(partialTick, x, y);
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		bindTexture(TEXTURE);
		for (Slot slot : inventorySlots.inventorySlots)
			drawTexturedModalRect(guiLeft + slot.xPos - 1, guiTop + slot.yPos - 1, 7, 122, 18, 18);
	}

}
