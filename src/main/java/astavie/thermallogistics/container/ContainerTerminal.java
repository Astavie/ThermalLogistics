package astavie.thermallogistics.container;

import astavie.thermallogistics.tile.TileTerminal;
import cofh.core.gui.container.ContainerCore;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.IContainerListener;

import javax.annotation.Nonnull;

public abstract class ContainerTerminal extends ContainerCore {

	public final TileTerminal<?> tile;

	public ContainerTerminal(TileTerminal<?> tile, InventoryPlayer inventory) {
		this.tile = tile;

		addSlotToContainer(new SlotSpecial(tile.requester, 0, 8, 56));
		addSlots();

		bindPlayerInventory(inventory);
	}

	protected abstract void addSlots();

	@Override
	public boolean canInteractWith(@Nonnull EntityPlayer player) {
		return tile.isUsable(player);
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
