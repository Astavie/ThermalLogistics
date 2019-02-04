package astavie.thermallogistics.gui.client.element;

import cofh.core.gui.GuiContainerCore;
import cofh.core.gui.element.ElementBase;
import cofh.core.init.CoreProps;
import cofh.core.util.helpers.RenderHelper;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

import java.util.List;

public abstract class ElementSlot extends ElementBase {

	private static final ResourceLocation SLOT = new ResourceLocation(CoreProps.PATH_ELEMENTS + "slot.png");

	public ElementSlot(GuiContainerCore gui, int posX, int posY) {
		super(gui, posX - 1, posY - 1, 18, 18);
	}

	@Override
	public void drawBackground(int mouseX, int mouseY, float gameTicks) {
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

		gui.zLevel = 100.0F;
		gui.itemRender.zLevel = 100.0F;

		GlStateManager.disableLighting();
		RenderHelper.bindTexture(SLOT);
		gui.drawSizedTexturedModalRect(posX, posY, 0, 0, 18, 18, 32, 32);
		GlStateManager.enableLighting();

		GlStateManager.enableDepth();
		drawSlot(mouseX, mouseY);

		gui.itemRender.zLevel = 0.0F;
		gui.zLevel = 0.0F;

		GlStateManager.disableLighting();
		GlStateManager.disableDepth();
		if (intersectsWith(mouseX, mouseY)) {
			GlStateManager.colorMask(true, true, true, false);
			gui.drawGradientRect(posX + 1, posY + 1, posX + 17, posY + 17, 0x80FFFFFF, 0x80FFFFFF);
			GlStateManager.colorMask(true, true, true, true);
		}
	}

	@Override
	public void drawForeground(int mouseX, int mouseY) {
	}

	@Override
	public void addTooltip(List<String> list) {
		if (intersectsWith(gui.getMouseX(), gui.getMouseY()) && gui.mc.player.inventory.getItemStack().isEmpty())
			addTooltip(gui.getMouseX(), gui.getMouseY(), list);
	}

	protected abstract void drawSlot(int mouseX, int mouseY);

	protected abstract void addTooltip(int mouseX, int mouseY, List<String> list);

}
