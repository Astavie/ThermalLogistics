package astavie.thermallogistics.client.gui;

import astavie.thermallogistics.ThermalLogistics;
import astavie.thermallogistics.client.gui.tab.TabCrafting;
import astavie.thermallogistics.client.gui.tab.TabRequest;
import astavie.thermallogistics.container.ContainerTerminalItem;
import astavie.thermallogistics.process.RequestItem;
import astavie.thermallogistics.tile.TileTerminalItem;
import astavie.thermallogistics.util.Shared;
import cofh.core.gui.element.ElementBase;
import cofh.core.inventory.InventoryCraftingFalse;
import cofh.core.network.PacketHandler;
import cofh.core.network.PacketTileInfo;
import cofh.core.util.helpers.ItemHelper;
import cofh.core.util.helpers.StringHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class GuiTerminalItem extends GuiTerminal<ItemStack> {

	public final TileTerminalItem tile;

	public TabCrafting tabCrafting;
	public TabRequest tabRequest;

	private Pair<List<ItemStack>, List<String>> cache = Pair.of(null, null);

	public GuiTerminalItem(TileTerminalItem tile, InventoryPlayer inventory) {
		super(tile, new ContainerTerminalItem(tile, inventory), new ResourceLocation(ThermalLogistics.MOD_ID, "textures/gui/terminal.png"));
		((ContainerTerminalItem) inventorySlots).gui = this;
		this.tile = tile;
		this.xSize = 194;
		this.ySize = 250;
	}

	@Override
	public void addTooltips(List<String> tooltip) {
		super.addTooltips(tooltip);

		if (tabCrafting.button.intersectsWith(mouseX - tabCrafting.posX(), mouseY - tabCrafting.getPosY())) {
			List<String> list = cache.getRight();
			if (list != null)
				tooltip.addAll(list);
		}
	}

	private Pair<List<ItemStack>, List<String>> request() {
		if (Arrays.stream(tile.shared).allMatch(shared -> shared.test(ItemStack.EMPTY)))
			return Pair.of(null, null);

		List<Triple<ItemStack, Long, Boolean>> copy = new LinkedList<>();

		for (ItemStack stack : tile.inventory.items)
			copy.add(Triple.of(ItemHelper.cloneStack(stack, 1), (long) stack.getCount(), false));
		for (ItemStack stack : Minecraft.getMinecraft().player.inventory.mainInventory)
			copy.add(Triple.of(ItemHelper.cloneStack(stack, 1), (long) stack.getCount(), false));

		copy.addAll(tile.terminal);

		int count = 1;
		if (!tabCrafting.amount.getText().isEmpty())
			count = Integer.parseInt(tabCrafting.amount.getText());

		RequestItem request = new RequestItem(null);
		RequestItem missing = new RequestItem(null);

		a:
		for (Shared.Item item : tile.shared) {
			if (item.test(ItemStack.EMPTY))
				continue;

			int amount = count;

			for (int i = 0; i < copy.size(); i++) {
				Triple<ItemStack, Long, Boolean> triple = copy.get(i);
				if (triple.getMiddle() == 0L)
					continue;

				if (!item.test(triple.getLeft()))
					continue;

				if (triple.getRight()) {
					request.addStack(ItemHelper.cloneStack(triple.getLeft(), count));
					continue a;
				} else {
					int shrink = (int) Math.min(triple.getMiddle(), amount);
					request.addStack(ItemHelper.cloneStack(triple.getLeft(), shrink));

					copy.set(i, Triple.of(triple.getLeft(), triple.getMiddle() - shrink, triple.getRight()));
					amount -= shrink;
					if (amount == 0)
						continue a;
				}
			}

			missing.addStack(ItemHelper.cloneStack(item.getDisplayStack(), amount));
		}

		if (missing.stacks.isEmpty()) {
			for (ItemStack stack : tile.inventory.items)
				request.decreaseStack(stack);
			for (ItemStack stack : Minecraft.getMinecraft().player.inventory.mainInventory)
				request.decreaseStack(stack);

			if (request.stacks.isEmpty())
				return Pair.of(null, Collections.singletonList(StringHelper.localize("gui.logistics.terminal.enough")));

			return Pair.of(request.stacks, null);
		} else {
			List<String> tooltip = new LinkedList<>();

			tooltip.add(StringHelper.localize("gui.logistics.terminal.missing"));
			for (ItemStack stack : missing.stacks)
				tooltip.add(StringHelper.localizeFormat("info.logistics.manager.e.1", stack.getCount(), stack.getDisplayName()));

			return Pair.of(null, tooltip);
		}
	}

	@Override
	public void initGui() {
		super.initGui();

		addTab(tabCrafting = new TabCrafting(this, tile.shared, () -> {
			InventoryCrafting inventory = new InventoryCraftingFalse(3, 3);
			for (int i = 0; i < 9; i++)
				inventory.setInventorySlotContents(i, tile.shared[i].get());
			ItemStack stack = CraftingManager.findMatchingResult(inventory, Minecraft.getMinecraft().world);

			PacketTileInfo packet = PacketTileInfo.newPacket(tile);
			packet.addByte(2);
			packet.addBool(StringHelper.isShiftKeyDown());

			for (Shared.Item stacks : tile.shared) {
				Ingredient ingredient = stacks.asIngredient();
				packet.addInt(ingredient.getMatchingStacks().length);
				for (ItemStack item : ingredient.getMatchingStacks())
					packet.addItemStack(item);
			}

			packet.addItemStack(stack);
			PacketHandler.sendToServer(packet);
		}, () -> {
			if (cache.getLeft() != null)
				for (ItemStack stack : cache.getLeft())
					request(ItemHelper.cloneStack(stack, 1), stack.getCount());
		}, () -> cache.getLeft() != null)).setOffsets(-18, 74);
		addTab(tabRequest = new TabRequest(this, tile.requests.stacks, tile)).setOffsets(-18, 74);
	}

	@Override
	public void updateScreen() {
		super.updateScreen();

		cache = request();

		boolean visible = requester().getHasStack();
		tabCrafting.setVisible(visible);
		tabRequest.setVisible(visible);
	}

	@Override
	protected void mouseClicked(int mX, int mY, int mouseButton) throws IOException {
		if (button.isVisible() && !Minecraft.getMinecraft().player.inventory.getItemStack().isEmpty()) {
			int mouseX = mX - guiLeft - 7;
			int mouseY = mY - guiTop - 17;

			if (mouseX >= 0 && mouseX < 9 * 18 && mouseY >= 0 && mouseY < 3 * 18) {
				PacketTileInfo packet = PacketTileInfo.newPacket(tile);
				packet.addByte(3);
				PacketHandler.sendToServer(packet);
				return;
			}
		}

		super.mouseClicked(mX, mY, mouseButton);
	}

	@Override
	protected void mouseClickMove(int mX, int mY, int lastClick, long timeSinceClick) {
		if (lastClick == 0 && button.isVisible() && Arrays.stream(tabCrafting.grid).anyMatch(slot -> slot.intersectsWith(mX - guiLeft - tabCrafting.posX(), mY - guiTop - tabCrafting.getPosY()))) {
			try {
				mouseClicked(mX, mY, lastClick);
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			super.mouseClickMove(mX, mY, lastClick, timeSinceClick);
		}
	}

	@Override
	protected boolean hasClickedOutside(int x, int y, int left, int top) {
		boolean yes = super.hasClickedOutside(x, y, left, top);
		if (yes) {
			for (ElementBase element : tabCrafting.grid)
				if (element.intersectsWith(x - left - tabCrafting.posX(), y - top - tabCrafting.getPosY()))
					return false;
			if (tabCrafting.output.intersectsWith(x - left - tabCrafting.posX(), y - top - tabCrafting.getPosY()))
				return false;
		}
		return yes;
	}

	@Override
	protected void keyTyped(char characterTyped, int keyPressed) throws IOException {
		if (tabCrafting != null && tabCrafting.isFullyOpened() && tabCrafting.onKeyTyped(characterTyped, keyPressed))
			return;
		super.keyTyped(characterTyped, keyPressed);
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
			i1.getLeft().getItem().getSubItems(i1.getLeft().getItem().getCreativeTab(), list);
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
