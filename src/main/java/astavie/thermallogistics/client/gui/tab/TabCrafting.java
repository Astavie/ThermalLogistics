package astavie.thermallogistics.client.gui.tab;

import astavie.thermallogistics.client.TLTextures;
import astavie.thermallogistics.client.gui.element.ElementSlotItem;
import astavie.thermallogistics.util.Shared;
import cofh.core.gui.GuiContainerCore;
import cofh.core.gui.element.*;
import cofh.core.gui.element.tab.TabBase;
import cofh.core.inventory.InventoryCraftingFalse;
import cofh.core.util.helpers.StringHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;

import java.util.List;
import java.util.function.Supplier;

public class TabCrafting extends TabBase {

	public final ElementSlotItem[] grid;
	public final ElementBase output;
	public final ElementTextField amount;
	public final ElementButtonBase button;
	private final Supplier<Boolean> enabled;

	public TabCrafting(GuiContainerCore gui, Shared.Item[] shared, Runnable click, Runnable request, Supplier<Boolean> enabled) {
		this(gui, RIGHT, shared, click, request, enabled);
		this.maxWidth = 118;
		this.maxHeight = 100;
		this.headerColor = 0xFF373737;
	}

	public TabCrafting(GuiContainerCore gui, int side, Shared.Item[] shared, Runnable click, Runnable request, Supplier<Boolean> enabled) {
		super(gui, side);
		this.enabled = enabled;

		int x = sideOffset() + 1;
		int y = 21;

		grid = new ElementSlotItem[9];

		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 3; j++) {
				if (shared[j + i * 3] == null) {
					Shared.Item stack = new Shared.Item(ItemStack.EMPTY);
					shared[j + i * 3] = stack;
				}

				Shared<ItemStack> stack = shared[j + i * 3];
				addElement(grid[j + i * 3] = new ElementSlotItem(gui, x + j * 18, y + i * 18, stack, stack, false));
			}
		}

		addElement(new ElementSimple(gui, x + 3 * 18 + 6, y + 18 + 1).setTexture(GuiContainerCore.TEX_ARROW_RIGHT, 64, 16).setSize(24, 16));

		addElement(output = new ElementSlotItem(gui, x + 5 * 18, y + 18, () -> {
			InventoryCrafting inventory = new InventoryCraftingFalse(3, 3);
			for (int i = 0; i < 9; i++)
				inventory.setInventorySlotContents(i, shared[i].get());
			IRecipe recipe = CraftingManager.findMatchingRecipe(inventory, Minecraft.getMinecraft().world);
			if (recipe != null)
				return recipe.getCraftingResult(inventory);
			return ItemStack.EMPTY;
		}, null, true) {

			@Override
			public boolean onMousePressed(int mouseX, int mouseY, int mouseButton) {
				if (mouseButton != 2)
					click.run();
				return true;
			}

		});

		addElement(button = new ElementButtonManaged(gui, x + 6 * 18 - 50, y + 3 * 18 + 2, 50, 16, StringHelper.localize("gui.logistics.terminal.request")) {
			@Override
			public void onClick() {
				request.run();
			}
		});

		addElement(amount = new ElementTextFieldLimited(gui, x + 1, y + 3 * 18 + 5, 52, 10).setFilter("0123456789", false));
	}

	@Override
	public int posX() {
		return super.posX();
	}

	@Override
	protected void drawForeground() {
		GlStateManager.disableLighting();
		drawTabIcon(TLTextures.ICON_CRAFTING);
		if (!isFullyOpened())
			return;
		getFontRenderer().drawString(getTitle(), sideOffset() + 18, 7, headerColor);
	}

	@Override
	public void addTooltip(List<String> list) {
		if (!isFullyOpened()) {
			list.add(getTitle());
		} else {
			int mouseX = gui.getMouseX() - posX();
			int mouseY = gui.getMouseY() - posY;

			for (ElementBase element : grid)
				if (element.intersectsWith(mouseX, mouseY))
					element.addTooltip(list);
			if (output.intersectsWith(mouseX, mouseY))
				output.addTooltip(list);
		}
	}

	@Override
	public void update() {
		super.update();
		button.setEnabled(enabled.get());
	}

	private String getTitle() {
		return StringHelper.localize("info.logistics.tab.crafting");
	}

}
