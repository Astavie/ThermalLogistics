package astavie.thermallogistics.client.gui.element;

import astavie.thermallogistics.client.gui.GuiCrafter;
import cofh.core.gui.element.ElementButtonBase;
import cofh.core.init.CoreTextures;
import cofh.core.network.PacketHandler;
import cofh.core.network.PacketTileInfo;
import cofh.thermaldynamics.duct.attachments.ConnectionBase;

public class ElementButtonCancelLink extends ElementButtonBase {

	private GuiCrafter crafter;
	private int index1, index2;

	public ElementButtonCancelLink(GuiCrafter gui, int posX, int posY, int index1, int index2) {
		super(gui, posX, posY, 18, 18);
		this.crafter = gui;
		this.index1 = index1;
		this.index2 = index2;
	}

	@Override
	public void drawBackground(int mouseX, int mouseY, float gameTicks) {
		if (intersectsWith(mouseX, mouseY)) {
			gui.drawIcon(CoreTextures.ICON_CANCEL, posX + 1, posY + 1);
		} else {
			gui.drawIcon(CoreTextures.ICON_CANCEL_INACTIVE, posX + 1, posY + 1);
		}
	}

	@Override
	public void drawForeground(int mouseX, int mouseY) {
	}

	@Override
	public void onClick() {
		// Cancel!
		PacketTileInfo packet = ((ConnectionBase) crafter.crafter).getNewPacket(ConnectionBase.NETWORK_ID.GUI);
		packet.addByte(2);
		packet.addInt(index1);
		packet.addInt(index2);
		PacketHandler.sendToServer(packet);
	}

}
