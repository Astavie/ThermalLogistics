package astavie.thermallogistics.client.gui.element;

import astavie.thermallogistics.ThermalLogistics;
import astavie.thermallogistics.attachment.Recipe;
import astavie.thermallogistics.client.gui.GuiCrafter;
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

	public ElementButtonLinks(GuiCrafter gui, int posX, int posY, int index) {
		super(gui, posX, posY, 16, 16, 116, 0, 116, 16, 116, 32, GuiCrafter.ICON_PATH);
		this.crafter = gui;
		this.index = index;

		ItemStack stack = gui.draggedStack.isEmpty() ? Minecraft.getMinecraft().player.inventory.getItemStack() : gui.draggedStack;
		if (stack.getItem() == ThermalLogistics.Items.manager) {
			setEnabled(true);
		} else {
			Recipe<?> recipe = (Recipe<?>) crafter.crafter.getCrafters().get(index);
			setEnabled(!recipe.linked.isEmpty());
		}
	}

	@Override
	public void drawForeground(int mouseX, int mouseY) {
		if (isEnabled()) {

			ItemStack stack = gui.draggedStack.isEmpty() ? Minecraft.getMinecraft().player.inventory.getItemStack() : gui.draggedStack;
			if (stack.getItem() == ThermalLogistics.Items.manager)
				return;

			if (intersectsWith(mouseX, mouseY)) {
				// TODO: Show linked
			}
		}
	}

	@Override
	public void onClick() {
		ItemStack stack = gui.draggedStack.isEmpty() ? Minecraft.getMinecraft().player.inventory.getItemStack() : gui.draggedStack;
		if (stack.getItem() == ThermalLogistics.Items.manager) {
			// Link!
			PacketTileInfo packet = ((ConnectionBase) crafter.crafter).getNewPacket(ConnectionBase.NETWORK_ID.GUI);
			packet.addByte(5);
			packet.addInt(index);
			PacketHandler.sendToServer(packet);
		} else {
			// TODO: Show linked
		}
	}

	@Override
	public void addTooltip(List<String> list) {
		if (!isEnabled()) {
			list.add(StringHelper.localize("info.logistics.unlinked.0"));
			list.add(StringHelper.localize("info.logistics.unlinked.1"));
		}
	}

}
