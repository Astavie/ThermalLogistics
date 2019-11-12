package astavie.thermallogistics.client.gui;

import astavie.thermallogistics.ThermalLogistics;
import astavie.thermallogistics.client.gui.tab.TabCrafting;
import astavie.thermallogistics.client.gui.tab.TabRequest;
import astavie.thermallogistics.container.ContainerTerminalItem;
import astavie.thermallogistics.tile.TileTerminalItem;
import astavie.thermallogistics.util.Shared;
import astavie.thermallogistics.util.collection.ItemList;
import astavie.thermallogistics.util.type.Type;
import cofh.core.gui.element.ElementBase;
import cofh.core.gui.element.ElementButton;
import cofh.core.inventory.InventoryCraftingFalse;
import cofh.core.network.PacketHandler;
import cofh.core.network.PacketTileInfo;
import cofh.core.util.helpers.ItemHelper;
import cofh.core.util.helpers.StringHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
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

	private Pair<ItemList, List<String>> cache = Pair.of(null, null);
	private ElementButton dump2;

	public GuiTerminalItem(TileTerminalItem tile, InventoryPlayer inventory) {
		super(tile, new ContainerTerminalItem(tile, inventory), new ResourceLocation(ThermalLogistics.MOD_ID, "textures/gui/terminal.png"));
		((ContainerTerminalItem) inventorySlots).gui = this;
		this.tile = tile;
		this.xSize = 194;
		this.ySize = 238;
		this.size = 238;
	}

	public Object getStackAt(int mouseX, int mouseY) {
		if (selected != null && button.isVisible() && mouseX >= 25 && mouseX < 43 && mouseY >= 73 && mouseY < 91)
			return selected.getAsStack();

		int i = slider.getValue() * 9;

		a:
		for (int y = 0; y < rows; y++) {
			for (int x = 0; x < 9; x++) {
				int slot = i + x + y * 9;
				if (slot >= filter.size())
					break a;

				int posX = 8 + x * 18;
				int posY = 18 + y * 18;

				if (mouseX >= posX - 1 && mouseX < posX + 17 && mouseY >= posY - 1 && mouseY < posY + 17)
					return filter.get(slot).getLeft();
			}
		}

		if (tabCrafting.isFullyOpened())
			return tabCrafting.getStackAt(mouseX, mouseY);

		return null;
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

	private Pair<ItemList, List<String>> request() {
		if (Arrays.stream(tile.shared).allMatch(shared -> shared.test(ItemStack.EMPTY)))
			return Pair.of(null, null);

		ItemList copy = new ItemList();

		for (ItemStack stack : tile.inventory.items)
			if (!stack.isEmpty())
				copy.add(stack);
		for (ItemStack stack : Minecraft.getMinecraft().player.inventory.mainInventory)
			if (!stack.isEmpty())
				copy.add(stack);

		copy.addAll(tile.terminal);

		int count = 1;
		if (!tabCrafting.amount.getText().isEmpty())
			count = Integer.parseInt(tabCrafting.amount.getText());

		ItemList request = new ItemList();
		ItemList missing = new ItemList();

		a:
		for (Shared.Item item : tile.shared) {
			if (item.test(ItemStack.EMPTY))
				continue;

			int amount = count;

			for (Type<ItemStack> type : copy.types()) {
				boolean craftable = copy.craftable(type);
				long size = copy.amount(type);

				if (!craftable && size == 0L)
					continue;

				if (!item.test(type))
					continue;

				if (craftable) {
					request.add(type.withAmount(count));
					continue a;
				} else {
					int shrink = (int) Math.min(size, amount);

					ItemStack stack = type.withAmount(shrink);
					request.add(stack);
					copy.remove(stack);

					amount -= shrink;
					if (amount == 0)
						continue a;
				}
			}

			missing.add(ItemHelper.cloneStack(item.getDisplayStack(), amount));
		}

		if (missing.isEmpty()) {
			for (ItemStack stack : tile.inventory.items)
				request.remove(stack);
			for (ItemStack stack : Minecraft.getMinecraft().player.inventory.mainInventory)
				request.remove(stack);

			if (request.isEmpty())
				return Pair.of(null, Collections.singletonList(StringHelper.localize("gui.logistics.terminal.enough")));

			return Pair.of(request, null);
		} else {
			List<String> tooltip = new LinkedList<>();

			tooltip.add(StringHelper.localize("gui.logistics.terminal.missing"));
			for (Type<ItemStack> type : missing.types())
				tooltip.add(StringHelper.localizeFormat("info.logistics.manager.e.1", missing.amount(type), type.getDisplayName()));

			return Pair.of(null, tooltip);
		}
	}

	@Override
	public void initGui() {
		super.initGui();

		guiLeft += 9;

		ElementButton dump = new ElementButton(this, 153, 141, "dump", 194, 0, 194, 14, 14, 14, texture.toString());
		dump.setToolTip("info.logistics.terminal.dump.inventory");

		dump2 = new ElementButton(this, 153, 73, "dump2", 208, 0, 208, 14, 14, 14, texture.toString());
		dump2.setToolTip("info.logistics.terminal.dump.network");

		addElement(dump);
		addElement(dump2);

		addTab(tabCrafting = new TabCrafting(this, tile.shared, () -> {
			InventoryCrafting inventory = new InventoryCraftingFalse(3, 3);
			for (int i = 0; i < 9; i++)
				inventory.setInventorySlotContents(i, tile.shared[i].get());

			IRecipe recipe = CraftingManager.findMatchingRecipe(inventory, Minecraft.getMinecraft().world);
			if (recipe == null) // Shouldn't happen but apparently it does :|
				return;

			PacketTileInfo packet = PacketTileInfo.newPacket(tile);
			packet.addByte(2);
			packet.addBool(StringHelper.isShiftKeyDown());

			for (Shared.Item stacks : tile.shared) {
				Ingredient ingredient = stacks.asIngredient();
				packet.addInt(ingredient.getMatchingStacks().length);
				for (ItemStack item : ingredient.getMatchingStacks())
					packet.addItemStack(item);
			}

			packet.addString(recipe.getRegistryName().toString());
			PacketHandler.sendToServer(packet);
		}, () -> {
			if (cache.getLeft() != null)
				for (Type<ItemStack> type : cache.getLeft().types())
					request(type, cache.getLeft().amount(type));
		}, () -> cache.getLeft() != null));
		addTab(tabRequest = new TabRequest(this, tile.requests.stacks(), tile));
	}

	@Override
	protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
		super.drawGuiContainerForegroundLayer(mouseX, mouseY);
		fontRenderer.drawString(StringHelper.localize("gui.logistics.terminal.buffer"), 8, ySize - 96 - 68 + 3, 0x404040);
	}

	@Override
	public void handleElementButtonClick(String buttonName, int mouseButton) {
		if (mouseButton == 0) {
			if (buttonName.equals("dump")) {
				playClickSound(1F);

				PacketTileInfo packet = PacketTileInfo.newPacket(tile);
				packet.addByte(4);
				PacketHandler.sendToServer(packet);
			} else if (buttonName.equals("dump2")) {
				playClickSound(1F);

				PacketTileInfo packet = PacketTileInfo.newPacket(tile);
				packet.addByte(5);
				PacketHandler.sendToServer(packet);
			}
		}
	}

	@Override
	public void updateScreen() {
		super.updateScreen();

		dump2.setVisible(requester().getHasStack());

		cache = request();

		boolean visible = requester().getHasStack();
		tabCrafting.setVisible(visible);
		tabRequest.setVisible(visible);
	}

	@Override
	protected int getTabOffsetX() {
		return -18;
	}

	@Override
	protected int getTabOffsetY() {
		return 56;
	}

	@Override
	protected void mouseClicked(int mX, int mY, int mouseButton) throws IOException {
		if (button.isVisible() && !Minecraft.getMinecraft().player.inventory.getItemStack().isEmpty()) {
			int mouseX = mX - guiLeft - 7;
			int mouseY = mY - guiTop - 17;

			if (mouseX >= 0 && mouseX < 9 * 18 && mouseY >= 0 && mouseY < rows * 18) {
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
	protected boolean isSelected(Type<ItemStack> type) {
		return type.equals(selected);
	}

	@Override
	protected void updateFilter() {
		filter.clear();
		for (Type<ItemStack> type : tile.terminal.types()) {
			if (search.getText().isEmpty() || type.getDisplayName().toLowerCase().contains(search.getText().toLowerCase())) {
				filter.add(Triple.of(type, tile.terminal.amount(type), tile.terminal.craftable(type)));
			} else for (String string : getItemToolTip(type.getAsStack())) {
				if (string.toLowerCase().contains(search.getText().toLowerCase())) {
					filter.add(Triple.of(type, tile.terminal.amount(type), tile.terminal.craftable(type)));
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
			int id = Integer.compare(Item.getIdFromItem(i1.getLeft().getAsStack().getItem()), Item.getIdFromItem(i2.getLeft().getAsStack().getItem()));
			if (id != 0)
				return id;

			// Then compare item damage
			int damage = Integer.compare(i1.getLeft().getAsStack().getItemDamage(), i2.getLeft().getAsStack().getItemDamage());
			if (damage != 0)
				return damage;

			// Then compare sub items
			CreativeTabs tab = i1.getLeft().getAsStack().getItem().getCreativeTab();
			if (tab == null)
				tab = CreativeTabs.SEARCH;

			NonNullList<ItemStack> list = NonNullList.create();
			i1.getLeft().getAsStack().getItem().getSubItems(tab, list);
			for (ItemStack stack : list) {
				if (ItemHelper.itemsIdentical(i1.getLeft().getAsStack(), stack))
					return -1;
				if (ItemHelper.itemsIdentical(i2.getLeft().getAsStack(), stack))
					return 1;
			}

			// Then compare nbt data
			if (i1.getLeft().getAsStack().getTagCompound() == null && i2.getLeft().getAsStack().getTagCompound() != null)
				return -1;
			if (i1.getLeft().getAsStack().getTagCompound() != null && i2.getLeft().getAsStack().getTagCompound() == null)
				return 1;

			// Then we give up
			return 0;
		});
	}

	@Override
	protected void updateAmount(Triple<Type<ItemStack>, Long, Boolean> stack) {
		amount.setText(Long.toString(stack.getRight() ? selected.getAsStack().getMaxStackSize() : Math.min(stack.getMiddle(), selected.getAsStack().getMaxStackSize())));
	}

}
