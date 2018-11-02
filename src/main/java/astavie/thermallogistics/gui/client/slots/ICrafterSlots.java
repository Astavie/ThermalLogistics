package astavie.thermallogistics.gui.client.slots;

import astavie.thermallogistics.attachment.Crafter;
import astavie.thermallogistics.gui.client.GuiCrafter;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public interface ICrafterSlots<C extends Crafter<?, ?, ?>> {

	void addSlots(C crafter, GuiCrafter gui);

}
