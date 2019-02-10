package astavie.thermallogistics.client.gui.tab;

import astavie.thermallogistics.client.TLTextures;
import astavie.thermallogistics.client.gui.IFluidGui;
import astavie.thermallogistics.client.gui.element.ElementSlotFluid;
import astavie.thermallogistics.util.Shared;
import cofh.core.gui.GuiContainerCore;
import cofh.core.gui.element.ElementButtonManaged;
import cofh.core.gui.element.ElementTextField;
import cofh.core.gui.element.ElementTextFieldLimited;
import cofh.core.gui.element.tab.TabBase;
import cofh.core.util.helpers.StringHelper;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.fluids.FluidStack;

import java.util.List;

public class TabFluid extends TabBase {

	public final ElementSlotFluid slot;

	private final Shared<FluidStack> shared = new Shared<>();
	private final IFluidGui fluid;
	private final ElementTextField text;

	public TabFluid(GuiContainerCore gui, IFluidGui fluid) {
		this(gui, RIGHT, fluid);
	}

	public TabFluid(GuiContainerCore gui, int side, IFluidGui fluid) {
		super(gui, side);
		this.fluid = fluid;

		this.backgroundColor = 0x345fda;
		this.maxHeight = 64;
		this.maxWidth = 72;

		this.text = new ElementTextFieldLimited(gui, sideOffset() + 26, 25, 33, 10, (short) 5).setFilter("0123456789", false);
		this.slot = new ElementSlotFluid(gui, sideOffset() + 4, 21, shared, shared, false);

		addElement(text);
		addElement(slot);

		addElement(new ElementButtonManaged(gui, sideOffset() + 4, 41, 57, 16, StringHelper.localize("info.logistics.fluid.get")) {
			@Override
			public void onClick() {
				if (shared.get() == null)
					return;

				FluidStack fluid = shared.get().copy();
				fluid.amount = 1000;

				if (!text.getText().isEmpty()) {
					fluid.amount = Integer.parseInt(text.getText());
					if (fluid.amount == 0)
						return;
				}

				TabFluid.this.fluid.setFluid(fluid);
			}
		});
	}

	@Override
	public int posX() {
		return super.posX();
	}

	@Override
	protected void drawForeground() {
		GlStateManager.disableLighting();
		drawTabIcon(TLTextures.ICON_FLUID);
		if (!isFullyOpened())
			return;
		getFontRenderer().drawStringWithShadow(getTitle(), sideOffset() + 18, 6, headerColor);
	}

	@Override
	public void addTooltip(List<String> list) {
		if (!isFullyOpened())
			list.add(getTitle());
		else if (slot.intersectsWith(gui.getMouseX() - posX(), gui.getMouseY() - getPosY()))
			slot.addTooltip(list);
	}

	private String getTitle() {
		return StringHelper.localize("info.logistics.tab.fluid");
	}

}
