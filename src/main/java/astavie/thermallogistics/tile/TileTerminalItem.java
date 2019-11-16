package astavie.thermallogistics.tile;

import astavie.thermallogistics.ThermalLogistics;
import astavie.thermallogistics.attachment.DistributorItem;
import astavie.thermallogistics.block.BlockTerminal;
import astavie.thermallogistics.client.gui.GuiTerminalItem;
import astavie.thermallogistics.container.ContainerTerminalItem;
import astavie.thermallogistics.process.ProcessItem;
import astavie.thermallogistics.process.Request;
import astavie.thermallogistics.process.Source;
import astavie.thermallogistics.util.Shared;
import astavie.thermallogistics.util.Snapshot;
import astavie.thermallogistics.util.collection.ItemList;
import astavie.thermallogistics.util.collection.StackList;
import astavie.thermallogistics.util.type.ItemType;
import codechicken.lib.inventory.InventorySimple;
import cofh.core.inventory.InventoryCraftingFalse;
import cofh.core.network.PacketBase;
import cofh.core.network.PacketHandler;
import cofh.core.util.helpers.InventoryHelper;
import cofh.core.util.helpers.ItemHelper;
import cofh.thermaldynamics.duct.attachments.servo.ServoItem;
import cofh.thermaldynamics.duct.item.DuctUnitItem;
import cofh.thermaldynamics.duct.item.GridItem;
import cofh.thermaldynamics.duct.item.TravelingItem;
import cofh.thermaldynamics.duct.tiles.DuctToken;
import cofh.thermaldynamics.duct.tiles.DuctUnit;
import cofh.thermaldynamics.duct.tiles.TileGrid;
import cofh.thermaldynamics.multiblock.MultiBlockGrid;
import cofh.thermaldynamics.multiblock.Route;
import cofh.thermaldynamics.util.ListWrapper;
import com.google.common.collect.Lists;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraftforge.items.wrapper.PlayerMainInvWrapper;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.stream.Stream;

public class TileTerminalItem extends TileTerminal<ItemStack> {

	public final Shared.Item[] shared = new Shared.Item[9];

	public final InventorySimple inventory = new InventorySimple(27);

	public TileTerminalItem() {
		process = new ProcessItem(this);
	}

	@Override
	protected void read(PacketBase packet, byte message, EntityPlayer player) {
		if (message == 2) {
			// CRAFT
			boolean shift = packet.getBool();

			Ingredient[] ingredients = new Ingredient[9];
			for (int i = 0; i < 9; i++) {
				ItemStack[] stacks = new ItemStack[packet.getInt()];
				for (int j = 0; j < stacks.length; j++)
					stacks[j] = packet.getItemStack();
				ingredients[i] = Ingredient.fromStacks(stacks);
			}

			IRecipe recipe = CraftingManager.getRecipe(new ResourceLocation(packet.getString()));
			if (recipe == null)
				return;

			ItemStack craft = recipe.getRecipeOutput().copy();
			ItemStack hand = player.inventory.getItemStack();
			if (!shift && !hand.isEmpty() && (!ItemHelper.itemsIdentical(craft, hand) || craft.getCount() + hand.getCount() > hand.getMaxStackSize()))
				return;

			b:
			//noinspection LoopConditionNotUpdatedInsideLoop
			do {
				// Get available items
				ItemList items = new ItemList();
				for (int slot = 0; slot < inventory.getSizeInventory(); slot++)
					if (!inventory.getStackInSlot(slot).isEmpty())
						items.add(inventory.getStackInSlot(slot));
				for (ItemStack stack : player.inventory.mainInventory)
					if (!stack.isEmpty())
						items.add(stack);

				// Check if those items are enough
				for (Ingredient ingredient : ingredients) {
					if (ingredient == Ingredient.EMPTY)
						continue;
					if (items.remove(ingredient))
						continue;

					break b;
				}

				InventoryCrafting inventory = new InventoryCraftingFalse(3, 3);

				// Craft item
				a:
				for (int i = 0, ingredientsLength = ingredients.length; i < ingredientsLength; i++) {
					Ingredient ingredient = ingredients[i];
					if (ingredient == Ingredient.EMPTY)
						continue;

					for (int slot = 0; slot < this.inventory.getSizeInventory(); slot++) {
						ItemStack stack = this.inventory.getStackInSlot(slot);
						if (ingredient.apply(stack)) {
							ItemStack decreased = this.inventory.decrStackSize(slot, 1);
							inventory.setInventorySlotContents(i, decreased);
							continue a;
						}
					}
					for (int slot = 0; slot < player.inventory.mainInventory.size(); slot++) {
						ItemStack stack = player.inventory.getStackInSlot(slot);
						if (ingredient.apply(stack)) {
							ItemStack decreased = player.inventory.decrStackSize(slot, 1);
							inventory.setInventorySlotContents(i, decreased);
							continue a;
						}
					}
				}

				craft.onCrafting(world, player, 1);
				FMLCommonHandler.instance().firePlayerCraftingEvent(player, craft, inventory);

				if (!recipe.isDynamic()) {
					player.unlockRecipes(Lists.newArrayList(recipe));
				}

				// Add containers
				ForgeHooks.setCraftingPlayer(player);
				NonNullList<ItemStack> ret = recipe.getRemainingItems(inventory);
				ForgeHooks.setCraftingPlayer(null);

				for (ItemStack stack : ret) {
					ItemStack item = InventoryHelper.insertStackIntoInventory(new InvWrapper(this.inventory), stack, false);
					if (!item.isEmpty())
						player.inventory.placeItemBackInInventory(world, item);
				}

				// Add item
				if (shift) {
					ItemStack item = InventoryHelper.insertStackIntoInventory(new InvWrapper(this.inventory), craft, false);
					if (!item.isEmpty())
						player.inventory.placeItemBackInInventory(world, item);
				} else {
					player.inventory.setItemStack(hand.isEmpty() ? craft : ItemHelper.cloneStack(hand, hand.getCount() + craft.getCount()));
					((EntityPlayerMP) player).updateHeldItem();
				}
			} while (shift);

			player.openContainer.detectAndSendChanges();
		} else if (message == 3) {
			// MANUAL DUMP
			if (requester.get().isEmpty())
				return;

			ItemStack hand = player.inventory.getItemStack();

			if (dump(hand)) {
				player.inventory.setItemStack(ItemStack.EMPTY);
				((EntityPlayerMP) player).updateHeldItem();
			}
		} else if (message == 4) {
			// DUMP ALL TO INVENTORY
			for (int i = 0; i < inventory.getSizeInventory(); i++) {
				ItemStack item = inventory.getStackInSlot(i);
				if (item.isEmpty())
					continue;

				item.setCount(InventoryHelper.insertStackIntoInventory(new PlayerMainInvWrapper(player.inventory), item.copy(), false).getCount());
			}
			player.openContainer.detectAndSendChanges();
		} else if (message == 5) {
			// DUMP ALL TO NETWORK
			if (requester.get().isEmpty())
				return;

			for (int i = 0; i < inventory.getSizeInventory(); i++) {
				ItemStack stack = inventory.getStackInSlot(i);
				if (!stack.isEmpty() && dump(stack))
					inventory.setInventorySlotContents(i, ItemStack.EMPTY);
			}
		}
	}

