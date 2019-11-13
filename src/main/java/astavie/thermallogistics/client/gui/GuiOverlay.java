package astavie.thermallogistics.client.gui;

import cofh.core.gui.GuiContainerCore;
import cofh.core.gui.element.ElementBase;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.inventory.Container;
import net.minecraft.util.ResourceLocation;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public abstract class GuiOverlay extends GuiContainerCore {

	private Overlay overlay;

	public GuiOverlay(Container container, ResourceLocation texture) {
		super(container, texture);
	}

	protected void setOverlay(Overlay overlay) {
		if (this.overlay != null) {
			elements.remove(this.overlay);
		}

		this.overlay = overlay;

		if (this.overlay != null) {
			elements.add(this.overlay);
		}
	}

	@Override
	protected void mouseClicked(int mX, int mY, int mouseButton) throws IOException {
		if (overlay != null && !withinOverlay(mX - guiLeft, mY - guiTop))
			setOverlay(null);

		super.mouseClicked(mX, mY, mouseButton);
	}

	protected boolean withinOverlay(int x, int y) {
		return overlay != null && getElementAtPosition(x, y) == overlay;
	}

	protected abstract int getOverlaySheetX();

	protected abstract int getOverlaySheetY();

	private void overlay(int x, int y, int width, int height) {
		nineInchSprite(x, y, width - 8, height - 8, 1, 4, getOverlaySheetX(), getOverlaySheetY());
	}

	private void nineInchSprite(int x, int y, int width, int height, int middle, int edge, int sheetX, int sheetY) {
		// Copied from GuiRecipeOverlay
		drawTexturedModalRect(x, y, sheetX, sheetY, edge, edge);
		drawTexturedModalRect(x + edge + width * middle, y, sheetX + middle + edge, sheetY, edge, edge);
		drawTexturedModalRect(x, y + edge + height * middle, sheetX, sheetY + middle + edge, edge, edge);
		drawTexturedModalRect(x + edge + width * middle, y + edge + height * middle, sheetX + middle + edge, sheetY + middle + edge, edge, edge);

		for (int i = 0; i < width; ++i) {
			drawTexturedModalRect(x + edge + i * middle, y, sheetX + edge, sheetY, middle, edge);

			for (int j = 0; j < height; ++j) {
				if (i == 0) {
					drawTexturedModalRect(x, y + edge + j * middle, sheetX, sheetY + edge, edge, middle);
				}

				drawTexturedModalRect(x + edge + i * middle, y + edge + j * middle, sheetX + edge, sheetY + edge, middle, middle);

				if (i == width - 1) {
					drawTexturedModalRect(x + edge + width * middle, y + edge + j * middle, sheetX + middle + edge, sheetY + edge, edge, middle);
				}
			}

			drawTexturedModalRect(x + edge + i * middle, y + edge + height * middle, sheetX + edge, sheetY + middle + edge, middle, edge);
		}
	}

	@Override
	public void addTooltips(List<String> tooltip) {
		if (withinOverlay(mouseX, mouseY)) {
			overlay.addTooltip(tooltip);
		} else {
			super.addTooltips(tooltip);
		}
	}

	public class Overlay extends ElementBase {

		public List<ElementBase> elements = new LinkedList<>();

		private float ticks;

		public Overlay(int posX, int posY, int width, int height) {
			super(GuiOverlay.this, posX - 4, posY - 4, width + 8, height + 8);
		}

		@Override
		public void drawBackground(int mouseX, int mouseY, float gameTicks) {
			ticks = gameTicks;
		}

		@Override
		public void drawForeground(int mouseX, int mouseY) {
			GlStateManager.pushMatrix();
			GlStateManager.translate(0, 0, 400);

			GlStateManager.color(1, 1, 1, 1);

			bindTexture(GuiOverlay.this.texture);
			overlay(posX, posY, sizeX, sizeY);

			GlStateManager.translate(posX + 4, posY + 4, 0);

			int mx = mouseX - posX - 4;
			int my = mouseY - posY - 4;

			GlStateManager.disableLighting();

			for (ElementBase element : elements) {
				element.drawBackground(mx, my, ticks);
			}

			for (ElementBase element : elements) {
				element.drawForeground(mx, my);
			}

			GlStateManager.popMatrix();
		}

		@Override
		public void addTooltip(List<String> list) {
			int mx = gui.getMouseX() - posX - 4;
			int my = gui.getMouseY() - posY - 4;

			for (ElementBase element : elements)
				if (element.intersectsWith(mx, my))
					element.addTooltip(list);
		}

		@Override
		public boolean onMousePressed(int mouseX, int mouseY, int mouseButton) throws IOException {
			int mx = mouseX - posX - 4;
			int my = mouseY - posY - 4;

			for (ElementBase element : elements)
				if (element.intersectsWith(mx, my))
					element.onMousePressed(mx, my, mouseButton);

			return true;
		}

		@Override
		public void onMouseReleased(int mouseX, int mouseY) {
			int mx = mouseX - posX - 4;
			int my = mouseY - posY - 4;

			for (ElementBase element : elements)
				element.onMouseReleased(mx, my);
		}

		@Override
		public boolean onMouseWheel(int mouseX, int mouseY, int movement) {
			int mx = mouseX - posX - 4;
			int my = mouseY - posY - 4;

			for (ElementBase element : elements)
				if (element.intersectsWith(mx, my))
					element.onMouseWheel(mx, my, movement);

			return true;
		}

		@Override
		public boolean onKeyTyped(char characterTyped, int keyPressed) {
			return elements.stream().anyMatch(e -> e.onKeyTyped(characterTyped, keyPressed));
		}

	}

}
