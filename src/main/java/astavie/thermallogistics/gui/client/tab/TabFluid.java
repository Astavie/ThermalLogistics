package astavie.thermallogistics.gui.client.tab;

import astavie.thermallogistics.attachment.CrafterFluid;
import astavie.thermallogistics.gui.client.GuiCrafter;
import astavie.thermallogistics.gui.client.element.ElementButtonIcon;
import astavie.thermallogistics.gui.client.element.ElementSlotFluid;
import astavie.thermallogistics.proxy.ProxyClient;
import astavie.thermallogistics.util.Shared;
import cofh.core.gui.element.ElementTextField;
import cofh.core.gui.element.ElementTextFieldLimited;
import cofh.core.gui.element.tab.TabBase;
import cofh.core.init.CoreTextures;
import cofh.core.network.PacketHandler;
import cofh.core.util.helpers.StringHelper;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.fluids.FluidStack;

import java.util.List;

public class TabFluid extends TabBase {

	public final ElementSlotFluid slot;

	private final Shared<FluidStack> fluid = new Shared<>();
	private final CrafterFluid crafter;
	private final ElementTextField text;

	public TabFluid(GuiCrafter gui) {
		super(gui);
		this.crafter = (CrafterFluid) gui.crafter;
		this.backgroundColor = 0x345fda;
		this.maxHeight = 63;
		this.maxWidth = 72;
		this.text = new ElementTextFieldLimited(gui, sideOffset() + 26, 25, 33, 10, (short) 5).setFilter("0123456789", false);
		this.slot = new ElementSlotFluid(gui, sideOffset() + 5, 22, false, fluid, fluid);

		this.addElement(text);
		this.addElement(slot);
		this.addElement(new ElementButtonIcon(gui, sideOffset() + 25, 40, CoreTextures.ICON_INPUT, "info.logistics.fluid.input", () -> {
			if (fluid.get() != null) {
				FluidStack fluid = this.fluid.get().copy();
				fluid.amount = 1000;
				if (!text.getText().isEmpty()) {
					fluid.amount = Integer.parseInt(text.getText());
					if (fluid.amount == 0)
						return;
				}
				for (int i = 0; i < crafter.inputs.length; i++) {
					if (crafter.inputs[i] == null) {
						crafter.inputs[i] = fluid;
						PacketHandler.sendToServer(crafter.getPacket(fluid, true, i));
						break;
					}
				}
			}
		}));
		this.addElement(new ElementButtonIcon(gui, sideOffset() + 44, 40, CoreTextures.ICON_OUTPUT, "info.logistics.fluid.output", () -> {
			if (fluid.get() != null) {
				FluidStack fluid = this.fluid.get().copy();
				fluid.amount = 1000;
				if (!text.getText().isEmpty()) {
					fluid.amount = Integer.parseInt(text.getText());
					if (fluid.amount == 0)
						return;
				}
				for (int i = 0; i < crafter.outputs.length; i++) {
					if (crafter.outputs[i] == null) {
						crafter.outputs[i] = fluid;
						PacketHandler.sendToServer(crafter.getPacket(fluid, false, i));
						break;
					}
				}
			}
		}));
	}

	@Override
	public int posX() {
		return super.posX();
	}

	@Override
	protected void drawForeground() {
		GlStateManager.disableLighting();
		drawTabIcon(ProxyClient.ICON_FLUID);
		if (!isFullyOpened())
			return;
		getFontRenderer().drawStringWithShadow(getTitle(), sideOffset() + 18, 6, headerColor);
	}

	@Override
	public void addTooltip(List<String> list) {
		if (!isFullyOpened())
			list.add(getTitle());
	}

	private String getTitle() {
		return StringHelper.localize("info.logistics.tab.fluid");
	}

}
