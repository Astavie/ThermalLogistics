package astavie.thermallogistics.gui.container;

import astavie.thermallogistics.attachment.Crafter;
import cofh.thermaldynamics.gui.container.ContainerAttachmentBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;

public class ContainerCrafter extends ContainerAttachmentBase {

	public final int inputWidth;
	public final int inputHeight;
	public final int inputOffset;

	public final int input;
	public final int output;

	public final int y = 20;

	private final EntityPlayer player;
	private final Crafter.Cache cache;

	public ContainerCrafter(InventoryPlayer inventory, Crafter crafter) {
		super(inventory, crafter);

		inputWidth = crafter.type > 0 ? crafter.type + 1 : 2;
		inputHeight = crafter.type > 0 ? 2 : 1;
		inputOffset = crafter.type > 0 ? 0 : 18;

		input = 89 - inputWidth * 9;
		output = 89 - (crafter.type + 1) * 9;

		player = inventory.player;
		cache = crafter.createCache();
	}

	@Override
	public void detectAndSendChanges() {
		super.detectAndSendChanges();
		cache.detectAndSendChanges(player);
	}

}
