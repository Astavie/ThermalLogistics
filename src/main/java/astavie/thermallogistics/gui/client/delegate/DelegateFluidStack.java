package astavie.thermallogistics.gui.client.delegate;

import astavie.thermallogistics.attachment.CrafterFluid;
import astavie.thermallogistics.gui.client.element.ElementSlotFluid;
import cofh.core.gui.GuiContainerCore;
import cofh.core.gui.element.ElementBase;
import cofh.core.network.PacketHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.fluids.FluidStack;

import java.text.NumberFormat;

public class DelegateFluidStack implements IDelegate<FluidStack, CrafterFluid> {

	public static final DelegateFluidStack INSTANCE = new DelegateFluidStack();

	@Override
	public void drawStack(GuiContainerCore gui, int x, int y, FluidStack stack) {
		if (stack != null) {
			GlStateManager.disableLighting();
			gui.drawFluid(x, y, stack, 16, 16);

			GlStateManager.disableDepth();
			GlStateManager.disableBlend();

			GlStateManager.pushMatrix();
			GlStateManager.scale(0.5, 0.5, 0.5);
			NumberFormat format = NumberFormat.getInstance(Minecraft.getMinecraft().getLanguageManager().getCurrentLanguage().getJavaLocale());
			String amount = format.format(stack.amount);
			gui.getFontRenderer().drawStringWithShadow(amount, (x + 16) * 2 - gui.getFontRenderer().getStringWidth(amount), (y + 12) * 2, 0xFFFFFF);
			GlStateManager.popMatrix();
		}
	}

	@Override
	public void drawHover(GuiContainerCore gui, int mouseX, int mouseY, FluidStack stack) {
		if (stack != null)
			gui.drawHoveringText(stack.getLocalizedName(), mouseX, mouseY);
	}

	@Override
	public boolean isNull(FluidStack stack) {
		return stack == null || stack.amount == 0;
	}

	@Override
	public ElementBase createSlot(GuiContainerCore gui, int x, int y, int slot, CrafterFluid crafter, boolean input) {
		return new ElementSlotFluid(gui, x, y, true, input ? () -> crafter.inputs[slot] : () -> crafter.outputs[slot], stack -> PacketHandler.sendToServer(crafter.getPacket(stack, input, slot)));
	}

}
