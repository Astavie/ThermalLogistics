package astavie.thermallogistics.gui.client.element;

import cofh.core.gui.GuiContainerCore;
import cofh.core.util.helpers.FluidHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

import java.text.NumberFormat;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ElementSlotFluid extends ElementSlot {

	private final boolean count;
	private final Supplier<FluidStack> fluid;
	private final Consumer<FluidStack> consumer;

	public ElementSlotFluid(GuiContainerCore gui, int posX, int posY, boolean count, Supplier<FluidStack> fluid, Consumer<FluidStack> consumer) {
		super(gui, posX, posY);
		this.count = count;
		this.fluid = fluid;
		this.consumer = consumer;
	}

	@Override
	protected void drawSlot(int mouseX, int mouseY) {
		if (fluid.get() != null) {
			GlStateManager.disableLighting();
			gui.drawFluid(posX + 1, posY + 1, fluid.get(), 16, 16);

			if (count) {
				GlStateManager.disableDepth();
				GlStateManager.disableBlend();

				GlStateManager.pushMatrix();
				GlStateManager.scale(0.5, 0.5, 0.5);
				NumberFormat format = NumberFormat.getInstance(Minecraft.getMinecraft().getLanguageManager().getCurrentLanguage().getJavaLocale());
				String amount = format.format(fluid.get().amount);
				gui.getFontRenderer().drawStringWithShadow(amount, (posX + 17) * 2 - gui.getFontRenderer().getStringWidth(amount), (posY + 13) * 2, 0xFFFFFF);
				GlStateManager.popMatrix();
			}
		}
	}

	@Override
	protected void drawIntersect(int mouseX, int mouseY) {
		if (fluid.get() != null)
			gui.drawHoveringText(fluid.get().getLocalizedName(), mouseX, mouseY);
	}

	@Override
	public boolean onMousePressed(int mouseX, int mouseY, int mouseButton) {
		if (mouseButton < 2 && intersectsWith(mouseX, mouseY)) {
			FluidStack drag = FluidHelper.getFluidForFilledItem(gui.draggedStack.isEmpty() ? gui.mc.player.inventory.getItemStack() : gui.draggedStack);
			FluidStack fluid;
			if (drag == null)
				fluid = null;
			else if (mouseButton == 0 || !count)
				fluid = drag.copy();
			else if (FluidHelper.isFluidEqual(this.fluid.get(), drag)) {
				fluid = this.fluid.get().copy();
				fluid.amount += Fluid.BUCKET_VOLUME;
			} else {
				fluid = drag.copy();
				fluid.amount = Fluid.BUCKET_VOLUME;
			}
			consumer.accept(fluid);
			return true;
		}
		return false;
	}

}