	@Override
	public DuctUnitItem getDuct(byte side) {
		TileEntity tile = world.getTileEntity(pos.offset(EnumFacing.byIndex(side)));
		if (tile instanceof TileGrid) {
			DuctUnitItem duct = ((TileGrid) tile).getDuct(DuctToken.ITEMS);
			if (duct != null && duct.isOutput(side ^ 1))
				return duct;
		}

		return null;
	}

	private ListWrapper<Route<DuctUnitItem, GridItem>> getRoutes(DuctUnitItem duct) {
		ListWrapper<Route<DuctUnitItem, GridItem>> routesWithInsertSideList = new ListWrapper<>();

		if (duct.getGrid() == null) {
			routesWithInsertSideList.setList(new LinkedList<>(), ListWrapper.SortType.NORMAL);
			return routesWithInsertSideList;
		}

		Stream<Route<DuctUnitItem, GridItem>> routesWithDestinations = ServoItem.getRoutesWithDestinations(duct.getCache().outputRoutes);
		LinkedList<Route<DuctUnitItem, GridItem>> objects = Lists.newLinkedList();
		routesWithDestinations.forEach(objects::add);

		routesWithInsertSideList.setList(objects, ListWrapper.SortType.NORMAL);

		return routesWithInsertSideList;
	}

	private boolean dump(ItemStack stack) {
		int type = requester.get().getMetadata();

		for (byte side = 0; side < 6; side++) {
			DuctUnitItem duct = getDuct(side);
			if (duct == null)
				continue;

			TravelingItem item = DistributorItem.findRouteForItem(stack, getRoutes(duct), duct, side ^ 1, ServoItem.range[type], ServoItem.speedBoost[type]);
			if (item == null)
				continue;

			duct.insertNewItem(item);
			return true;
		}

		return false;
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);

		NBTTagList slots = new NBTTagList();
		for (int i = 0; i < inventory.getSizeInventory(); i++) {
			ItemStack stack = inventory.getStackInSlot(i);
			if (!stack.isEmpty()) {
				NBTTagCompound item = stack.writeToNBT(new NBTTagCompound());
				item.setInteger("slot", i);
				slots.appendTag(item);
			}
		}

