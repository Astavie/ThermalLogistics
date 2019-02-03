package astavie.thermallogistics.attachment;

import astavie.thermallogistics.ThermalLogistics;
import astavie.thermallogistics.compat.ICrafterWrapper;
import astavie.thermallogistics.event.EventHandler;
import astavie.thermallogistics.process.ProcessItem;
import astavie.thermallogistics.util.IProcessLoader;
import astavie.thermallogistics.util.IRequester;
import astavie.thermallogistics.util.NetworkUtils;
import astavie.thermallogistics.util.delegate.DelegateClientItem;
import astavie.thermallogistics.util.delegate.DelegateItem;
import cofh.core.network.PacketBase;
import cofh.core.network.PacketHandler;
import cofh.core.util.helpers.ItemHelper;
import cofh.thermaldynamics.duct.attachments.filter.IFilterFluid;
import cofh.thermaldynamics.duct.attachments.filter.IFilterItems;
import cofh.thermaldynamics.duct.attachments.servo.ServoItem;
import cofh.thermaldynamics.duct.item.DuctUnitItem;
import cofh.thermaldynamics.duct.item.TravelingItem;
import cofh.thermaldynamics.duct.tiles.DuctToken;
import cofh.thermaldynamics.duct.tiles.TileGrid;
import cofh.thermaldynamics.multiblock.Route;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.oredict.OreDictionary;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

public class CrafterItem extends Crafter<ProcessItem, DuctUnitItem, ItemStack> implements IProcessLoader {

	public static final ResourceLocation ID = new ResourceLocation(ThermalLogistics.MODID, "crafter_item");

	public final List<ItemStack> leftovers = new LinkedList<>();
	public final List<Pair<ItemStack, IRequester<DuctUnitItem, ItemStack>>> registry = new LinkedList<>();

	private final IFilterItems filter = new FilterItem();
	public ItemStack[] inputs;
	public ItemStack[] outputs;

	private NBTTagList _registry;

	public CrafterItem(TileGrid tile, byte side) {
		super(tile, side);
	}

	public CrafterItem(TileGrid tile, byte side, int type) {
		super(tile, side, type);

		int max = type + 1;
		inputs = new ItemStack[max * 2];
		outputs = new ItemStack[max];
		Arrays.fill(inputs, ItemStack.EMPTY);
		Arrays.fill(outputs, ItemStack.EMPTY);
	}

	@Override
	public void readFromNBT(NBTTagCompound tag) {
		super.readFromNBT(tag);

		NBTTagList leftovers = tag.getTagList("leftovers", Constants.NBT.TAG_COMPOUND);
		for (int i = 0; i < leftovers.tagCount(); i++)
			this.leftovers.add(new ItemStack(leftovers.getCompoundTagAt(i)));

		_registry = tag.getTagList("registry", Constants.NBT.TAG_COMPOUND);
		EventHandler.LOADERS.add(this);
	}

	@Override
	public void writeToNBT(NBTTagCompound tag) {
		super.writeToNBT(tag);

		NBTTagList leftovers = new NBTTagList();
		for (ItemStack stack : this.leftovers)
			leftovers.appendTag(stack.writeToNBT(new NBTTagCompound()));

		NBTTagList registry = new NBTTagList();
		for (Pair<ItemStack, IRequester<DuctUnitItem, ItemStack>> pair : this.registry) {
			NBTTagCompound nbt = new NBTTagCompound();
			if (pair.getLeft() != null)
				nbt.setTag("item", pair.getLeft().writeToNBT(new NBTTagCompound()));
			nbt.setTag("destination", IRequester.writeNbt(pair.getRight(), true));
			registry.appendTag(nbt);
		}

		tag.setTag("leftovers", leftovers);
		tag.setTag("registry", registry);
	}

