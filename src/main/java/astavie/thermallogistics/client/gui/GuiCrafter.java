package astavie.thermallogistics.client.gui;

import astavie.thermallogistics.attachment.CrafterItem;
import astavie.thermallogistics.attachment.ICrafter;
import astavie.thermallogistics.client.gui.element.ElementSlotItem;
import cofh.core.gui.GuiContainerCore;
import cofh.core.gui.element.ElementButton;
import cofh.core.gui.element.tab.TabInfo;
import cofh.core.gui.element.tab.TabRedstoneControl;
import cofh.core.network.PacketHandler;
import cofh.core.network.PacketTileInfo;
import cofh.core.util.helpers.StringHelper;
import cofh.thermaldynamics.duct.attachments.ConnectionBase;
import cofh.thermaldynamics.duct.attachments.filter.FilterLogic;
import cofh.thermaldynamics.gui.container.ContainerAttachmentBase;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class GuiCrafter extends GuiContainerCore {

	private static final String TEX_PATH = "thermaldynamics:textures/gui/connection.png";
	private static final ResourceLocation TEXTURE = new ResourceLocation(TEX_PATH);
	private static final int[][] levelButtonPos = {{-1, -1}, {0, 204}, {80, 204}};
	private static final int[][] flagButtonsPos = {{176, 0}, {176, 60}, {216, 0}, {216, 60}, {176, 120}, {216, 120}, {176, 180}, {216, 180},};
	private final CrafterItem crafter;
	private ElementButton[] flagButtons = new ElementButton[0];
	private ElementButton[] levelButtons = new ElementButton[FilterLogic.defaultLevels.length];
	private int buttonSize;

	public GuiCrafter(InventoryPlayer inventoryPlayer, CrafterItem crafter) {
		super(new ContainerAttachmentBase(inventoryPlayer, crafter), TEXTURE);
		this.crafter = crafter;
		this.crafter.getFilter();
		this.name = crafter.getName();
		this.ySize = 204;

		String info = crafter.getInfo();
		if (info != null) {
			generateInfo(info);
		}
	}

	@Override
	public void initGui() {
		super.initGui();

		if (!myInfo.isEmpty()) {
			myInfo += "\n\n" + StringHelper.localize("tab.thermaldynamics.conChange");
			addTab(new TabInfo(this, myInfo));
		}

		if (crafter.canAlterRS())
			addTab(new TabRedstoneControl(this, crafter));

		int[] flagNums = new int[crafter.filter.validFlags().length - 1];
		System.arraycopy(crafter.filter.validFlags(), 1, flagNums, 0, flagNums.length);

		flagButtons = new ElementButton[crafter.filter.numFlags()];

		int[] levelNums = crafter.filter.getValidLevels();
		levelButtons = new ElementButton[FilterLogic.defaultLevels.length];

		int buttonNo = flagNums.length + levelNums.length;
		// if (crafter.type > 0)
		//	buttonNo++;

		if (buttonNo != 0) {
			buttonSize = 20;
			int button_offset = buttonSize + 6;
			int x0 = xSize / 2 - buttonNo * (button_offset / 2) + 3;
			int y0 = 38 + 2 * 18 + 8;

			for (int i = 0; i < flagNums.length; i++) {
				int j = flagNums[i];
				flagButtons[j] = new ElementButton(this, x0 + button_offset * i, y0, crafter.filter.flagType(j), flagButtonsPos[j][0], flagButtonsPos[j][1], flagButtonsPos[j][0], flagButtonsPos[j][1] + buttonSize, flagButtonsPos[j][0], flagButtonsPos[j][1] + buttonSize * 2, buttonSize, buttonSize, TEX_PATH);
				addElement(flagButtons[j]);
			}
			for (int i = 0; i < levelNums.length; i++) {
				int j = levelNums[i];
				levelButtons[j] = new ElementButton(this, x0 + button_offset * (i + flagNums.length), y0, FilterLogic.levelNames[j], levelButtonPos[j][0], levelButtonPos[j][1], levelButtonPos[j][0], levelButtonPos[j][1] + buttonSize, buttonSize, buttonSize, TEX_PATH);
				addElement(levelButtons[j]);
			}
		}

		if (crafter.type > 0) {
			int slots = crafter.type + 1;
			int recipeSlots = slots / crafter.recipes.size();

			int start = slots * 9 + (crafter.recipes.size() - 1);
			int x0 = xSize / 2 - start;
			int y0 = 20;

			for (int i = 0; i < crafter.recipes.size(); i++) {
				for (int x = 0; x < recipeSlots; x++) {
					Slot up = new Slot(crafter, i, true, x * 2);
					addElement(new ElementSlotItem(this, x0 + x * 18 + i * 2, y0, up, up));

					Slot down = new Slot(crafter, i, true, x * 2 + 1);
					addElement(new ElementSlotItem(this, x0 + x * 18 + i * 2, y0 + 18, down, down));

					Slot output = new Slot(crafter, i, false, x);
					addElement(new ElementSlotItem(this, x0 + x * 18 + i * 2, y0 + 38, output, output));
				}
			}
		} else {
			int x0 = xSize / 2;
			int y0 = 38;

			Slot left = new Slot(crafter, 0, true, 0);
			addElement(new ElementSlotItem(this, x0 - 18, y0, left, left));

			Slot right = new Slot(crafter, 0, true, 1);
			addElement(new ElementSlotItem(this, x0, y0, right, right));

			Slot output = new Slot(crafter, 0, false, 0);
			addElement(new ElementSlotItem(this, x0 - 9, y0 + 20, output, output));
		}
	}

	private void setButtons() {
		for (int i = 0; i < flagButtons.length; i++) {
			if (flagButtons[i] != null) {
				boolean b = crafter.filter.getFlag(i);
				int x = flagButtonsPos[i][0] + (b ? buttonSize : 0);
				flagButtons[i].setSheetX(x);
				flagButtons[i].setHoverX(x);
				flagButtons[i].setToolTip("info.thermaldynamics.filter." + flagButtons[i].getName() + (b ? ".on" : ".off"));
			}
		}
		for (int i = 0; i < levelButtons.length; i++) {
			if (levelButtons[i] != null) {
				int level = crafter.filter.getLevel(i);
				int x = levelButtonPos[i][0] + level * buttonSize;
				levelButtons[i].setSheetX(x);
				levelButtons[i].setHoverX(x);
				levelButtons[i].setToolTip("info.thermaldynamics.filter." + levelButtons[i].getName() + "." + level);
			}
		}
	}

	@Override
	protected void updateElementInformation() {
		super.updateElementInformation();
		setButtons();
	}

	@Override
	public void handleElementButtonClick(String buttonName, int mouseButton) {
		for (int i = 0; i < flagButtons.length; i++) {
			ElementButton button = flagButtons[i];
			if (button != null && button.getName().equals(buttonName)) {
				if (crafter.filter.setFlag(i, !crafter.filter.getFlag(i))) {
					if (crafter.filter.getFlag(i)) {
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
					crafter.filter.incLevel(i);
					playClickSound(0.8F);
				} else if (mouseButton == 1) {
					crafter.filter.decLevel(i);
					playClickSound(0.6F);
				}
				setButtons();
				return;
			}
		}
	}

	public static class Slot implements Supplier<ItemStack>, Consumer<ItemStack> {

		private final CrafterItem crafter;
		private final boolean input;
		private final int recipe, index;

		public Slot(CrafterItem crafter, int recipe, boolean input, int index) {
			this.crafter = crafter;
			this.recipe = recipe;
			this.input = input;
			this.index = index;
		}

		@Override
		public void accept(ItemStack itemStack) {
			if (recipe < crafter.recipes.size()) {
				ICrafter.Recipe<ItemStack> recipe = crafter.recipes.get(this.recipe);
				if (input) {
					if (index < recipe.inputs.size())
						recipe.inputs.set(index, itemStack);
				} else if (index < recipe.outputs.size())
					recipe.outputs.set(index, itemStack);
			}

			// Send to server
			PacketTileInfo packet = crafter.getNewPacket(ConnectionBase.NETWORK_ID.GUI);
			packet.addInt(recipe);
			packet.addBool(input);
			packet.addInt(index);
			packet.addItemStack(itemStack);
			PacketHandler.sendToServer(packet);
		}

		@Override
		public ItemStack get() {
			if (recipe < crafter.recipes.size()) {
				ICrafter.Recipe<ItemStack> recipe = crafter.recipes.get(this.recipe);
				if (input) {
					if (index < recipe.inputs.size())
						return recipe.inputs.get(index);
				} else if (index < recipe.outputs.size())
					return recipe.outputs.get(index);
			}
			return ItemStack.EMPTY;
		}

	}

}
