package astavie.thermallogistics.client.gui.element;

import astavie.thermallogistics.client.gui.IFluidGui;
import astavie.thermallogistics.util.StackHandler;
import codechicken.lib.fluid.FluidUtils;
import cofh.core.gui.GuiContainerCore;
import cofh.core.util.helpers.FluidHelper;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ElementSlotFluid extends ElementSlot<FluidStack> {

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
		if (fluid != null)
			StackHandler.render(gui, posX + 1, posY + 1, fluid, count);
	}

	@Override
	protected void addTooltip(int mouseX, int mouseY, List<String> list) {
		if (fluid.get() != null)
			list.add(fluid.get().getLocalizedName());
	}

	@Override
	public Object getIngredient() {
		return fluid.get();
	}

	@Override
	public boolean onMousePressed(int mouseX, int mouseY, int mouseButton) {
		if (mouseButton < 2 && intersectsWith(mouseX, mouseY)) {
			FluidStack get = fluid.get();
			FluidStack drag = null;
			FluidStack fluid;

			if (gui instanceof IFluidGui)
				drag = ((IFluidGui) gui).getFluid();
			if (drag == null)
				drag = FluidHelper.getFluidForFilledItem(gui.draggedStack.isEmpty() ? gui.mc.player.inventory.getItemStack() : gui.draggedStack);

			if (drag == null)
				fluid = null;
			else if (mouseButton == 0 || !count)
				fluid = drag.copy();
			else if (FluidHelper.isFluidEqual(get, drag))
				fluid = FluidUtils.copy(get, get.amount + drag.amount);
			else
				fluid = drag.copy();

			consumer.accept(fluid);
			return true;
		}
		return false;
	}

	@Override
	public void accept(@Nonnull FluidStack ingredient) {
		consumer.accept(ingredient);
	}

}
