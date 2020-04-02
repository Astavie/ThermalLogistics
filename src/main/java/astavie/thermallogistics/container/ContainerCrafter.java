package astavie.thermallogistics.container;

import astavie.thermallogistics.attachment.IAttachmentCrafter;
import cofh.thermaldynamics.duct.attachments.ConnectionBase;
import cofh.thermaldynamics.gui.container.ContainerAttachmentBase;
import net.minecraft.entity.player.InventoryPlayer;

public class ContainerCrafter extends ContainerAttachmentBase {

	public final IAttachmentCrafter<?> crafter;

	public <C extends ConnectionBase & IAttachmentCrafter<?>> ContainerCrafter(InventoryPlayer inventory, C tile) {
		super(inventory, tile);
		this.crafter = tile;
	}

}
