package astavie.thermallogistics.container;

import astavie.thermallogistics.attachment.ICrafter;
import cofh.thermaldynamics.duct.attachments.ConnectionBase;
import cofh.thermaldynamics.gui.container.ContainerAttachmentBase;
import net.minecraft.entity.player.InventoryPlayer;

public class ContainerCrafter extends ContainerAttachmentBase {

	public final ICrafter<?> crafter;

	public <C extends ConnectionBase & ICrafter<?>> ContainerCrafter(InventoryPlayer inventory, C tile) {
		super(inventory, tile);
		this.crafter = tile;
	}

}
