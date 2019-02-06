package astavie.thermallogistics.tile;

import astavie.thermallogistics.event.EventHandler;
import astavie.thermallogistics.gui.client.GuiTerminalItem;
import astavie.thermallogistics.gui.container.ContainerTerminal;
import astavie.thermallogistics.gui.container.ContainerTerminalItem;
import astavie.thermallogistics.process.ProcessItem;
import astavie.thermallogistics.util.NetworkUtils;
import astavie.thermallogistics.util.delegate.DelegateClientItem;
import astavie.thermallogistics.util.delegate.DelegateItem;
import codechicken.lib.inventory.InventorySimple;
import cofh.core.network.PacketBase;
import cofh.core.network.PacketHandler;
import cofh.core.network.PacketTileInfo;
import cofh.core.util.helpers.ItemHelper;
import cofh.thermaldynamics.duct.attachments.servo.ServoItem;
import cofh.thermaldynamics.duct.item.DuctUnitItem;
import cofh.thermaldynamics.duct.item.GridItem;
import cofh.thermaldynamics.duct.item.TravelingItem;
import cofh.thermaldynamics.duct.tiles.DuctToken;
import cofh.thermaldynamics.multiblock.Route;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.items.CapabilityItemHandler;
import org.apache.commons.lang3.tuple.Triple;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

public class TileTerminalItem extends TileTerminal<ProcessItem, DuctUnitItem, ItemStack> {

	public final InventorySimple inventory = new InventorySimple(27);

	public NonNullList<Triple<ItemStack, Long, Boolean>> terminal;

	private boolean refreshed = false;

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		nbt = super.writeToNBT(nbt);

		NBTTagList slots = new NBTTagList();
		for (int i = 0; i < inventory.getSizeInventory(); i++) {
			ItemStack stack = inventory.getStackInSlot(i);
			if (!stack.isEmpty()) {
				NBTTagCompound item = stack.writeToNBT(new NBTTagCompound());
				item.setInteger("Slot", i);
				slots.appendTag(item);
			}
		}

		nbt.setTag("Inventory", slots);
		return nbt;
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);

		NBTTagList slots = nbt.getTagList("Inventory", Constants.NBT.TAG_COMPOUND);
		for (int i = 0; i < slots.tagCount(); i++) {
			NBTTagCompound item = slots.getCompoundTagAt(i);
			inventory.setInventorySlotContents(item.getInteger("Slot"), new ItemStack(item));
		}
	}

	@Override
	protected DuctToken<?, ?, ?> getDuctToken() {
		return DuctToken.ITEMS;
	}

	@Override
	public void handlePacket(PacketBase payload, byte message, boolean isServer, EntityPlayer player) {
		if (isServer) {
			switch (message) {
				case 1:
					// Send list of items
					PacketBase p = PacketTileInfo.newPacket(this).addByte(1);
					GridItem grid = getGrid(DuctToken.ITEMS);
					if (grid != null) {
						if (!refreshed) {
							terminal = NetworkUtils.getItems(grid);
							refreshed = true;
						}
						p.addInt(terminal.size());
						for (Triple<ItemStack, Long, Boolean> stack : terminal) {
							try {
								p.addShort(Item.getIdFromItem(stack.getLeft().getItem()));
								p.addLong(stack.getMiddle());
								p.addShort(ItemHelper.getItemDamage(stack.getLeft()));
								p.writeNBT(stack.getLeft().getTagCompound());
								p.addBool(stack.getRight());
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
						PacketHandler.sendTo(p, player);
					} else onNeighborBlockChange();
					break;
				case 2:
					// New request
					ItemStack stack = ItemStack.EMPTY;
					try {
						short itemID = payload.getShort();
						int stackSize = payload.getInt();
						short damage = payload.getShort();
						stack = new ItemStack(Item.getItemById(itemID), stackSize, damage);
						stack.setTagCompound(payload.readNBT());
					} catch (IOException e) {
						e.printStackTrace();
					}
					new ProcessItem(null, this, stack, 1);
					break;
				case 3:
					// Send item into network
					DuctUnitItem duct = getDuct();

					TravelingItem item = ServoItem.findRouteForItem(player.inventory.getItemStack(), NetworkUtils.getRoutes(duct, getSide()).iterator(), duct, getSide(), ServoItem.range[getType()], ServoItem.speedBoost[getType()]);
					if (item == null) {
						Route route = new Route<>(duct);
						route.endPoint = null;
						route.pathDirections.add((byte) this.duct.getIndex());

						item = new TravelingItem(player.inventory.getItemStack(), duct, route, (byte) this.duct.getIndex(), ServoItem.speedBoost[getType()]);
						item.reRoute = true;
					}

					duct.insertNewItem(item);
					player.inventory.setItemStack(ItemStack.EMPTY);
					break;
			}
		} else if (message == 1) {
			terminal = NonNullList.create();
			int size = payload.getInt();
			for (int i = 0; i < size; i++) {
				ItemStack stack = ItemStack.EMPTY;
				short itemID = payload.getShort();
				long stackSize = payload.getLong();

				try {
					short damage = payload.getShort();
					stack = new ItemStack(Item.getItemById(itemID), 1, damage);
					stack.setTagCompound(payload.readNBT());
				} catch (IOException e) {
					e.printStackTrace();
				}

				boolean craft = payload.getBool();
				terminal.add(Triple.of(stack, stackSize, craft));
			}
		}
	}

	@Override
	public void update() {
		super.update();
		refreshed = false;
	}

	@Override
	public Object getGuiClient(InventoryPlayer inventory) {
		return new GuiTerminalItem(getGuiServer(inventory));
	}

	@Override
	public ContainerTerminal getGuiServer(InventoryPlayer inventory) {
		return new ContainerTerminalItem(this, inventory);
	}

	@Override
	public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
		return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY && facing == duct || super.hasCapability(capability, facing);
	}

	@Nullable
	@Override
	@SuppressWarnings("unchecked")
	public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
		if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY && facing == duct)
			return (T) new InvWrapperInsert(inventory);
		return super.getCapability(capability, facing);
	}

	@Override
	public int amountRequired(ProcessItem process, ItemStack item) {
		return ItemHelper.itemsIdentical(item, process.getOutput()) ? process.getOutput().getCount() : 0;
	}

	@Override
	public boolean itemsIdentical(ItemStack one, ItemStack two) {
		return ItemHelper.itemsIdentical(one, two);
	}

	@Override
	public Collection<ItemStack> getInputs(ProcessItem process) {
		return Collections.singleton(process.getOutput());
	}

	@Override
	public DelegateItem getDelegate() {
		return DelegateItem.INSTANCE;
	}

	@Override
	public DelegateClientItem getClientDelegate() {
		return DelegateClientItem.INSTANCE;
	}

	@Override
	public boolean isTick() {
		return EventHandler.time % ServoItem.tickDelays[getType()] == 0;
	}

}
