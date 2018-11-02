package astavie.thermallogistics.gui.container;

import astavie.thermallogistics.tile.TileTerminal;
import cofh.core.gui.container.ContainerCore;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.IContainerListener;

public abstract class ContainerTerminal extends ContainerCore {

	public final TileTerminal tile;

	public ContainerTerminal(TileTerminal tile, InventoryPlayer player) {
		this.tile = tile;

		addSlotToContainer(new SlotSpecial(tile.requester, 0, 8, 74));
		addSlots(player);

		bindPlayerInventory(player);
	}

	protected abstract void addSlots(InventoryPlayer player);

	@Override
	public boolean canInteractWith(EntityPlayer playerIn) {
		return tile.isUsable(playerIn);
	}

	@Override
	public void addListener(IContainerListener listener) {
		super.addListener(listener);
		tile.register(this);
	}

	@Override
	public void onContainerClosed(EntityPlayer playerIn) {
		super.onContainerClosed(playerIn);
		tile.remove(this);
	}

}
