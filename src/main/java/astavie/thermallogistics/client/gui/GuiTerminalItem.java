package astavie.thermallogistics.client.gui;

import astavie.thermallogistics.ThermalLogistics;
import astavie.thermallogistics.client.gui.tab.TabRequest;
import astavie.thermallogistics.container.ContainerTerminalItem;
import astavie.thermallogistics.tile.TileTerminalItem;
import cofh.core.util.helpers.ItemHelper;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.lang3.tuple.Triple;

public class GuiTerminalItem extends GuiTerminal<ItemStack> {

	public GuiTerminalItem(TileTerminalItem tile, InventoryPlayer inventory) {
		super(tile, new ContainerTerminalItem(tile, inventory), new ResourceLocation(ThermalLogistics.MOD_ID, "textures/gui/terminal.png"));
		this.xSize = 194;
		this.ySize = 250;
	}

	@Override
	public void initGui() {
		super.initGui();
		addTab(new TabRequest(this, ((TileTerminalItem) tile).requests.stacks, tile)).setOffsets(-18, 74);
	}

	@Override
	protected boolean isSelected(ItemStack stack) {
		return ItemHelper.itemsIdentical(selected, stack);
	}

	@Override
	protected void updateFilter() {
		filter.clear();
		for (Triple<ItemStack, Long, Boolean> stack : tile.terminal) {
			if (search.getText().isEmpty() || stack.getLeft().getDisplayName().toLowerCase().contains(search.getText().toLowerCase())) {
				filter.add(stack);
			} else for (String string : getItemToolTip(stack.getLeft())) {
				if (string.toLowerCase().contains(search.getText().toLowerCase())) {
					filter.add(stack);
					break;
				}
			}
		}

		filter.sort((i1, i2) -> {
			// First compare stack size
			int count = -Long.compare(i1.getMiddle(), i2.getMiddle());
			if (count != 0)
				return count;

			// Then compare item id
			int id = Integer.compare(Item.getIdFromItem(i1.getLeft().getItem()), Item.getIdFromItem(i2.getLeft().getItem()));
			if (id != 0)
				return id;

			// Then compare item damage
			int damage = Integer.compare(i1.getLeft().getItemDamage(), i2.getLeft().getItemDamage());
			if (damage != 0)
				return damage;

			// Then compare sub items
			NonNullList<ItemStack> list = NonNullList.create();
			i1.getLeft().getItem().getSubItems(CreativeTabs.SEARCH, list);
			for (ItemStack stack : list) {
				if (ItemHelper.itemsIdentical(i1.getLeft(), stack))
					return -1;
				if (ItemHelper.itemsIdentical(i2.getLeft(), stack))
					return 1;
			}

			// Then compare nbt data
			if (i1.getLeft().getTagCompound() == null && i2.getLeft().getTagCompound() != null)
				return -1;
			if (i1.getLeft().getTagCompound() != null && i2.getLeft().getTagCompound() == null)
				return 1;

			// Then we give up
			return 0;
		});
	}

	@Override
	protected void updateAmount(Triple<ItemStack, Long, Boolean> stack) {
		amount.setText(Long.toString(stack.getRight() ? selected.getMaxStackSize() : Math.min(stack.getMiddle(), selected.getMaxStackSize())));
	}

}
