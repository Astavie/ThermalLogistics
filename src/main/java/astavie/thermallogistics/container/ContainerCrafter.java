package astavie.thermallogistics.container;

import astavie.thermallogistics.attachment.IAttachmentCrafter;
import cofh.thermaldynamics.duct.attachments.ConnectionBase;
import cofh.thermaldynamics.gui.container.ContainerAttachmentBase;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;

public class ContainerCrafter extends ContainerAttachmentBase {

	public final IAttachmentCrafter<?> crafter;

	public <C extends ConnectionBase & IAttachmentCrafter<?>> ContainerCrafter(InventoryPlayer inventory, C tile) {
		super(inventory, tile);
		this.crafter = tile;
	}

	@Override
	protected void addPlayerInventory(InventoryPlayer inventory) {
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 9; j++) {
				addSlotToContainer(new Slot(inventory, j + i * 9 + 9, 8 + j * 18, 20 + 123 + i * 18));
			}
		}
		for (int i = 0; i < 9; i++) {
			addSlotToContainer(new Slot(inventory, i, 8 + i * 18, 20 + 181));
		}
	}

}
