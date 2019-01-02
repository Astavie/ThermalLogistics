package astavie.thermallogistics.gui.client.tab;

import astavie.thermallogistics.attachment.Crafter;
import astavie.thermallogistics.gui.client.GuiCrafter;
import astavie.thermallogistics.proxy.ProxyClient;
import astavie.thermallogistics.util.delegate.DelegateClientItem;
import astavie.thermallogistics.util.reference.CrafterReference;
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
	private final List<CrafterReference> linked;
	private int num;
	private int max;

	private int first = 0;

	public TabLink(GuiCrafter gui) {
		super(gui);
		this.maxHeight = 96;
		this.backgroundColor = 0xc46d00;

		//noinspection unchecked
		this.crafter = gui.crafter;
		this.linked = new ArrayList<>(crafter.linked);
		this.linked.remove(new CrafterReference<>(crafter));
		this.num = Math.min((maxHeight - 24) / HEIGHT, linked.size());
		this.max = linked.size() - num;
	}

	@Override
	protected void drawForeground() {
		GlStateManager.disableLighting();
		drawTabIcon(ProxyClient.ICON_LINK);
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

		ItemStack blockSelect = null;
		Runnable stackSelect = null;

		for (int i = first; i < first + num; i++) {
			int x = sideOffset() + 2;
			int y = 21 + (i - first) * HEIGHT;

			Crafter<?, ?, ?> link = linked.get(i).getCrafter();
			BlockPos pos = link.baseTile.getPos().offset(EnumFacing.VALUES[link.side]);
			IBlockState state = link.baseTile.world().getBlockState(pos);

			ItemStack item = state.getBlock().getItem(link.baseTile.world(), pos, state);

			if (mouseX >= x - 1 && mouseX < x + 17 && mouseY >= y - 1 && mouseY < y + 17)
				blockSelect = item;
			gui.drawItemStack(item, x, y, false, null);

			Runnable run = drawSummary(link, x + 18, y);
			if (run != null)
				stackSelect = run;

			if (mouseX >= x + 90 && mouseX < x + 106 && mouseY >= y && mouseY < y + 16)
				gui.drawIcon(CoreTextures.ICON_CANCEL, x + 90, y);
			else
				gui.drawIcon(CoreTextures.ICON_CANCEL_INACTIVE, x + 90, y);
		}

		if (blockSelect != null)
			DelegateClientItem.INSTANCE.drawHover(gui, mouseX, mouseY, blockSelect);
		else if (stackSelect != null)
			stackSelect.run();

		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
	}

	private <I, C extends Crafter<?, ?, I>> Runnable drawSummary(C crafter, int x, int y) {
		int mouseX = gui.getMouseX() - posX();
		int mouseY = gui.getMouseY() - posY;

		I input = null, output = null;
		boolean bI = false, bO = false;

		boolean b = false;
		for (I stack : crafter.getInputs()) {
			if (!crafter.getDelegate().isNull(stack)) {
				if (input == null)
					input = stack;
				if (b) {
					bI = true;
					break;
				} else b = true;
			}
		}

		b = false;
		for (I stack : crafter.getOutputs()) {
			if (!crafter.getDelegate().isNull(stack)) {
				if (output == null)
					output = stack;
				if (b) {
					bI = true;
					break;
				} else b = true;
			}
		}
		if (input != null)
			crafter.getClientDelegate().drawStack(gui, x, y, input);
		if (bI)
			gui.getFontRenderer().drawString("...", x + 19, y + 4, textColor);

		gui.drawIcon(ProxyClient.ICON_ARROW_RIGHT, x + 26, y);

		if (output != null)
			crafter.getClientDelegate().drawStack(gui, x + 44, y, output);
		if (bO)
			gui.getFontRenderer().drawString("...", x + 63, y + 4, textColor);

		if (!crafter.getDelegate().isNull(input) && mouseX >= x - 1 && mouseX < x + 17 && mouseY >= y - 1 && mouseY < y + 17) {
			final I i = input;
			return () -> crafter.getClientDelegate().drawHover(gui, mouseX, mouseY, i);
		}
		if (!crafter.getDelegate().isNull(output) && mouseX >= x + 43 && mouseX < x + 61 && mouseY >= y - 1 && mouseY < y + 17) {
			final I i = output;
			return () -> crafter.getClientDelegate().drawHover(gui, mouseX, mouseY, i);
		}
		return null;
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
