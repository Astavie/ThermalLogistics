package astavie.thermallogistics.client.gui;

import astavie.thermallogistics.ThermalLogistics;
import astavie.thermallogistics.attachment.CrafterFluid;
import astavie.thermallogistics.attachment.CrafterItem;
import astavie.thermallogistics.attachment.IAttachmentCrafter;
import astavie.thermallogistics.attachment.Recipe;
import astavie.thermallogistics.client.gui.element.ElementButtonLinks;
import astavie.thermallogistics.client.gui.element.ElementSlot;
import astavie.thermallogistics.client.gui.tab.TabFluid;
import astavie.thermallogistics.client.gui.tab.TabStackList;
import astavie.thermallogistics.compat.ICrafterWrapper;
import astavie.thermallogistics.container.ContainerCrafter;
import astavie.thermallogistics.util.StackHandler;
import cofh.core.gui.element.ElementButton;
import cofh.core.gui.element.tab.TabInfo;
import cofh.core.gui.element.tab.TabRedstoneControl;
import cofh.core.network.PacketHandler;
import cofh.core.network.PacketTileInfo;
import cofh.core.util.helpers.BlockHelper;
import cofh.core.util.helpers.StringHelper;
import cofh.thermaldynamics.duct.attachments.ConnectionBase;
import cofh.thermaldynamics.duct.attachments.filter.FilterLogic;

