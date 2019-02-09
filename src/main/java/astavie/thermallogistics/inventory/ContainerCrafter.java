package astavie.thermallogistics.inventory;

import astavie.thermallogistics.attachment.ICrafter;
import cofh.thermaldynamics.duct.Attachment;
import cofh.thermaldynamics.gui.container.ContainerAttachmentBase;
import net.minecraft.entity.player.InventoryPlayer;

public class ContainerCrafter extends ContainerAttachmentBase {

	public final ICrafter<?> crafter;

	public <C extends Attachment & ICrafter<?>> ContainerCrafter(InventoryPlayer inventory, C tile) {
		super(inventory, tile);
		this.crafter = tile;
	}

}