	@Override
	protected void writeRecipe(NBTTagCompound tag) {
		NBTTagList inputs = new NBTTagList();
		for (int i = 0; i < this.inputs.length; i++) {
			if (this.inputs[i] != null) {
				NBTTagCompound compound = new NBTTagCompound();
				compound.setInteger("Slot", i);
				this.inputs[i].writeToNBT(compound);
				inputs.appendTag(compound);
			}
		}

		NBTTagList outputs = new NBTTagList();
		for (int i = 0; i < this.outputs.length; i++) {
			if (this.outputs[i] != null) {
				NBTTagCompound compound = new NBTTagCompound();
				compound.setInteger("Slot", i);
				this.outputs[i].writeToNBT(compound);
				outputs.appendTag(compound);
			}
		}

		tag.setTag("Inputs", inputs);
		tag.setTag("Outputs", outputs);
	}

	@Override
	protected void readRecipe(NBTTagCompound tag) {
		int max = type + 1;
		inputs = new ItemStack[max * 2];
		outputs = new ItemStack[max];
		Arrays.fill(inputs, ItemStack.EMPTY);
		Arrays.fill(outputs, ItemStack.EMPTY);

		NBTTagList inputs = tag.getTagList("Inputs", Constants.NBT.TAG_COMPOUND);
		for (int i = 0; i < inputs.tagCount(); i++) {
			NBTTagCompound compound = inputs.getCompoundTagAt(i);
			this.inputs[compound.getInteger("Slot")] = new ItemStack(compound);
		}

		NBTTagList outputs = tag.getTagList("Outputs", Constants.NBT.TAG_COMPOUND);
		for (int i = 0; i < outputs.tagCount(); i++) {
			NBTTagCompound compound = outputs.getCompoundTagAt(i);
			this.outputs[compound.getInteger("Slot")] = new ItemStack(compound);
		}
	}

	@Override
	public void postLoad() {
		if (_registry != null) {
			for (int i = 0; i < _registry.tagCount(); i++) {
				NBTTagCompound tag = _registry.getCompoundTagAt(i);
				NBTTagCompound item = tag.getCompoundTag("item");
				NBTTagCompound destination = tag.getCompoundTag("destination");

				this.registry.add(Pair.of(item.isEmpty() ? null : new ItemStack(item), IRequester.readNbt(baseTile.world(), destination)));
			}
			_registry = null;
		}
		super.postLoad();
	}

	@Override
	public void addProcess(ProcessItem process, int index) {
		super.addProcess(process, index);
		if (!process.getOutput().isEmpty())
			registry.add(Pair.of(null, process));
	}

	@Override
	public void removeProcess(ProcessItem process) {
		super.removeProcess(process);
		for (Iterator<Pair<ItemStack, IRequester<DuctUnitItem, ItemStack>>> iterator = registry.iterator(); iterator.hasNext(); )
			if (iterator.next().getRight() == process)
				iterator.remove();
	}

	@Override
	public Crafter.Cache createCache() {
		return new Cache();
	}

	@Override
	public ProcessItem createLinkedProcess(int sum) {
		return new ProcessItem(null, this, ItemStack.EMPTY, sum);
	}

	@Override
	public int amountRequired(ItemStack item) {
		int amt = 0;
		for (ItemStack input : inputs)
			if (itemsIdentical(item, input))
				amt += input.getCount();
		return amt;
	}

