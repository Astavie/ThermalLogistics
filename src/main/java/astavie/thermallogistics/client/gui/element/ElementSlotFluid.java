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
			FluidStack drag = null;

			if (gui instanceof IFluidGui)
				drag = ((IFluidGui) gui).getFluid();
			if (drag == null)
				drag = FluidHelper.getFluidForFilledItem(gui.draggedStack.isEmpty() ? gui.mc.player.inventory.getItemStack() : gui.draggedStack);

			accept(drag, mouseButton);
			return true;
		}
		return false;
	}

	@Override
	public void accept(@Nonnull FluidStack ingredient, int mouse) {
		FluidStack get = fluid.get();
		FluidStack fluid;

		if (ingredient == null)
			fluid = null;
		else if (mouse == 0 || !count)
			fluid = ingredient.copy();
		else if (FluidHelper.isFluidEqual(get, ingredient))
			fluid = FluidUtils.copy(get, get.amount + ingredient.amount);
		else
			fluid = ingredient.copy();

		consumer.accept(fluid);
	}

	@Override
	public void accept(@Nonnull FluidStack ingredient) {
		consumer.accept(ingredient);
	}

}
