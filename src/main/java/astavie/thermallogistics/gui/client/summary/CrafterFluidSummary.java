package astavie.thermallogistics.gui.client.summary;

import astavie.thermallogistics.attachment.CrafterFluid;
import astavie.thermallogistics.proxy.ProxyClient;
import cofh.core.gui.GuiContainerCore;
import net.minecraftforge.fluids.FluidStack;

public class CrafterFluidSummary implements ICrafterSummary<CrafterFluid> {

	@Override
	public void renderSummary(CrafterFluid crafter, GuiContainerCore gui, int x, int y, int textColor) {
		FluidStack input = null, output = null;
		boolean bI = false, bO = false;

		boolean b = false;
		for (FluidStack stack : crafter.getInputs()) {
			if (stack != null) {
				if (input == null)
					input = stack;
				if (b) {
					bI = true;
					break;
				} else b = true;
			}
		}

		b = false;
		for (FluidStack stack : crafter.getOutputs()) {
			if (stack != null) {
				if (output == null)
					output = stack;
				if (b) {
					bI = true;
					break;
				} else b = true;
			}
		}

		if (input != null)
			gui.drawFluid(x, y, input, 16, 16);
		if (bI)
			gui.getFontRenderer().drawString("...", x + 19, y + 4, textColor);

		gui.drawIcon(ProxyClient.ICON_ARROW_RIGHT, x + 26, y);

		if (output != null)
			gui.drawFluid(x + 44, y, output, 16, 16);
		if (bO)
			gui.getFontRenderer().drawString("...", x + 63, y + 4, textColor);
	}

}