	@Override
	public boolean itemsIdentical(ItemStack a, ItemStack b) {
		return !values[4] && a.getItem().getRegistryName().getNamespace().equals(b.getItem().getRegistryName().getNamespace()) || !values[3] && !Collections.disjoint(Collections.singletonList(OreDictionary.getOreIDs(a)), Collections.singletonList(OreDictionary.getOreIDs(b))) || a.getItem() == b.getItem() && (values[1] || a.getItemDamage() == b.getItemDamage()) && (values[2] || ItemStack.areItemStackTagsEqual(a, b));
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
	public ItemStack[] getInputs() {
		return inputs;
	}

	@Override
	public Collection<ItemStack> getInputs(ProcessItem process) {
		Collection<ItemStack> inputs = new LinkedList<>();
		a:
		for (ItemStack item : this.inputs) {
			if (item.isEmpty())
				continue;
			for (ItemStack input : inputs) {
				if (ItemHelper.itemsIdentical(item, input)) {
					input.grow(item.getCount() * process.getSum());
					continue a;
				}
			}
			inputs.add(ItemHelper.cloneStack(item, item.getCount() * process.getSum()));
		}
		return inputs;
	}

	@Override
	public ItemStack[] getOutputs() {
		return outputs;
	}

	public PacketBase getPacket(ItemStack stack, boolean input, int slot) {
		PacketBase packet = getNewPacket();
		packet.addByte(6);
		packet.addItemStack(stack);
		packet.addBool(input);
		packet.addInt(slot);
		return packet;
	}

	@Override
	protected void handleInfoPacket(byte message, PacketBase payload) {
		if (message == 6) {
			ItemStack stack = payload.getItemStack();
			ItemStack[] inventory = payload.getBool() ? inputs : outputs;
			inventory[payload.getInt()] = stack;
			baseTile.markChunkDirty();
		}
	}

	@Override
	public ResourceLocation getId() {
		return ID;
	}

	@Override
	protected boolean isValidTile(TileEntity tile) {
		return tile.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, EnumFacing.VALUES[side ^ 1]);
	}

	@Override
	protected void setAutoInput(ICrafterWrapper wrapper) {
		Arrays.fill(inputs, ItemStack.EMPTY);
		List<ItemStack> list = wrapper.getInputs(ItemStack.class);
		for (int i = 0; i < list.size(); i++) {
			if (i >= inputs.length)
				break;
			inputs[i] = list.get(i);
		}
	}

	@Override
	protected void setAutoOutput(ICrafterWrapper wrapper) {
		Arrays.fill(outputs, ItemStack.EMPTY);
		List<ItemStack> list = wrapper.getOutputs(ItemStack.class);
		for (int i = 0; i < list.size(); i++) {
			if (i >= outputs.length)
				break;
			outputs[i] = list.get(i);
		}
	}

	public void addLeftover(ItemStack stack) {
		for (ItemStack leftover : leftovers) {
			if (ItemHelper.itemsIdentical(stack, leftover)) {
				leftover.grow(stack.getCount());
				baseTile.markChunkDirty();
				return;
			}
		}
		leftovers.add(stack);
		baseTile.markChunkDirty();
	}

	public ItemStack registerLeftover(ItemStack stack, IRequester<DuctUnitItem, ItemStack> destination, boolean simulate) {
		Iterator<ItemStack> iterator = leftovers.iterator();
		while (iterator.hasNext()) {
			ItemStack next = iterator.next();
			if (ItemHelper.itemsIdentical(next, stack)) {
				int amt = Math.min(next.getCount(), stack.getCount());
				if (!simulate) {
					next.shrink(amt);
					if (next.isEmpty())
						iterator.remove();
				}

				stack = ItemHelper.cloneStack(stack, amt);
				if (!simulate) {
					registry.add(Pair.of(stack, destination));
					baseTile.markChunkDirty();
				}
				return stack;
			}
		}
		return ItemStack.EMPTY;
	}