import com.google.common.primitives.Ints;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.FluidStack;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class GuiCrafter extends GuiOverlay implements IFluidGui {

	public static final String ICON_PATH = ThermalLogistics.MOD_ID + ":textures/gui/icons/crafter.png";

	private static final String TEX_PATH = ThermalLogistics.MOD_ID + ":textures/gui/crafter.png";
	private static final ResourceLocation TEXTURE = new ResourceLocation(TEX_PATH);

	private static final int[][] levelButtonPos = {{-1, -1}, {176, 180}, {80, 204}};
	private static final int[][] flagButtonsPos = {{176, 0}, {176, 60}, {216, 0}, {216, 60}, {176, 120}, {216, 120}, {176, 180}, {216, 180},};

	public final List<ElementSlot<?>> slots = new LinkedList<>();
	public final List<ElementButton> links = new LinkedList<>();

	private final ICrafterWrapper<?> wrapper;
	public final IAttachmentCrafter<?> crafter;
	private final ConnectionBase attachment;
	public TabFluid tab = null;

	private ElementButton splitButton;
	private ElementButton circuitButton;
	private ElementButton[] flagButtons = new ElementButton[0];
	private ElementButton[] levelButtons = new ElementButton[FilterLogic.defaultLevels.length];

	public ElementButton decRetainSize;
	public ElementButton incRetainSize;

	public boolean isAdvItemFilter;

	int minRetainSize;
	int maxRetainSize;

	private int buttonSize;
	private FluidStack fluid = null;

	public <C extends ConnectionBase & IAttachmentCrafter<?>> GuiCrafter(InventoryPlayer inventoryPlayer, C crafter) {
		super(new ContainerCrafter(inventoryPlayer, crafter), TEXTURE);
		this.crafter = crafter;
		this.attachment = crafter;
		this.attachment.getFilter();
		this.name = attachment.getName();
		this.ySize = 238;

		TileEntity tile = BlockHelper.getAdjacentTileEntity(attachment.baseTile, attachment.side);
		if (tile == null) {
			wrapper = null;
		} else {
			wrapper = ThermalLogistics.INSTANCE.getWrapper(tile.getClass());
		}

		this.isAdvItemFilter = attachment.allowDuctConnection() && attachment.filter.canAlterFlag(FilterLogic.levelRetainSize);

		String info = attachment.getInfo();
		if (info != null)
			generateInfo(info);
	}

	@Override
	public void initGui() {
		super.initGui();

		if (!myInfo.isEmpty()) {
			myInfo += "\n\n" + StringHelper.localize("tab.thermaldynamics.conChange");
			addTab(new TabInfo(this, myInfo));
		}

		if (attachment.canAlterRS())
			addTab(new TabRedstoneControl(this, attachment));

		if (attachment instanceof CrafterFluid)
			addTab(tab = new TabFluid(this, this));

		addTab(new TabStackList<>(this, crafter, true));
		addTab(new TabStackList<>(this, crafter, false));

		int[] flagNums = new int[attachment.filter.validFlags().length - 1];
		System.arraycopy(attachment.filter.validFlags(), 1, flagNums, 0, flagNums.length);

		flagButtons = new ElementButton[attachment.filter.numFlags()];

		int[] levelNums = attachment.filter.getValidLevels();
		levelButtons = new ElementButton[FilterLogic.defaultLevels.length];

		int buttonNo = flagNums.length + levelNums.length;
		if (attachment.type > 0)
			buttonNo+=2;

		if (buttonNo != 0) {
			buttonSize = 20;
			int button_offset = buttonSize + 4;
			int x0 = xSize / 2 - buttonNo * (button_offset / 2) + 2;
			int y0 = 20 + 38 + 2 * 18 + 8 + 14;

			if (attachment.type > 0) {
				splitButton = new ElementButton(this, x0, y0, "split", 0, 0, 0, buttonSize, 0, buttonSize * 2, buttonSize, buttonSize, ICON_PATH);
				addElement(splitButton);

				circuitButton = new ElementButton(this, x0 + button_offset, y0, "circuit", 132, 0, 132, buttonSize, 132, buttonSize * 2, buttonSize, buttonSize, ICON_PATH);
				addElement(circuitButton);
			}

			int offset = attachment.type > 0 ? 2 : 0;

			for (int i = 0; i < flagNums.length; i++) {
				int j = flagNums[i];
				flagButtons[j] = new ElementButton(this, x0 + button_offset * (i + offset), y0, attachment.filter.flagType(j), flagButtonsPos[j][0], flagButtonsPos[j][1], flagButtonsPos[j][0], flagButtonsPos[j][1] + buttonSize, flagButtonsPos[j][0], flagButtonsPos[j][1] + buttonSize * 2, buttonSize, buttonSize, TEX_PATH);
				addElement(flagButtons[j]);
			}
			for (int i = 0; i < levelNums.length; i++) {
				int j = levelNums[i];
				levelButtons[j] = new ElementButton(this, x0 + button_offset * (i + offset + flagNums.length), y0, FilterLogic.levelNames[j], levelButtonPos[j][0], levelButtonPos[j][1], levelButtonPos[j][0], levelButtonPos[j][1] + buttonSize, buttonSize, buttonSize, TEX_PATH);
				addElement(levelButtons[j]);
			}
		}

		decRetainSize = new ElementButton(this, 137, 52, "DecRetainSize", 216, 120, 216, 134, 216, 148, 14, 14, TEX_PATH).setToolTip("info.thermaldynamics.filter.decRetainSize");
		incRetainSize = new ElementButton(this, 153, 52, "IncRetainSize", 230, 120, 230, 134, 230, 148, 14, 14, TEX_PATH).setToolTip("info.thermaldynamics.filter.incRetainSize");

		if (isAdvItemFilter) {
			addElement(decRetainSize);
			addElement(incRetainSize);
			minRetainSize = FilterLogic.minLevels[attachment.filter.type][FilterLogic.levelRetainSize];
			maxRetainSize = FilterLogic.maxLevels[attachment.filter.type][FilterLogic.levelRetainSize];
		}

		if (wrapper != null) {
			ElementButton button = new ElementButton(this, xSize - 32, 20 + 48, "import", 80, 0, 80, 16, 16, 16, ICON_PATH);
			button.setToolTip("info.logistics.import");
			addElement(button);
		}
	}

	private void setButtons() {
		if (splitButton != null) {
			int x = Math.min(CrafterItem.SIZE[attachment.type] / crafter.getCrafters().size() - 1, 3) * buttonSize;
			splitButton.setSheetX(x);
			splitButton.setHoverX(x);
			splitButton.setToolTipLocalized(StringHelper.localizeFormat("info.logistics.split", crafter.getCrafters().size()));
		}
		if (circuitButton != null) {
			int x = 132;
			if (!crafter.processParallel())
				x += buttonSize;
			
			circuitButton.setSheetX(x);
			circuitButton.setHoverX(x);
			circuitButton.setDisabledX(x);

			circuitButton.setEnabled(crafter.getCrafters().size() > 1);
			circuitButton.setToolTipLocalized(StringHelper.localize("info.logistics." + (crafter.processParallel() ? "parallel" : "series")));
		}
		for (int i = 0; i < flagButtons.length; i++) {
			if (flagButtons[i] != null) {
				boolean b = attachment.filter.getFlag(i);
				int x = flagButtonsPos[i][0] + (b ? buttonSize : 0);
				flagButtons[i].setSheetX(x);
				flagButtons[i].setHoverX(x);
				flagButtons[i].setToolTip("info.thermaldynamics.filter." + flagButtons[i].getName() + (b ? ".on" : ".off"));
			}
		}
		for (int i = 0; i < levelButtons.length; i++) {
			if (levelButtons[i] != null) {
				int level = attachment.filter.getLevel(i);
				int x = levelButtonPos[i][0] + level * buttonSize;
				levelButtons[i].setSheetX(x);
				levelButtons[i].setHoverX(x);
				levelButtons[i].setToolTip("info.thermaldynamics.filter." + levelButtons[i].getName() + "." + level);
			}
		}

		elements.removeAll(slots);
		elements.removeAll(links);
		slots.clear();
		links.clear();

		if (attachment.type > 0) {
			int slots = CrafterItem.SIZE[attachment.type];
			int recipeSlots = slots / crafter.getRecipes().size();

			int start = slots * 9 + (crafter.getRecipes().size() - 1);
			int x0 = xSize / 2 - start;
			if (attachment.type == 4) {
				x0 -= 17;
			}
			int y0 = 20 + 20;

			for (int i = 0; i < crafter.getRecipes().size(); i++) {
				for (int x = 0; x < recipeSlots; x++) {
					int posX = x0 + (x + i * recipeSlots) * 18 + i * 2;

					// Add slots
					Slot<?> up = new Slot<>(crafter, i, true, x * 2);
					this.slots.add((ElementSlot<?>) addElement(StackHandler.getSlot(this, posX, y0, up)));

					Slot<?> down = new Slot<>(crafter, i, true, x * 2 + 1);
					this.slots.add((ElementSlot<?>) addElement(StackHandler.getSlot(this, posX, y0 + 18, down)));

					Slot<?> output = new Slot<>(crafter, i, false, x);
					this.slots.add((ElementSlot<?>) addElement(StackHandler.getSlot(this, posX, y0 + 38, output)));
				}

				// Add link info
				int pos0 = x0 + (i * recipeSlots) * 18 + i * 2;
				this.links.add((ElementButtonLinks) addElement(new ElementButtonLinks(this, pos0 + (recipeSlots * 18) / 2 - 8, 20, i)));

				// Add enabled / disabled
				int y = 20 + 38 + 2 * 18 + 8 - 1;
				int sheetY = crafter.getRecipes().get(i).enabled ? 10 : 0;
				final int j = i;
				this.links.add((ElementButton) addElement(new ElementButton(this, pos0 + (recipeSlots * 18) / 2 - 5, y, 10, 10, 96, sheetY, 106, sheetY, ICON_PATH) {
					@Override
					public void onClick() {
						// Send to server
						PacketTileInfo packet = ((ConnectionBase) crafter).getNewPacket(ConnectionBase.NETWORK_ID.GUI);
						packet.addByte(6);
						packet.addInt(j);
						PacketHandler.sendToServer(packet);
					}
				}.setToolTip(crafter.getRecipes().get(i).enabled ? "info.logistics.disable" : "info.logistics.enable")));
			}
		} else {
			int x0 = xSize / 2;
			int y0 = 20 + 38;

			// Add slots
			Slot<?> left = new Slot<>(crafter, 0, true, 0);
			this.slots.add((ElementSlot<?>) addElement(StackHandler.getSlot(this, x0 - 18, y0, left)));

			Slot<?> right = new Slot<>(crafter, 0, true, 1);
			this.slots.add((ElementSlot<?>) addElement(StackHandler.getSlot(this, x0, y0, right)));

			Slot<?> output = new Slot<>(crafter, 0, false, 0);
			this.slots.add((ElementSlot<?>) addElement(StackHandler.getSlot(this, x0 - 9, y0 + 20, output)));

			// Add link info
			this.links.add((ElementButtonLinks) addElement(new ElementButtonLinks(this, 80, 38, 0)));

			// Add enabled / disabled
			int y = 20 + 38 + 2 * 18 + 8 - 1;
			int sheetY = crafter.getRecipes().get(0).enabled ? 10 : 0;
			this.links.add((ElementButton) addElement(new ElementButton(this, 83, y, 10, 10, 96, sheetY, 106, sheetY, ICON_PATH) {
				@Override
				public void onClick() {
					// Send to server
					PacketTileInfo packet = ((ConnectionBase) crafter).getNewPacket(ConnectionBase.NETWORK_ID.GUI);
					packet.addByte(6);
					packet.addInt(0);
					PacketHandler.sendToServer(packet);
				}
			}.setToolTip(crafter.getRecipes().get(0).enabled ? "info.logistics.disable" : "info.logistics.enable")));
		}

		if (overlay != null) {
			elements.remove(overlay);
			elements.add(overlay);
		}
	}

	@Override
	protected void updateElementInformation() {
		setButtons();

		if (isAdvItemFilter) {
			int qty = attachment.filter.getLevel(FilterLogic.levelRetainSize);
			if (qty > minRetainSize) {
				decRetainSize.setActive();
			} else {
				decRetainSize.setDisabled();
			}
			if (qty < maxRetainSize) {
				incRetainSize.setActive();
			} else {
				incRetainSize.setDisabled();
			}
		}
	}

	@Override
	protected void mouseClicked(int mX, int mY, int mouseButton) throws IOException {
		if (fluid != null && slots.stream().noneMatch(slot -> slot.intersectsWith(mX - guiLeft, mY - guiTop))) {
			fluid = null;
		} else {
			super.mouseClicked(mX, mY, mouseButton);
		}
	}

	@Override
	protected int getOverlaySheetX() {
		return 96;
	}

	@Override
	protected int getOverlaySheetY() {
		return 20;
	}

	@Override
	protected ResourceLocation getOverlayTexture() {
		return new ResourceLocation(ICON_PATH);
	}

	@Override
	protected void mouseClickMove(int mX, int mY, int lastClick, long timeSinceClick) {
		if (lastClick == 0 && (slots.stream().anyMatch(slot -> slot.intersectsWith(mX - guiLeft, mY - guiTop)) || (tab != null && tab.slot.intersectsWith(mX - guiLeft - tab.posX(), mY - guiTop - tab.getPosY())))) {
			try {
				mouseClicked(mX, mY, lastClick);
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			super.mouseClickMove(mX, mY, lastClick, timeSinceClick);
		}
	}

	@Override
	protected boolean hasClickedOutside(int x, int y, int left, int top) {
		if (fluid != null)
			return false;

		boolean yes = super.hasClickedOutside(x, y, left, top);
		if (yes && tab != null && tab.slot.intersectsWith(x - left - tab.posX(), y - top - tab.getPosY()))
			return false;

		return yes;
	}

	@Override
	protected void keyTyped(char characterTyped, int keyPressed) throws IOException {
		if (tab != null && tab.isFullyOpened() && tab.onKeyTyped(characterTyped, keyPressed))
			return;
		super.keyTyped(characterTyped, keyPressed);
	}


	@Override
	public void handleElementButtonClick(String buttonName, int mouseButton) {
		if (splitButton != null && splitButton.getName().equals(buttonName)) {
			int offset = mouseButton == 0 ? 1 : mouseButton == 1 ? -1 : 0;
			if (offset != 0) {
				int index = Ints.indexOf(CrafterItem.SPLITS[attachment.type], CrafterItem.SIZE[attachment.type] / crafter.getCrafters().size());
				if (index != -1) {
					index += offset;
					if (index < 0)
						index = CrafterItem.SPLITS[attachment.type].length - 1;
					else if (index >= CrafterItem.SPLITS[attachment.type].length)
						index = 0;

					int split = CrafterItem.SPLITS[attachment.type][index];
					crafter.split(split);

					playClickSound(0.8F);

					// Send to server
					PacketTileInfo packet = ((ConnectionBase) crafter).getNewPacket(ConnectionBase.NETWORK_ID.GUI);
					packet.addByte(1);
					packet.addInt(split);
					PacketHandler.sendToServer(packet);
				}
			}
			setButtons();
			return;
		}
		if (circuitButton != null && circuitButton.getName().equals(buttonName)) {
			playClickSound(0.8F);

			crafter.processParallel(!crafter.processParallel());
			
			PacketTileInfo packet = ((ConnectionBase) crafter).getNewPacket(ConnectionBase.NETWORK_ID.GUI);
			packet.addByte(7);
			PacketHandler.sendToServer(packet);

			setButtons();
			return;
		}
		for (int i = 0; i < flagButtons.length; i++) {
			ElementButton button = flagButtons[i];
			if (button != null && button.getName().equals(buttonName)) {
				if (attachment.filter.setFlag(i, !attachment.filter.getFlag(i))) {
					if (attachment.filter.getFlag(i)) {
						playClickSound(0.8F);
					} else {
						playClickSound(0.6F);
					}
				}
				setButtons();
				return;
			}
		}
		for (int i = 0; i < levelButtons.length; i++) {
			ElementButton button = levelButtons[i];
			if (button != null && button.getName().equals(buttonName)) {
				if (mouseButton == 0) {
					attachment.filter.incLevel(i);
					playClickSound(0.8F);
				} else if (mouseButton == 1) {
					attachment.filter.decLevel(i);
					playClickSound(0.6F);
				}
				setButtons();
				return;
			}
		}
		if (buttonName.equals("import")) {
			playClickSound(0.8F);

			// Send to server
			PacketTileInfo packet = ((ConnectionBase) crafter).getNewPacket(ConnectionBase.NETWORK_ID.GUI);
			packet.addByte(3);
			PacketHandler.sendToServer(packet);
			return;
		}

		int change = 1;
		float pitch = 0.7F;
		if (isShiftKeyDown()) {
			change = 16;
			pitch = 0.9F;
			if (mouseButton == 1) {
				change = 4;
				pitch = 0.8F;
			}
		}
		
		if (buttonName.equalsIgnoreCase("DecRetainSize")) {
			attachment.filter.decLevel(FilterLogic.levelRetainSize, change, false);
			pitch -= 0.1F;
		} else if (buttonName.equalsIgnoreCase("IncRetainSize")) {
			attachment.filter.incLevel(FilterLogic.levelRetainSize, change, false);
			pitch += 0.1F;
		}

		playClickSound(pitch);
	}

	@Override
	protected void drawGuiContainerForegroundLayer(int x, int y) {
		if (isAdvItemFilter) {
			int xQty = 146;
			int qty = attachment.filter.getLevel(FilterLogic.levelRetainSize);
			if (qty == 0) {
				xQty -= 9;
				fontRenderer.drawString(StringHelper.localize("info.thermaldynamics.filter.zeroRetainSize"), xQty, 42, 0x404040);
			} else {
				if (qty < 10) {
					xQty += 6;
				} else if (qty >= 100) {
					xQty -= 3;
				}
				fontRenderer.drawString("" + qty, xQty, 42, 0x404040);
			}
		}

		super.drawGuiContainerForegroundLayer(x, y);

		if (fluid != null) {
			GlStateManager.pushMatrix();
			GlStateManager.translate(0.0F, 0.0F, 64.0F);
			this.zLevel = 200.0F;
			itemRender.zLevel = 200.0F;

			StackHandler.render(this, x - guiLeft - 8, y - guiTop - 8, fluid, true);

			this.zLevel = 0.0F;
			itemRender.zLevel = 0.0F;
			GlStateManager.popMatrix();
		}
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTick, int x, int y) {
		GlStateManager.color(1, 1, 1, 1);
		bindTexture(texture);

		if (xSize > 256 || ySize > 256) {
			drawSizedTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize, 512, 512);
		} else {
			drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
		}

		mouseX = x - guiLeft;
		mouseY = y - guiTop;

		GlStateManager.pushMatrix();
		GlStateManager.translate(guiLeft, guiTop, 0.0F);

		// start own code
		if (!crafter.processParallel() && crafter.currentRecipe() >= 0) {
			int current = crafter.currentRecipe();

			int slots = CrafterItem.SIZE[attachment.type];
			int recipeSlots = slots / crafter.getRecipes().size();
			int start = slots * 9 + (crafter.getRecipes().size() - 1);

			int x0 = xSize / 2 - start;
			int posX = x0 + (current * recipeSlots) * 18 + current * 2;
			int posY = 20 + 20;

			int width = recipeSlots * 18;
			int height = 38 + 18;

			drawGradientRect(posX - 2, posY - 2, posX + width + 2, posY + height + 2, 0xFF00AA00, 0xFF00AA00);
		}

		// end own code
		drawElements(partialTick, false);
		drawTabs(partialTick, false);
		GlStateManager.popMatrix();
	}

	@Override
	protected void renderHoveredToolTip(int x, int y) {
		if (fluid == null)
			super.renderHoveredToolTip(x, y);
	}

	@Override
	public void addTooltips(List<String> tooltip) {
		if (fluid == null)
			super.addTooltips(tooltip);
	}

	@Override
	public FluidStack getFluid() {
		return fluid;
	}

	@Override
	public void setFluid(FluidStack fluid) {
		this.fluid = fluid;
	}

	public static class Slot<I> implements Supplier<I>, Consumer<I> {

		private final IAttachmentCrafter<I> crafter;
		private final boolean input;
		private final int recipe, index;

		private Slot(IAttachmentCrafter<I> crafter, int recipe, boolean input, int index) {
			this.crafter = crafter;
			this.recipe = recipe;
			this.input = input;
			this.index = index;
		}

		@Override
		public void accept(I stack) {
			if (recipe < crafter.getRecipes().size()) {
				Recipe<I> recipe = crafter.getRecipes().get(this.recipe);
				if (input) {
					if (index < recipe.inputs.size())
						recipe.inputs.set(index, stack);
				} else if (index < recipe.outputs.size())
					recipe.outputs.set(index, stack);
			}

			// Send to server
			PacketTileInfo packet = ((ConnectionBase) crafter).getNewPacket(ConnectionBase.NETWORK_ID.GUI);
			packet.addByte(0);
			packet.addInt(recipe);
			packet.addBool(input);
			packet.addInt(index);
			StackHandler.writePacket(packet, stack, crafter.getItemClass(), false);
			PacketHandler.sendToServer(packet);
		}

		@Override
		public I get() {
			if (recipe < crafter.getRecipes().size()) {
				Recipe<I> recipe = crafter.getRecipes().get(this.recipe);
				if (input) {
					if (index < recipe.inputs.size())
						return recipe.inputs.get(index);
				} else if (index < recipe.outputs.size())
					return recipe.outputs.get(index);
			}
			return null;
		}

		public IAttachmentCrafter<I> getCrafter() {
			return crafter;
		}

	}

}
