package astavie.thermallogistics.tile;

import astavie.thermallogistics.ThermalLogistics;
import astavie.thermallogistics.attachment.ICrafter;
import astavie.thermallogistics.block.BlockTerminal;
import astavie.thermallogistics.client.gui.GuiTerminalItem;
import astavie.thermallogistics.container.ContainerTerminalItem;
import astavie.thermallogistics.process.Process;
import astavie.thermallogistics.process.ProcessItem;
import astavie.thermallogistics.process.Request;
import astavie.thermallogistics.process.RequestItem;
import astavie.thermallogistics.util.StackHandler;
import codechicken.lib.inventory.InventorySimple;
import cofh.core.network.PacketBase;
import cofh.core.util.helpers.ItemHelper;
import cofh.thermaldynamics.duct.Attachment;
import cofh.thermaldynamics.duct.item.DuctUnitItem;
import cofh.thermaldynamics.duct.item.GridItem;
import cofh.thermaldynamics.duct.item.StackMap;
import cofh.thermaldynamics.duct.tiles.DuctToken;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import org.apache.commons.lang3.tuple.Triple;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TileTerminalItem extends TileTerminal<ItemStack> {

	public final InventorySimple inventory = new InventorySimple(27);

	public final RequestItem requests = new RequestItem(null);

	@Override
	protected void sync(PacketBase packet) {
		packet.addInt(requests.stacks.size());
		for (ItemStack stack : requests.stacks)
			packet.addItemStack(stack);
	}

	@Override
	protected void read(PacketBase packet) {
		requests.stacks.clear();
		int size = packet.getInt();
		for (int i = 0; i < size; i++)
			requests.stacks.add(packet.getItemStack());
	}

	@Override
	protected void read(PacketBase packet, byte message) {
		if (message == 1) {
			int index = packet.getInt();
			if (index < requests.stacks.size()) {
				requests.stacks.remove(index);

				Set<ItemStack> set = new HashSet<>();

				a:
				for (Requester requester : processes) {
					for (Object object : requester.process.getStacks()) {
						ItemStack stack = (ItemStack) object;
						for (ItemStack compare : set) {
							if (ItemHelper.itemsIdentical(stack, compare)) {
								compare.grow(stack.getCount());
								continue a;
							}
						}
						set.add(stack.copy());
					}
				}

				Map<ItemStack, Integer> map = set.stream().collect(Collectors.toMap(Function.identity(), item -> Math.max(item.getCount() - requests.getCount(item), 0)));
				map.entrySet().removeIf(e -> e.getValue() == 0);

				for (Requester requester : processes) {
					for (@SuppressWarnings("unchecked") Iterator<Request<ItemStack>> iterator = requester.process.requests.iterator(); iterator.hasNext() && !map.isEmpty(); ) {
						Request<ItemStack> request = iterator.next();

						for (Iterator<ItemStack> iterator1 = request.stacks.iterator(); iterator1.hasNext() && !map.isEmpty(); ) {
							ItemStack stack = iterator1.next();

							for (Iterator<Map.Entry<ItemStack, Integer>> iterator2 = map.entrySet().iterator(); iterator2.hasNext(); ) {
								Map.Entry<ItemStack, Integer> entry = iterator2.next();
								if (!ItemHelper.itemsIdentical(entry.getKey(), stack))
									continue;

								int shrink = Math.min(stack.getCount(), entry.getValue());
								stack.shrink(shrink);
								entry.setValue(entry.getValue() - shrink);

								if (stack.isEmpty())
									iterator1.remove();
								if (entry.getValue() == 0)
									iterator2.remove();

								break;
							}
						}

						if (request.stacks.isEmpty())
							iterator.remove();
					}
				}

				markChunkDirty();
			}
		}
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

		NBTTagList requests = new NBTTagList();
		for (ItemStack stack : this.requests.stacks)
			requests.appendTag(stack.writeToNBT(new NBTTagCompound()));

		nbt.setTag("inventory", slots);
		nbt.setTag("requests", requests);
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

		NBTTagList requests = nbt.getTagList("requests", Constants.NBT.TAG_COMPOUND);
		for (int i = 0; i < requests.tagCount(); i++)
			this.requests.stacks.add(new ItemStack(requests.getCompoundTagAt(i)));
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
	public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
		return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY && facing != world.getBlockState(pos).getValue(BlockTerminal.DIRECTION).getFace() || super.hasCapability(capability, facing);
	}

	@Nullable
	@Override
	public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
		if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY && facing != world.getBlockState(pos).getValue(BlockTerminal.DIRECTION).getFace())
			return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(new Inventory(this, inventory));
		return super.getCapability(capability, facing);
	}

	@Override
	public String getTileName() {
		return ThermalLogistics.Blocks.terminal_item.getTranslationKey() + ".name";
	}

	@Override
	protected DuctToken<?, ?, ?> getDuctToken() {
		return DuctToken.ITEMS;
	}

	@Override
	protected Process<ItemStack> createProcess(Requester<ItemStack> requester) {
		return new ProcessItem(requester);
	}

	@Override
	protected int amountRequired(ItemStack stack) {
		int required = requests.getCount(stack);
		if (required == 0)
			return 0;

		for (Requester request : processes) {
			DuctUnitItem duct = (DuctUnitItem) request.getDuct();
			if (duct == null)
				continue;

			//noinspection unchecked
			Process<ItemStack> process = request.process;

			// Items in requests
			for (ItemStack item : process.getStacks())
				if (ItemHelper.itemsIdentical(item, stack))
					required -= item.getCount();

			// Items traveling
			StackMap map = duct.getGrid().travelingItems.get(pos);
			if (map != null)
				for (ItemStack item : map.getItems())
					if (ItemHelper.itemsIdentical(item, stack))
						required -= item.getCount();
		}

		return required;
	}

	@Override
	protected void request(PacketBase payload) {
		requests.addStack(ItemHelper.cloneStack((ItemStack) StackHandler.readPacket(payload), payload.getInt()));
	}

	@Override
	protected void updateTerminal() {
		terminal.clear();

		Set<GridItem> grids = new HashSet<>();
		Set<IItemHandler> handlers = new HashSet<>();

		for (Requester requester : processes) {
			DuctUnitItem duct = (DuctUnitItem) requester.getDuct();
			if (duct == null || grids.contains(duct.getGrid()))
				continue;

			for (DuctUnitItem start : duct.getGrid().nodeSet) {
				for (byte side = 0; side < 6; side++) {
					if (start == duct && side == (requester.side ^ 1))
						continue;

					if ((!start.isInput(side) && !start.isOutput(side)) || !start.parent.getConnectionType(side).allowTransfer)
						continue;

					Attachment attachment = start.parent.getAttachment(side);
					if (attachment != null) {
						if (attachment instanceof ICrafter) {
							a:
							//noinspection unchecked
							for (ItemStack out : ((ICrafter<ItemStack>) attachment).getOutputs()) {
								if (out.isEmpty())
									continue;
								for (int i = 0; i < terminal.size(); i++) {
									Triple<ItemStack, Long, Boolean> stack = terminal.get(i);
									if (!ItemHelper.itemsIdentical(out, stack.getLeft()))
										continue;
									if (!stack.getRight())
										terminal.set(i, Triple.of(stack.getLeft(), stack.getMiddle(), true));
									continue a;
								}
								terminal.add(Triple.of(ItemHelper.cloneStack(out, 1), 0L, true));
							}
						}
						if (!attachment.canSend())
							continue;
					}

					DuctUnitItem.Cache cache = start.tileCache[side];
					if (cache == null)
						continue;

					if (cache.tile != null && cache.tile instanceof ICrafter) {
						a:
						//noinspection unchecked
						for (ItemStack out : ((ICrafter<ItemStack>) cache.tile).getOutputs()) {
							if (out.isEmpty())
								continue;
							for (int i = 0; i < terminal.size(); i++) {
								Triple<ItemStack, Long, Boolean> stack = terminal.get(i);
								if (!ItemHelper.itemsIdentical(out, stack.getLeft()))
									continue;
								if (!stack.getRight())
									terminal.set(i, Triple.of(stack.getLeft(), stack.getMiddle(), true));
								continue a;
							}
							terminal.add(Triple.of(ItemHelper.cloneStack(out, 1), 0L, true));
						}
					}

					IItemHandler inv = cache.getItemHandler(side ^ 1);
					if (inv == null || handlers.contains(inv))
						continue;

					a:
					for (int slot = 0; slot < inv.getSlots(); slot++) {
						ItemStack extract = inv.getStackInSlot(slot);
						if (extract.isEmpty())
							continue;

						for (int i = 0; i < terminal.size(); i++) {
							Triple<ItemStack, Long, Boolean> stack = terminal.get(i);
							if (!ItemHelper.itemsIdentical(extract, stack.getLeft()))
								continue;
							terminal.set(i, Triple.of(stack.getLeft(), stack.getMiddle() + extract.getCount(), stack.getRight()));
							continue a;
						}
						terminal.add(Triple.of(ItemHelper.cloneStack(extract, 1), (long) extract.getCount(), false));
					}

					handlers.add(inv);
				}
			}

			grids.add(duct.getGrid());
		}
	}

	@Override
	public Class<ItemStack> getItemClass() {
		return ItemStack.class;
	}

	private static class Inventory extends InvWrapper {

		private final TileTerminalItem tile;

		private Inventory(TileTerminalItem tile, IInventory inv) {
			super(inv);
			this.tile = tile;
		}

		@Nonnull
		@Override
		public ItemStack extractItem(int slot, int amount, boolean simulate) {
			return ItemStack.EMPTY;
		}

		@Nonnull
		@Override
		public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
			ItemStack insert = ItemHelper.cloneStack(stack, tile.requests.getCount(stack));
			if (insert.isEmpty())
				return stack;

			ItemStack remainder = super.insertItem(slot, insert, simulate);
			if (!simulate)
				tile.requests.decreaseStack(ItemHelper.cloneStack(insert, insert.getCount() - remainder.getCount()));

			return ItemHelper.cloneStack(stack, stack.getCount() - insert.getCount() + remainder.getCount());
		}

	}

}
