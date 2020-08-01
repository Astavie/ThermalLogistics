package astavie.thermallogistics.client.gui.tab;

import astavie.thermallogistics.attachment.IAttachmentCrafter;
import astavie.thermallogistics.attachment.Recipe;
import astavie.thermallogistics.util.RequesterReference;
import astavie.thermallogistics.util.StackHandler;
import astavie.thermallogistics.util.collection.StackList;
import astavie.thermallogistics.util.type.Type;
import cofh.core.gui.GuiContainerCore;
import cofh.core.gui.element.tab.TabBase;
import cofh.core.init.CoreTextures;
import cofh.core.util.helpers.MathHelper;
import cofh.core.util.helpers.StringHelper;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TabStackList<I> extends TabBase {

	private static final int STACK_HEIGHT = 18;
	private static final int STACKS_PER_LINE = 4;

	private final IAttachmentCrafter<I> crafter;
	private final boolean input;

	private final Map<RequesterReference<I>, StackList<I>> map = new LinkedHashMap<>();
	private final StackList<I> network;

	private final int lines;

	private int currentLine = 0;
	private int totalLines;

	public TabStackList(GuiContainerCore gui, IAttachmentCrafter<I> crafter, boolean input) {
		super(gui, LEFT);
		this.crafter = crafter;
		this.input = input;

		network = crafter.getSupplier().get();

		maxHeight = 96;
		maxWidth += 2;
		lines = (maxHeight - 24) / STACK_HEIGHT;

		refresh();
	}

	@Override
	public void update() {
		super.update();

		if (isFullyOpened()) {
			refresh();
		}
	}

	private void refresh() {
		// Refresh items
		network.clear();
		map.clear();

		for (Recipe<I> recipe : crafter.getRecipes()) {
			for (Map.Entry<RequesterReference<I>, StackList<I>> entry : (input ? recipe.requestInput : recipe.requestOutput).entrySet()) {
				if (entry.getKey() == null) {
					network.addAll(entry.getValue());
				} else if (!map.containsKey(entry.getKey())) {
					map.put(entry.getKey(), entry.getValue().copy());
				} else {
					map.get(entry.getKey()).addAll(entry.getValue());
				}
			}

			network.addAll(input ? recipe.missing : recipe.leftovers);
		}

		// Calculate total lines
		totalLines = 0;

		for (StackList<I> stackList : map.values()) {
			totalLines += (stackList.size() - 1) / STACKS_PER_LINE + 1;
		}

		if (!network.isEmpty()) {
			totalLines += (network.size() - 1) / STACKS_PER_LINE + 1;
		}

		if (totalLines < lines) {
			totalLines = lines;
		}
	}

	@Override
	public void addTooltip(List<String> list) {
		if (!isFullyOpened()) {
			list.add(getTitle());
			return;
		}

		int mouseX = gui.getMouseX() - posX();
		int mouseY = gui.getMouseY() - posY;

		// Add map
		int i = 0;
		for (Map.Entry<RequesterReference<I>, StackList<I>> entry : map.entrySet()) {
			if (i >= currentLine && i < currentLine + lines) {
				int x = sideOffset() + 2;
				int y = 21 + (i - currentLine) * STACK_HEIGHT;

				// Add icons
				if (!entry.getKey().getTileIcon().isEmpty() && mouseX >= x - 1 && mouseX < x + 17 && mouseY >= y - 1 && mouseY < y + 17) {
					list.addAll(gui.getItemToolTip(entry.getKey().getTileIcon()));
				}

				if (!entry.getKey().getIcon().isEmpty() && mouseX >= x + 17 && mouseX < x + 35 && mouseY >= y - 1 && mouseY < y + 17) {
					list.addAll(gui.getItemToolTip(entry.getKey().getIcon()));
				}
			}

			i = addTooltip(entry.getValue(), i, list, mouseX, mouseY);
			if (i >= currentLine + lines) {
				return;
			}
		}

		// Add network
		if (!network.isEmpty()) {
			i = addTooltip(network, i, list, mouseX, mouseY);
			if (i >= currentLine + lines) {
				return;
			}
		}
	}

	private int addTooltip(StackList<I> value, int i, List<String> list, int mouseX, int mouseY) {
		Iterator<Type<I>> iterator = value.getTypeIterator();
		while (iterator.hasNext()) {
			for (int j = 0; j < STACKS_PER_LINE; j++) {
				Type<I> type = iterator.next();

				if (i >= currentLine && i < currentLine + lines) {
					int x = sideOffset() + 2 + 38 + STACK_HEIGHT * j;
					int y = 21 + (i - currentLine) * STACK_HEIGHT;

					if (mouseX >= x - 1 && mouseX < x + 17 && mouseY >= y - 1 && mouseY < y + 17) {
						list.addAll(type.getTooltip(gui));
					}
				}

				if (!iterator.hasNext())
					break;
			}

			i += 1;

			if (i >= currentLine + lines) {
				return i;
			}
		}

		return i;
	}

	@Override
	public void drawForeground() {
		GlStateManager.disableLighting();
		drawTabIcon(input ? CoreTextures.ICON_INPUT : CoreTextures.ICON_OUTPUT);
		if (!isFullyOpened())
			return;

		if (currentLine > 0) {
			gui.drawIcon(CoreTextures.ICON_ARROW_UP, sideOffset() + maxWidth - 20, 16);
		} else {
			gui.drawIcon(CoreTextures.ICON_ARROW_UP_INACTIVE, sideOffset() + maxWidth - 20, 16);
		}

		if (currentLine < totalLines - lines) {
			gui.drawIcon(CoreTextures.ICON_ARROW_DOWN, sideOffset() + maxWidth - 20, 76);
		} else {
			gui.drawIcon(CoreTextures.ICON_ARROW_DOWN_INACTIVE, sideOffset() + maxWidth - 20, 76);
		}

		getFontRenderer().drawString(getTitle(), sideOffset() + 18, 6, 0xFF373737);

		RenderHelper.disableStandardItemLighting();
		RenderHelper.enableGUIStandardItemLighting();

		// Render line separator
		{
			int x = sideOffset() + 2;
			int y = 21;

			Gui.drawRect(x + 37 - 1, y - 1, x + 37, y + lines * STACK_HEIGHT - 1, 0xFF373737);
		}

		// Render map
		int i = 0;
		for (Map.Entry<RequesterReference<I>, StackList<I>> entry : map.entrySet()) {
			if (i >= currentLine && i < currentLine + lines) {
				int x = sideOffset() + 2;
				int y = 21 + (i - currentLine) * STACK_HEIGHT;

				// Render line separator
				if (i > currentLine) {
					Gui.drawRect(x - 1, y - 1, sideOffset() + maxWidth - 20, y, 0xFF373737);
				}

				// Render icons
				if (!entry.getKey().getTileIcon().isEmpty()) {
					gui.drawItemStack(entry.getKey().getTileIcon(), x, y, false, null);
				}

				if (!entry.getKey().getIcon().isEmpty()) {
					gui.drawItemStack(entry.getKey().getIcon(), x + 18, y, false, null);
				}
			}

			i = render(entry.getValue(), i);
			if (i >= currentLine + lines) {
				return;
			}
		}

		// Render network
		if (!network.isEmpty()) {
			if (i >= currentLine && i < currentLine + lines) {
				int x = sideOffset() + 2;
				int y = 21 + (i - currentLine) * STACK_HEIGHT;

				// Render line separator
				if (i > currentLine) {
					Gui.drawRect(x - 1, y - 1, sideOffset() + maxWidth - 20, y, 0xFF373737);
				}
			}

			i = render(network, i);
			if (i >= currentLine + lines) {
				return;
			}
		}

		// Finished
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
	}

	private int render(StackList<I> list, int i) {
		Iterator<Type<I>> iterator = list.getTypeIterator();
		while (iterator.hasNext()) {
			for (int j = 0; j < STACKS_PER_LINE; j++) {
				Type<I> type = iterator.next();

				if (i >= currentLine && i < currentLine + lines) {
					int x = sideOffset() + 2 + 38 + STACK_HEIGHT * j;
					int y = 21 + (i - currentLine) * STACK_HEIGHT;

					StackHandler.render(gui, x, y, type.getAsStack(), StackHandler.getScaledNumber(list.amount(type)));
				}

				if (!iterator.hasNext())
					break;
			}

			i += 1;

			if (i >= currentLine + lines) {
				GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
				return i;
			}
		}

		return i;
	}

	@Override
	public boolean onMouseWheel(int mouseX, int mouseY, int movement) {
		if (!isFullyOpened())
			return false;

		if (movement > 0) {
			currentLine = MathHelper.clamp(currentLine - 1, 0, totalLines - lines);
			return true;
		} else if (movement < 0) {
			currentLine = MathHelper.clamp(currentLine + 1, 0, totalLines - lines);
			return true;
		}

		return false;
	}

	private String getTitle() {
		return StringHelper.localize("info.logistics.tab." + (input ? "input" : "output"));
	}

}
