package astavie.thermallogistics.client.gui.element;

import astavie.thermallogistics.ThermalLogistics;
import astavie.thermallogistics.attachment.Recipe;
import astavie.thermallogistics.client.gui.GuiCrafter;
import astavie.thermallogistics.client.gui.GuiOverlay;
import astavie.thermallogistics.util.RequesterReference;
import astavie.thermallogistics.util.type.ItemType;
import cofh.core.gui.element.ElementButton;
import cofh.core.network.PacketHandler;
import cofh.core.network.PacketTileInfo;
import cofh.core.util.helpers.StringHelper;
import cofh.thermaldynamics.duct.attachments.ConnectionBase;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;

import java.util.List;

public class ElementButtonLinks extends ElementButton {

	private final GuiCrafter crafter;
	private final int index;

	private final Overlay overlay;

	public ElementButtonLinks(GuiCrafter gui, int posX, int posY, int index) {
		super(gui, posX, posY, 16, 16, 116, 0, 116, 16, 116, 32, GuiCrafter.ICON_PATH);
		this.crafter = gui;
		this.index = index;

		ItemStack stack = gui.draggedStack.isEmpty() ? Minecraft.getMinecraft().player.inventory.getItemStack() : gui.draggedStack;
		if (stack.getItem() == ThermalLogistics.Items.manager) {
			setEnabled(true);
			overlay = null;
		} else {
			Recipe<?> recipe = (Recipe<?>) crafter.crafter.getCrafters().get(index);
			setEnabled(!recipe.linked.isEmpty());

			if (isEnabled()) {
				overlay = new Overlay(gui, index);
			} else {
				overlay = null;
			}
		}
	}

	@Override
	public void drawBackground(int mouseX, int mouseY, float gameTicks) {
		super.drawBackground(mouseX, mouseY, gameTicks);

		if (isEnabled() && crafter.overlay == null) {

			ItemStack stack = gui.draggedStack.isEmpty() ? Minecraft.getMinecraft().player.inventory.getItemStack() : gui.draggedStack;
			if (stack.getItem() == ThermalLogistics.Items.manager)
				return;

			if (intersectsWith(mouseX, mouseY)) {
				overlay.setPos(mouseX + 4, mouseY + 4);
				overlay.drawBackground(mouseX, mouseY, gameTicks);
			}
		}
	}

	@Override
	public void drawForeground(int mouseX, int mouseY) {
		if (isEnabled() && crafter.overlay == null) {

			ItemStack stack = gui.draggedStack.isEmpty() ? Minecraft.getMinecraft().player.inventory.getItemStack() : gui.draggedStack;
			if (stack.getItem() == ThermalLogistics.Items.manager)
				return;

			if (intersectsWith(mouseX, mouseY)) {
				overlay.drawForeground(mouseX, mouseY);
			}
		}
	}

	@Override
	public boolean onMousePressed(int mouseX, int mouseY, int mouseButton) {
		playSound(mouseButton);
		switch (mouseButton) {
			case 0:
				ItemStack stack = gui.draggedStack.isEmpty() ? Minecraft.getMinecraft().player.inventory.getItemStack() : gui.draggedStack;
				if (stack.getItem() == ThermalLogistics.Items.manager) {
					// Link!
					PacketTileInfo packet = ((ConnectionBase) crafter.crafter).getNewPacket(ConnectionBase.NETWORK_ID.GUI);
					packet.addByte(5);
					packet.addInt(index);
					PacketHandler.sendToServer(packet);
				} else {
					overlay.setPos(mouseX + 4, mouseY + 4);
					crafter.setOverlay(overlay);
				}
				break;
			case 1:
				onRightClick();
				break;
			case 2:
				onMiddleClick();
				break;
		}
		return true;
	}

	@Override
	public void addTooltip(List<String> list) {
		if (!isEnabled()) {
			list.add(StringHelper.localize("info.logistics.unlinked.0"));
			list.add(StringHelper.localize("info.logistics.unlinked.1"));
		}
	}

	public static class Overlay extends GuiOverlay.Overlay {

		private GuiCrafter crafter;
		private int index;

		public Overlay(GuiCrafter gui, int index) {
			super(gui, 0, 0, 0, 0);
			this.crafter = gui;
			this.index = index;

			update();
		}

		@Override
		public void update() {
			Recipe<?> recipe = (Recipe<?>) crafter.crafter.getCrafters().get(index);

			if (recipe.linked.isEmpty()) {
				crafter.setOverlay(null);
				return;
			}

			setDimensions(3 * 18, recipe.linked.size() * 18);

			elements.clear();

			for (int i = 0; i < recipe.linked.size(); i++) {
				RequesterReference<?> reference = recipe.linked.get(i);
				elements.add(new ElementButtonCancelLink(crafter, 0, i * 18, index, i));
				elements.add(new ElementStack(crafter, 18, i * 18, new ItemType(reference.getTileIcon()), true));
				elements.add(new ElementStack(crafter, 36, i * 18, new ItemType(reference.getIcon()), true));
			}
		}

	}

}