	@Override
	public void tick(int p) {
		super.tick(p);
		if (p == 0) {
			// Get new items
			ListIterator<TravelingItem> iterator = getDuct().itemsToAdd.listIterator();
			a:
			while (iterator.hasNext()) {
				TravelingItem item = iterator.next();
				if (item.oldDirection == (side ^ 1)) {
					Iterator<Pair<ItemStack, IRequester<DuctUnitItem, ItemStack>>> leftoverIterator = registry.iterator();
					while (leftoverIterator.hasNext()) {
						Pair<ItemStack, IRequester<DuctUnitItem, ItemStack>> leftover = leftoverIterator.next();

						//noinspection SuspiciousMethodCalls
						if (processes.contains(leftover.getRight()) && ((ProcessItem) leftover.getRight()).addItem(iterator, item))
							continue a;

						if (leftover.getRight().isInvalid()) {
							addLeftover(leftover.getLeft());
							leftoverIterator.remove();
							baseTile.markChunkDirty();
							continue;
						}
						if (ItemHelper.itemsIdentical(item.stack, leftover.getLeft())) {
							// Copied from ProcessItem TODO: Make this generic
							Route pass = getDuct().getRoute(leftover.getRight().getDuct());
							if (pass == null) {
								leftover.getRight().removeLeftover(this, leftover.getLeft());
								addLeftover(leftover.getLeft());
								leftoverIterator.remove();
								baseTile.markChunkDirty();
								continue;
							}

							if (leftover.getRight() instanceof ProcessItem)
								((ProcessItem) leftover.getRight()).send(ItemHelper.cloneStack(leftover.getLeft(), Math.min(item.stack.getCount(), leftover.getLeft().getCount())));

							if (item.stack.getCount() > leftover.getLeft().getCount()) {
								int amt = item.stack.getCount() - leftover.getLeft().getCount();
								item.stack.shrink(amt);
								leftover.getRight().removeLeftover(this, leftover.getLeft());
								leftoverIterator.remove();

								// Split the item
								Route route = item.myPath.copy();
								if (route.pathDirections.size() == 0)
									route.pathDirections.add(item.direction);
								else
									route.pathDirections.insert(0, item.direction);

								TravelingItem ti = new TravelingItem(ItemHelper.cloneStack(item.stack, amt), item.startX, item.startY, item.startZ, route, (byte) (side ^ 1), (byte) item.step);
								getDuct().getGrid().poll(ti);

								iterator.add(ti);
								iterator.previous();
							} else {
								leftover.getRight().removeLeftover(this, ItemHelper.cloneStack(leftover.getLeft(), item.stack.getCount()));
								leftover.getLeft().shrink(item.stack.getCount());
								if (leftover.getLeft().isEmpty())
									leftoverIterator.remove();
							}

							// Update path
							pass = pass.copy();
							pass.pathDirections.add(leftover.getRight().getSide());
							item.direction = pass.getNextDirection();
							item.myPath = pass;
							item.step = ServoItem.speedBoost[leftover.getRight().getType()];
							item.destX = pass.endPoint.x();
							item.destY = pass.endPoint.y();
							item.destZ = pass.endPoint.z();
							item.hasDest = true;
							getDuct().getGrid().poll(item);
							getDuct().getGrid().shouldRepoll = true;
							baseTile.markChunkDirty();
							continue a;
						}
					}

					// Remove it from our leftovers
					Iterator<ItemStack> iterator1 = leftovers.iterator();
					while (iterator1.hasNext()) {
						ItemStack stack = iterator1.next();
						if (ItemHelper.itemsIdentical(item.stack, stack)) {
							stack.shrink(item.stack.getCount());
							if (stack.isEmpty())
								iterator1.remove();
							baseTile.markChunkDirty();
							continue a;
						}
					}
				}
			}

			if (getDuct().tileCache[side] != null) {
				// Extract leftovers
				IItemHandler handler = getDuct().tileCache[side].getItemHandler(side ^ 1);
				Iterator<Pair<ItemStack, IRequester<DuctUnitItem, ItemStack>>> leftoverIterator = registry.iterator();
				while (leftoverIterator.hasNext()) {
					Pair<ItemStack, IRequester<DuctUnitItem, ItemStack>> leftover = leftoverIterator.next();
					if (leftover.getLeft() == null || !leftover.getRight().isTick())
						break;

					Route route = getDuct().getRoute(leftover.getRight().getDuct());
					if (route == null) {
						leftover.getRight().removeLeftover(this, leftover.getLeft());
						addLeftover(leftover.getLeft());
						leftoverIterator.remove();
						baseTile.markChunkDirty();
						continue;
					}

					ItemStack stack = NetworkUtils.maxTransfer(leftover.getLeft(), leftover.getRight().getDuct(), leftover.getRight().getSide(), leftover.getRight().getType(), true);
					if (stack.isEmpty())
						break;

					ItemStack item = NetworkUtils.extract(handler, stack);
					if (item.isEmpty())
						break;

					if (ServoItem.multiStack[leftover.getRight().getType()]) {
						while (item.getCount() < stack.getCount()) {
							ItemStack extract = NetworkUtils.extract(handler, ItemHelper.cloneStack(stack, stack.getCount() - item.getCount()));
							if (!extract.isEmpty())
								item.grow(extract.getCount());
							else break;
						}
					}

					if (leftover.getRight() instanceof ProcessItem)
						((ProcessItem) leftover.getRight()).send(ItemHelper.cloneStack(item));

					leftover.getRight().removeLeftover(this, item);
					leftover.getLeft().shrink(item.getCount());
					baseTile.markChunkDirty();

					route = route.copy();
					route.pathDirections.add(leftover.getRight().getSide());
					getDuct().insertNewItem(new TravelingItem(item, getDuct(), route, (byte) (side ^ 1), ServoItem.speedBoost[leftover.getRight().getType()]));

					if (leftover.getLeft().isEmpty()) {
						leftoverIterator.remove();
						continue;
					}
					break;
				}
				if ((!leftovers.isEmpty() || !registry.isEmpty()) && processes.isEmpty() && NetworkUtils.isEmpty(handler)) { // TODO: Optimise NetworkUtils.isEmpty
					// Reset leftovers
					leftovers.clear();
					registry.forEach(pair -> pair.getRight().removeLeftover(this, pair.getLeft()));
					registry.clear();
					baseTile.markChunkDirty();
				}
			} else {
				// Reset leftovers
				leftovers.clear();
				registry.forEach(pair -> pair.getRight().removeLeftover(this, pair.getLeft()));
				registry.clear();
				baseTile.markChunkDirty();
			}
		}
	}

