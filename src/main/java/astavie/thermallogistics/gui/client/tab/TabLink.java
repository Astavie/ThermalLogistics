package astavie.thermallogistics.gui.client.tab;

import astavie.thermallogistics.attachment.Crafter;
import astavie.thermallogistics.gui.client.GuiCrafter;
import astavie.thermallogistics.proxy.ProxyClient;
import cofh.core.gui.GuiContainerCore;
import cofh.core.gui.element.tab.TabBase;
import cofh.core.init.CoreTextures;
import cofh.core.network.PacketHandler;
import cofh.core.network.PacketTileInfo;
import cofh.core.util.helpers.MathHelper;
import cofh.core.util.helpers.StringHelper;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TabLink extends TabBase {

	private static final int HEIGHT = 18;

	private final Crafter<?, ?, ?> crafter;
	private final List<Crafter> linked;
	private int num;
	private int max;

	private int first = 0;

	public TabLink(GuiCrafter gui) {
		super(gui);
		this.maxHeight = 92;
		this.backgroundColor = 0xc46d00;

		//noinspection unchecked
		this.crafter = gui.crafter;
		this.linked = new ArrayList<>(crafter.linked);
		this.linked.remove(crafter);
		this.num = Math.min((maxHeight - 24) / HEIGHT, linked.size());
		this.max = linked.size() - num;
	}

	@Override
	protected void drawForeground() {
		GlStateManager.disableLighting();
		drawTabIcon(ProxyClient.ICON_LINK);
		if (!isFullyOpened())
			return;

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

			Crafter link = linked.get(i);
			BlockPos pos = link.baseTile.getPos().offset(EnumFacing.VALUES[link.side]);
			IBlockState state = link.baseTile.world().getBlockState(pos);
			//noinspection deprecation
			ItemStack item = state.getBlock().getItem(link.baseTile.world(), pos, state);

			gui.drawItemStack(item, x, y, false, null);
			ProxyClient.renderSummary(link, gui, x + 18, y, textColor);
			gui.drawIcon(CoreTextures.ICON_CANCEL, x + 90, y);
		}
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
	}

	@Override
	public void addTooltip(List<String> list) {
		if (!isFullyOpened())
			list.add(getTitle());
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
				linked.remove(n);
				num = Math.min((maxHeight - 24) / HEIGHT, linked.size());
				max = linked.size() - num;
				if (first > max)
					first = max;
				PacketTileInfo packet = crafter.getNewPacket();
				packet.addByte(3);
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