		nbt.setTag("inventory", slots);
		return nbt;
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);

		NBTTagList slots = nbt.getTagList("inventory", Constants.NBT.TAG_COMPOUND);
		for (int i = 0; i < slots.tagCount(); i++) {
			NBTTagCompound item = slots.getCompoundTagAt(i);
			inventory.setInventorySlotContents(item.getInteger("slot"), new ItemStack(item));
		}
	}

	@Override
	public Object getGuiClient(InventoryPlayer inventory) {
		return new GuiTerminalItem(this, inventory);
	}

	@Override
	public Object getGuiServer(InventoryPlayer inventory) {
		return new ContainerTerminalItem(this, inventory);
	}

	@Override
	protected StackList<ItemStack> createStackList() {
		return new ItemList();
	}

	@Override
	public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
		return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY && facing != world.getBlockState(pos).getValue(BlockTerminal.DIRECTION).getFace() || super.hasCapability(capability, facing);
	}

	@Nullable
	@Override
	public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
		if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY && facing != world.getBlockState(pos).getValue(BlockTerminal.DIRECTION).getFace())
			return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(new Inventory(this, inventory, (byte) facing.getIndex()));
		return super.getCapability(capability, facing);
	}

	@Override
	public String getTileName() {
		return ThermalLogistics.Blocks.terminal_item.getTranslationKey() + ".name";
	}

	@Override
	protected void updateTerminal() {
		Set<GridItem> grids = new HashSet<>();

		Set<IItemHandler> inventories = new HashSet<>();

		terminal.clear();
		for (byte side = 0; side < 6; side++) {
			DuctUnitItem duct = getDuct(side);
			if (duct == null || grids.contains(duct.getGrid()))
				continue;

			terminal.addAll(Snapshot.INSTANCE.getItems(duct.getGrid()));

			// Remove duplicate items
			for (IItemHandler handler : Snapshot.INSTANCE.getInventories(duct.getGrid())) {
				if (!inventories.add(handler)) {
					// Duplicate!
					for (int slot = 0; slot < handler.getSlots(); slot++) {
						ItemStack extract = handler.getStackInSlot(slot);
						if (extract.isEmpty())
							continue;
						terminal.remove(extract);
					}
				}
			}

			grids.add(duct.getGrid());
		}
	}

	@Override
	public ItemStack getTileIcon() {
		return new ItemStack(ThermalLogistics.Blocks.terminal_item);
	}

	@Override
	public StackList<ItemStack> getRequestedStacks(MultiBlockGrid<?> grid) {
		ItemList list = new ItemList();

		for (Request<ItemStack> request : requests) {
			if (request.isError() || request.source.isCrafter())
				continue;

			DuctUnitItem duct = getDuct(request.source.side);
			if (duct != null && duct.getGrid() == grid)
				list.add(request.type, request.amount);
		}

		return list;
	}

	@Override
	public ListWrapper<Pair<DuctUnit, Byte>> getSources(byte side) {
		ListWrapper<Pair<DuctUnit, Byte>> sources = new ListWrapper<>();

		DuctUnitItem duct = getDuct(side);

		LinkedList<Pair<DuctUnit, Byte>> list = new LinkedList<>();
		Stream<Route<DuctUnitItem, GridItem>> stream = ServoItem.getRoutesWithDestinations(duct.getCache().outputRoutes);
		stream.filter(r -> r.endPoint != duct || r.getLastSide() != (side ^ 1)).map(r -> Pair.of((DuctUnit) r.endPoint, r.getLastSide())).forEach(list::add);

		sources.setList(list, ListWrapper.SortType.NORMAL);
		return sources;
	}

	private static class Inventory extends InvWrapper {

		private final TileTerminalItem tile;
		private final byte side;

		private Inventory(TileTerminalItem tile, IInventory inv, byte side) {
			super(inv);
			this.tile = tile;
			this.side = side;
		}

		@Nonnull
		@Override
		public ItemStack extractItem(int slot, int amount, boolean simulate) {
			return ItemStack.EMPTY;
		}

		@Nonnull
		@Override
		public ItemStack getStackInSlot(int slot) {
			return ItemStack.EMPTY;
		}

		@Nonnull
		@Override
		public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
			ItemType type = new ItemType(stack);

			ItemStack insert = ItemHelper.cloneStack(stack, (int) Math.min(stack.getCount(), tile.amountRequested(new Source<>(side), type)));
			if (insert.isEmpty())
				return stack;

			ItemStack remainder = super.insertItem(slot, insert, simulate);
			if (!simulate) {
				int amount = insert.getCount() - remainder.getCount();
				if (amount > 0) {
					tile.removeRequested(new Source<>(side), type, amount);
					PacketHandler.sendToAllAround(tile.getSyncPacket(), tile);
				}
			}

			return ItemHelper.cloneStack(stack, stack.getCount() - insert.getCount() + remainder.getCount());
		}

	}

}
