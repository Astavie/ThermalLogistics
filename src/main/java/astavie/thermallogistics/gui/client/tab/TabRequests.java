package astavie.thermallogistics.gui.client.tab;

import astavie.thermallogistics.network.PacketCancelProcess;
import astavie.thermallogistics.proxy.ProxyClient;
import astavie.thermallogistics.util.IRequester;
import astavie.thermallogistics.util.delegate.DelegateClientItem;
import astavie.thermallogistics.util.delegate.IDelegateClient;
import astavie.thermallogistics.util.request.IRequest;
import astavie.thermallogistics.util.request.Requests;
import cofh.core.gui.GuiContainerCore;
import cofh.core.gui.element.tab.TabBase;
import cofh.core.init.CoreTextures;
import cofh.core.network.PacketHandler;
import cofh.core.util.helpers.MathHelper;
import cofh.core.util.helpers.StringHelper;
import cofh.thermaldynamics.duct.tiles.DuctUnit;
import cofh.thermaldynamics.duct.tiles.TileGrid;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;

public class TabRequests<T extends DuctUnit<T, ?, ?>, I> extends TabBase {

	private static final int HEIGHT = 18;

	private final IDelegateClient<I, ?> delegate;
	private final List<Requests<T, I>> requests;
	private final Runnable refresh;

	private final int num;

	private int sum;

	private int first = 0;
	private int ticks = 0;

	public TabRequests(GuiContainerCore gui, IDelegateClient<I, ?> delegate, List<Requests<T, I>> requests, Runnable refresh) {
		super(gui);
		this.maxHeight = 96;
		this.num = (maxHeight - 24) / HEIGHT;

		this.delegate = delegate;
		this.requests = requests;
		this.refresh = refresh;

		refresh.run();
	}

	@Override
	public void addTooltip(List<String> list) {
		if (!isFullyOpened())
			list.add(getTitle());
	}

	private String getTitle() {
		return StringHelper.localize("info.logistics.tab.requests");
	}

	@Override
	public void update() {
		super.update();
		ticks++;
		if (ticks >= 10) {
			ticks = 0;
			refresh.run();
		}
		refresh();
	}

	@Override
	protected void drawForeground() {
		GlStateManager.disableLighting();
		drawTabIcon(requests.isEmpty() ? ProxyClient.ICON_REQUESTS_OFF : ProxyClient.ICON_REQUESTS_ON);
		if (!isFullyOpened())
			return;

		int mouseX = gui.getMouseX() - posX();
		int mouseY = gui.getMouseY() - posY;

		if (first > 0)
			gui.drawIcon(CoreTextures.ICON_ARROW_UP, sideOffset() + maxWidth - 20, 16);
		else
			gui.drawIcon(CoreTextures.ICON_ARROW_UP_INACTIVE, sideOffset() + maxWidth - 20, 16);

		if (first < sum - num)
			gui.drawIcon(CoreTextures.ICON_ARROW_DOWN, sideOffset() + maxWidth - 20, 76);
		else
			gui.drawIcon(CoreTextures.ICON_ARROW_DOWN_INACTIVE, sideOffset() + maxWidth - 20, 76);

		getFontRenderer().drawString(getTitle(), sideOffset() + 18, 6, 0xFF373737);

		RenderHelper.disableStandardItemLighting();
		RenderHelper.enableGUIStandardItemLighting();

		ItemStack select = null;
		I sel = null;

		for (int i = first; i < Math.min(first + num, sum); i++) {
			int x = sideOffset() + 2;
			int y = 21 + (i - first) * HEIGHT;

			IRequest<T, I> request = null;
			I stack = null;

			int j = 0;
			boolean f = true;
			a:
			for (Requests<T, I> r : requests) {
				f = true;
				for (IRequest<T, I> q : r.getRequests()) {
					j += q.getStacks().size();
					if (j > i) {
						j--;
						request = q;
						stack = q.getStacks().get(j - i);
						break a;
					}
					f = false;
				}
			}

			if (j == i) {
				if (f) {
					// Render line separator
					if (i > first)
						Gui.drawRect(x - 1, y - 1, sideOffset() + maxWidth - 20, y, 0xFF373737);

					// Render cancel button
					if (mouseX >= x + 90 && mouseX < x + 106 && mouseY >= y && mouseY < y + 16)
						gui.drawIcon(CoreTextures.ICON_CANCEL, x + 90, y);
					else
						gui.drawIcon(CoreTextures.ICON_CANCEL_INACTIVE, x + 90, y);
				}

				// Render block
				IRequester<T, I> start = request.getStart();
				if (start != null) {
					BlockPos pos = start.getBase();

					TileEntity tile = Minecraft.getMinecraft().player.world.getTileEntity(pos);
					if (tile instanceof TileGrid)
						pos = pos.offset(EnumFacing.byIndex(start.getSide()));

					IBlockState state = Minecraft.getMinecraft().player.world.getBlockState(pos);
					ItemStack item = state.getBlock().getItem(Minecraft.getMinecraft().player.world, pos, state);

					gui.drawItemStack(item, x, y, false, null);
					if (mouseX >= x - 1 && mouseX < x + 17 && mouseY >= y - 1 && mouseY < y + 17)
						select = item;

					// Render attachment
					if (tile instanceof TileGrid) {
						ItemStack attachment = ((TileGrid) tile).getAttachment(start.getSide()).getPickBlock();
						gui.drawItemStack(attachment, x + 18, y, false, null);
						if (mouseX >= x + 17 && mouseX < x + 35 && mouseY >= y - 1 && mouseY < y + 17)
							select = attachment;
					}
				}
			}

			// Render stack
			delegate.drawStack(gui, x + 54, y, stack);
			if (mouseX >= x + 53 && mouseX < x + 71 && mouseY >= y - 1 && mouseY < y + 17)
				sel = stack;
		}

		if (select != null)
			DelegateClientItem.INSTANCE.drawHover(gui, mouseX, mouseY, select);
		else if (sel != null)
			delegate.drawHover(gui, mouseX, mouseY, sel);

		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
	}

	private void refresh() {
		sum = 0;
		requests.sort(Comparator.reverseOrder());
		for (Requests<T, I> list : requests) {
			list.getRequests().sort(Comparator.reverseOrder());
			for (IRequest<T, I> request : list.getRequests())
				sum += request.getStacks().size();
		}

		first = Math.min(first, sum - num);
		if (first < 0)
			first = 0;
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

				int i = 0;
				Requests<T, I> requests = null;
				for (Requests<T, I> list : this.requests) {
					if (i == n) {
						requests = list;
						break;
					} else for (IRequest<T, I> request : list.getRequests())
						i += request.getStacks().size();
				}

				if (requests != null) {
					PacketHandler.sendToServer(new PacketCancelProcess(requests));
					GuiContainerCore.playClickSound(1.0F);
					return true;
				}
			}
			return super.onMousePressed(mouseX, mouseY, mouseButton);
		}

		if (shiftedMouseY < 52)
			first = MathHelper.clamp(first - 1, 0, sum - num);
		else
			first = MathHelper.clamp(first + 1, 0, sum - num);
		if (first < 0)
			first = 0;

		return true;
	}

	@Override
	public boolean onMouseWheel(int mouseX, int mouseY, int movement) {
		if (!isFullyOpened())
			return false;
		if (movement > 0) {
			first = MathHelper.clamp(first - 1, 0, sum - num);
			if (first < 0)
				first = 0;
			return true;
		} else if (movement < 0) {
			first = MathHelper.clamp(first + 1, 0, sum - num);
			if (first < 0)
				first = 0;
			return true;
		}
		return false;
	}

}
