package astavie.thermallogistics.gui.client.slots;

import astavie.thermallogistics.attachment.CrafterFluid;
import astavie.thermallogistics.gui.client.GuiCrafter;
import astavie.thermallogistics.gui.client.element.ElementSlotFluid;
import cofh.core.network.PacketHandler;

public class CrafterFluidSlots implements ICrafterSlots<CrafterFluid> {

	@Override
	public void addSlots(CrafterFluid crafter, GuiCrafter gui) {
		for (int x = 0; x < gui.container.inputWidth; x++) {
			for (int y = 0; y < gui.container.inputHeight; y++) {
				final int slot = x + y * gui.container.inputWidth;
				gui.addElement(new ElementSlotFluid(gui, gui.container.input + x * 18, gui.container.y + gui.container.inputOffset + y * 18, true, () -> crafter.inputs[slot], stack -> PacketHandler.sendToServer(crafter.getPacket(stack, true, slot))));
			}
		}
		for (int i = 0; i < crafter.outputs.length; i++) {
			final int slot = i;
			gui.addElement(new ElementSlotFluid(gui, gui.container.output + i * 18, gui.container.y + (int) (18 * 2.5), true, () -> crafter.outputs[slot], stack -> PacketHandler.sendToServer(crafter.getPacket(stack, false, slot))));
		}
	}

}
