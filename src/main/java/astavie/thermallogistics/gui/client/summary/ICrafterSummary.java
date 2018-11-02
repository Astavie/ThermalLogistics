package astavie.thermallogistics.gui.client.summary;

import astavie.thermallogistics.attachment.Crafter;
import cofh.core.gui.GuiContainerCore;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public interface ICrafterSummary<C extends Crafter<?, ?, ?>> {

	void renderSummary(C crafter, GuiContainerCore gui, int x, int y, int textColor);

}
