package astavie.thermallogistics.client.gui.tab;

import astavie.thermallogistics.util.StackHandler;
import cofh.core.gui.GuiContainerCore;
import cofh.core.gui.element.tab.TabBase;
import cofh.core.init.CoreTextures;
import cofh.core.network.PacketHandler;
import cofh.core.network.PacketTileInfo;
import cofh.core.util.helpers.MathHelper;
import cofh.core.util.helpers.StringHelper;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.tileentity.TileEntity;

import java.io.IOException;
import java.util.List;

public class TabRequest extends TabBase {

	private static final int HEIGHT = 18;

	private final List<?> list;
	private final TileEntity tile;

	private int num;
	private int max;

	private int first = 0;

	public TabRequest(GuiContainerCore gui, List<?> list, TileEntity tile) {
		this(gui, RIGHT, list, tile);
	}

	public TabRequest(GuiContainerCore gui, int side, List<?> list, TileEntity tile) {
		super(gui, side);
		this.list = list;
		this.tile = tile;

		this.maxWidth = 88;
		this.maxHeight = 96;
		this.backgroundColor = 0xFF008080;
		this.textColor = 0xFFFFFFFF;

		this.num = Math.min((maxHeight - 24) / HEIGHT, list.size());
		this.max = list.size() - num;
	}

	@Override
	public void update() {
		super.update();
		if (max != list.size() - num || num > list.size()) {
			num = Math.min((maxHeight - 24) / HEIGHT, list.size());
			max = list.size() - num;
			if (first > max)
				first = max;
		}
	}

	@Override
	protected void drawForeground() {
		GlStateManager.disableLighting();
		drawTabIcon(list.isEmpty() ? CoreTextures.ICON_RS_TORCH_OFF : CoreTextures.ICON_RS_TORCH_ON);
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

			StackHandler.render(gui, x, y, list.get(i), true);

			if (mouseX >= x + 53 && mouseX < x + 69 && mouseY >= y && mouseY < y + 16)
				gui.drawIcon(CoreTextures.ICON_CANCEL, x + 53, y);
			else
				gui.drawIcon(CoreTextures.ICON_CANCEL_INACTIVE, x + 53, y);
		}

		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
	}


	private String getTitle() {
		return StringHelper.localize("info.logistics.tab.request");
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

				if (mouseX >= x - 1 && mouseX < x + 17 && mouseY >= y - 1 && mouseY < y + 17)
					list.addAll(StackHandler.getTooltip(gui, this.list.get(i)));
			}
		}
	}

	@Override
	public boolean onMousePressed(int mouseX, int mouseY, int mouseButton) throws IOException {
		if (!isFullyOpened())
			return false;

		int shiftedMouseX = mouseX - this.posX();
		int shiftedMouseY = mouseY - this.posY;

		if (shiftedMouseX < maxWidth - 16) {
			int x = shiftedMouseX - sideOffset() - 2;
			int y = shiftedMouseY - 22;
			if (x >= 53 && x < 69 && y >= 0 && y < HEIGHT * num) {
				int n = first + (y / HEIGHT);
				list.remove(n);

				PacketTileInfo packet = PacketTileInfo.newPacket(tile);
				packet.addByte(1);
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
