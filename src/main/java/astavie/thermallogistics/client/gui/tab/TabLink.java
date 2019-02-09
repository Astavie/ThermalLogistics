package astavie.thermallogistics.client.gui.tab;

import astavie.thermallogistics.attachment.ICrafter;
import astavie.thermallogistics.client.TLTextures;
import astavie.thermallogistics.util.RequesterReference;
import astavie.thermallogistics.util.StackHandler;
import cofh.core.gui.GuiContainerCore;
import cofh.core.gui.element.tab.TabBase;
import cofh.core.init.CoreTextures;
import cofh.core.network.PacketHandler;
import cofh.core.network.PacketTileInfo;
import cofh.core.util.helpers.MathHelper;
import cofh.core.util.helpers.StringHelper;
import cofh.thermaldynamics.duct.attachments.ConnectionBase;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;

import java.io.IOException;
import java.util.List;

public class TabLink extends TabBase {

	private static final int HEIGHT = 18;

	private final ICrafter<?> crafter;

	private int num;
	private int max;

	private int first = 0;

	public TabLink(GuiContainerCore gui, ICrafter<?> crafter) {
		super(gui);
		this.crafter = crafter;

		this.maxHeight = 96;
		this.backgroundColor = 0xc46d00;

		this.num = Math.min((maxHeight - 24) / HEIGHT, crafter.getLinked().size());
		this.max = crafter.getLinked().size() - num;
	}

	public TabLink(GuiContainerCore gui, int side, ICrafter<?> crafter) {
		super(gui, side);
		this.crafter = crafter;

		this.maxHeight = 96;
		this.backgroundColor = 0xc46d00;

		this.num = Math.min((maxHeight - 24) / HEIGHT, crafter.getLinked().size());
		this.max = crafter.getLinked().size() - num;
	}

	@Override
	public void update() {
		super.update();
		if (max != crafter.getLinked().size() - num || num > crafter.getLinked().size()) {
			num = Math.min((maxHeight - 24) / HEIGHT, crafter.getLinked().size());
			max = crafter.getLinked().size() - num;
			if (first > max)
				first = max;
		}
	}

	@Override
	protected void drawForeground() {
		GlStateManager.disableLighting();
		drawTabIcon(TLTextures.ICON_LINK);
		if (!isFullyOpened())
			return;

		int mouseX = gui.getMouseX() - posX();
		int mouseY = gui.getMouseY() - posY;

		if (first > 0)
			gui.drawIcon(CoreTextures.ICON_ARROW_UP, sideOffset() + maxWidth - 20, 16);
		else
			gui.drawIcon(CoreTextures.ICON_ARROW_UP_INACTIVE, sideOffset() + maxWidth - 20, 16);

		if (first < max)
			gui.drawIcon(CoreTextures.ICON_ARROW_DOWN, sideOffset() + maxWidth - 20, 76);
		else
			gui.drawIcon(CoreTextures.ICON_ARROW_DOWN_INACTIVE, sideOffset() + maxWidth - 20, 76);

		getFontRenderer().drawStringWithShadow(getTitle(), sideOffset() + 18, 6, headerColor);

		RenderHelper.disableStandardItemLighting();
		RenderHelper.enableGUIStandardItemLighting();

		for (int i = first; i < first + num; i++) {
			int x = sideOffset() + 2;
			int y = 21 + (i - first) * HEIGHT;

			RequesterReference<?> link = crafter.getLinked().get(i);

			gui.drawItemStack(link.getTileIcon(), x, y, false, null);
			gui.drawItemStack(link.getIcon(), x + 18, y, false, null);

			gui.drawIcon(TLTextures.ICON_ARROW_RIGHT, x + 36, y);

			int max = 2;
			int num = link.outputs.size() > max ? max - 1 : link.outputs.size();

			for (int j = 0; j < num; j++)
				StackHandler.render(gui, x + (j + 3) * 18, y, link.outputs.get(j), true);
			if (num < link.outputs.size())
				gui.getFontRenderer().drawString("...", x + (max + 2) * 18 + 1, y + 4, textColor);

			if (mouseX >= x + 90 && mouseX < x + 106 && mouseY >= y && mouseY < y + 16)
				gui.drawIcon(CoreTextures.ICON_CANCEL, x + 90, y);
			else
				gui.drawIcon(CoreTextures.ICON_CANCEL_INACTIVE, x + 90, y);
		}

		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
	}

	@Override
	public void addTooltip(List<String> list) {
		if (!isFullyOpened()) {
			list.add(getTitle());
		} else {
			int mouseX = gui.getMouseX() - posX();
			int mouseY = gui.getMouseY() - posY;

			for (int i = first; i < first + num; i++) {
				int x = sideOffset() + 2;
				int y = 21 + (i - first) * HEIGHT;

				RequesterReference<?> link = crafter.getLinked().get(i);

				if (mouseX >= x - 1 && mouseX < x + 17 && mouseY >= y - 1 && mouseY < y + 17)
					list.addAll(gui.getItemToolTip(link.getTileIcon()));
				if (mouseX >= x + 17 && mouseX < x + 35 && mouseY >= y - 1 && mouseY < y + 17)
					list.addAll(gui.getItemToolTip(link.getIcon()));

				int max = 2;
				int num = link.outputs.size() > max ? max - 1 : link.outputs.size();

				for (int j = 0; j < num; j++)
					if (mouseX >= x + (j + 3) * 18 - 1 && mouseX < x + (j + 3) * 18 + 17 && mouseY >= y - 1 && mouseY < y + 17)
						list.addAll(StackHandler.getTooltip(gui, link.outputs.get(i)));
			}
		}
	}

	private String getTitle() {
		return StringHelper.localize("info.logistics.tab.link");
	}

	@Override
	public boolean onMousePressed(int mouseX, int mouseY, int mouseButton) throws IOException {
		if (!isFullyOpened())
			return false;

		int shiftedMouseX = mouseX - this.posX();
		int shiftedMouseY = mouseY - this.posY;

		if (shiftedMouseX < 108) {
			int x = shiftedMouseX - sideOffset() - 2;
			int y = shiftedMouseY - 22;
			if (x >= 90 && y >= 0 && y < HEIGHT * num) {
				int n = first + (y / HEIGHT);
				crafter.getLinked().remove(n);

				num = Math.min((maxHeight - 24) / HEIGHT, crafter.getLinked().size());
				max = crafter.getLinked().size() - num;
				if (first > max)
					first = max;

				PacketTileInfo packet = crafter.getNewPacket(ConnectionBase.NETWORK_ID.GUI);
				packet.addByte(2);
				packet.addInt(n);
				PacketHandler.sendToServer(packet);
				GuiContainerCore.playClickSound(1.0F);
				return true;
			}
			return super.onMousePressed(mouseX, mouseY, mouseButton);
		}
		if (shiftedMouseY < 52)
			first = MathHelper.clamp(first - 1, 0, max);
		else
			first = MathHelper.clamp(first + 1, 0, max);

		return true;
	}

	@Override
	public boolean onMouseWheel(int mouseX, int mouseY, int movement) {
		if (!isFullyOpened())
			return false;
		if (movement > 0) {
			first = MathHelper.clamp(first - 1, 0, max);
			return true;
		} else if (movement < 0) {
			first = MathHelper.clamp(first + 1, 0, max);
			return true;
		}
		return false;
	}

}
