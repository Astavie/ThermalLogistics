package astavie.thermallogistics.client.gui.element;

import cofh.core.gui.GuiContainerCore;
import cofh.core.util.helpers.FluidHelper;
import cofh.core.util.helpers.StringHelper;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ElementSlotFluid extends ElementSlot {

	private final Supplier<FluidStack> fluid;
	private final Consumer<FluidStack> consumer;

	private final boolean count;

	public ElementSlotFluid(GuiContainerCore gui, int posX, int posY, Supplier<FluidStack> fluid, Consumer<FluidStack> consumer, boolean count) {
		super(gui, posX, posY);
		this.fluid = fluid;
		this.consumer = consumer;
		this.count = count;
	}

	@Override
	protected void drawSlot(int mouseX, int mouseY) {
		FluidStack fluid = this.fluid.get();
		if (fluid != null) {
			GlStateManager.disableLighting();
			gui.drawFluid(posX + 1, posY + 1, fluid, 16, 16);

			if (count) {
				GlStateManager.pushMatrix();

				GlStateManager.disableDepth();
				GlStateManager.disableBlend();

				GlStateManager.scale(0.5, 0.5, 0.5);
				String amount = StringHelper.getScaledNumber(fluid.amount);
				gui.getFontRenderer().drawStringWithShadow(amount, (posX + 17) * 2 - gui.getFontRenderer().getStringWidth(amount), (posY + 13) * 2, 0xFFFFFF);

				GlStateManager.popMatrix();
			}
		}
	}

	@Override
	protected void addTooltip(int mouseX, int mouseY, List<String> list) {
		if (fluid.get() != null)
			list.add(fluid.get().getLocalizedName());
	}

	@Override
	public boolean onMousePressed(int mouseX, int mouseY, int mouseButton) {
		if (mouseButton < 2 && intersectsWith(mouseX, mouseY)) {
			FluidStack get = fluid.get();
			FluidStack drag = FluidHelper.getFluidForFilledItem(gui.draggedStack.isEmpty() ? gui.mc.player.inventory.getItemStack() : gui.draggedStack);
			FluidStack fluid;

			if (drag == null)
				fluid = null;
			else if (mouseButton == 0 || !count)
				fluid = drag.copy();
			else if (FluidHelper.isFluidEqual(get, drag)) {
				fluid = get.copy();
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
