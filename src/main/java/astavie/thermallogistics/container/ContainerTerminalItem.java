package astavie.thermallogistics.container;

import astavie.thermallogistics.client.gui.GuiTerminalItem;
import astavie.thermallogistics.tile.TileTerminalItem;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ContainerTerminalItem extends ContainerTerminal {

	@SideOnly(Side.CLIENT)
	public GuiTerminalItem gui;

	public ContainerTerminalItem(TileTerminalItem tile, InventoryPlayer inventory) {
		super(tile, inventory);
	}

	@Override
	protected void addSlots() {
		for (int y = 0; y < 3; y++)
			for (int x = 0; x < 9; x++)
				addSlotToContainer(new Slot(((TileTerminalItem) super.tile).inventory, x + y * 9, 8 + x * 18, 88 + y * 18));
	}

	@Override
	protected int getPlayerInventoryVerticalOffset() {
		return 156;
	}

	@Override
	protected int getSizeInventory() {
		return 28;
	}

}