	@Override
	public DuctToken tickUnit() {
		return DuctToken.ITEMS;
	}

	@Override
	public IFilterItems getItemFilter() {
		return filter;
	}

	@Override
	public IFilterFluid getFluidFilter() {
		return null;
	}

	@Override
	public boolean isTick() {
		return baseTile.getWorld().getTotalWorldTime() % ServoItem.tickDelays[getType()] == 0;
	}

	private class FilterItem implements IFilterItems {

		@Override
		public boolean matchesFilter(ItemStack item) {
			for (ItemStack input : inputs)
				if (itemsIdentical(input, item))
					return true;
			return false;
		}

		@Override
		public boolean shouldIncRouteItems() {
			return true;
		}

		@Override
		public int getMaxStock() {
			return Integer.MAX_VALUE;
		}

	}

	private class Cache implements Crafter.Cache {

		private final ItemStack[] inputs;
		private final ItemStack[] outputs;

		private Cache() {
			inputs = new ItemStack[CrafterItem.this.inputs.length];
			outputs = new ItemStack[CrafterItem.this.outputs.length];
			Arrays.fill(inputs, ItemStack.EMPTY);
			Arrays.fill(outputs, ItemStack.EMPTY);
		}

		@Override
		public void detectAndSendChanges(EntityPlayer player) {
			for (int i = 0; i < inputs.length; i++) {
				if (!ItemStack.areItemStacksEqual(inputs[i], CrafterItem.this.inputs[i])) {
					inputs[i] = CrafterItem.this.inputs[i];
					PacketHandler.sendTo(getPacket(inputs[i], true, i), player);
				}
			}
			for (int i = 0; i < outputs.length; i++) {
				if (!ItemStack.areItemStacksEqual(outputs[i], CrafterItem.this.outputs[i])) {
					outputs[i] = CrafterItem.this.outputs[i];
					PacketHandler.sendTo(getPacket(outputs[i], false, i), player);
				}
			}
		}

	}

}
