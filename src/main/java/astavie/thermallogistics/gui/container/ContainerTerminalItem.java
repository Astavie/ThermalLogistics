package astavie.thermallogistics.gui.container;

import astavie.thermallogistics.tile.TileTerminalItem;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;

public class ContainerTerminalItem extends ContainerTerminal {

	public ContainerTerminalItem(TileTerminalItem tile, InventoryPlayer player) {
		super(tile, player);
	}

	@Override
	protected void addSlots(InventoryPlayer player) {
		TileTerminalItem tile = (TileTerminalItem) this.tile;
		for (int y = 0; y < 3; y++)
			for (int x = 0; x < 9; x++)
				addSlotToContainer(new Slot(tile.inventory, x + y * 9, 8 + x * 18, 100 + y * 18));
	}

	@Override
	protected int getPlayerInventoryVerticalOffset() {
		return 168;
	}

	@Override
	protected int getSizeInventory() {
		return 28;
	}

}
