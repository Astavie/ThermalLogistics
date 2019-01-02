package astavie.thermallogistics.process;

import astavie.thermallogistics.attachment.CrafterItem;
import astavie.thermallogistics.util.IProcessHolder;
import astavie.thermallogistics.util.IRequester;
import astavie.thermallogistics.util.NetworkUtils;
import astavie.thermallogistics.util.reference.RequesterReference;
import astavie.thermallogistics.util.request.IRequest;
import astavie.thermallogistics.util.request.Request;
import astavie.thermallogistics.util.request.Requests;
import cofh.core.util.helpers.ItemHelper;
import cofh.thermaldynamics.duct.Attachment;
import cofh.thermaldynamics.duct.attachments.servo.ServoItem;
import cofh.thermaldynamics.duct.item.DuctUnitItem;
import cofh.thermaldynamics.duct.item.GridItem;
import cofh.thermaldynamics.duct.item.StackMap;
import cofh.thermaldynamics.duct.item.TravelingItem;
import cofh.thermaldynamics.multiblock.Route;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.items.IItemHandler;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

public class ProcessItem extends Process<IProcessHolder<ProcessItem, DuctUnitItem, ItemStack>, ProcessItem, DuctUnitItem, ItemStack> {

	private final int delay;
	private boolean progress = false;

	public ProcessItem(IRequester<DuctUnitItem, ItemStack> destination, CrafterItem crafter, ItemStack output, int sum) {
		this(destination, (IProcessHolder<ProcessItem, DuctUnitItem, ItemStack>) crafter, output, sum);

		ItemStack stack = output.copy();
		for (ItemStack item : crafter.outputs) {
			if (item.isEmpty())
				continue;
			item = ItemHelper.cloneStack(item, item.getCount() * sum);
			if (ItemHelper.itemsIdentical(item, stack)) {
				int amt = Math.min(item.getCount(), stack.getCount());
				item.shrink(amt);
				stack.shrink(amt);
				if (!item.isEmpty())
					crafter.addLeftover(item);
			} else
				crafter.addLeftover(item);
		}
	}

	public ProcessItem(IRequester<DuctUnitItem, ItemStack> destination, IProcessHolder<ProcessItem, DuctUnitItem, ItemStack> crafter, ItemStack output, int sum) {
		super(destination, crafter, output, sum);
		this.delay = (int) (crafter.getTile().getWorld().getTotalWorldTime() % ServoItem.tickDelays[crafter.getType()]) + 1;
	}

	public ProcessItem(World world, NBTTagCompound tag) {
		super(world, tag);
		this.delay = tag.getInteger("delay");
		this.progress = tag.getBoolean("progress");
	}

	@Override
	public List<Requests<DuctUnitItem, ItemStack>> getRequests() {
		List<Requests<DuctUnitItem, ItemStack>> list = super.getRequests();
		if (!this.input.getStacks().isEmpty()) {
			IRequest<DuctUnitItem, ItemStack> input = this.input.copyFaceless(getDelegate());
			for (ProcessItem process : sub) {
				ItemStack output = process.output.copy();
				Iterator<ItemStack> iterator = input.getStacks().iterator();
				while (iterator.hasNext()) {
					ItemStack stack = iterator.next();
					if (crafter.getRequester().itemsIdentical(output, stack)) {
						int i = Math.min(output.getCount(), stack.getCount());

						stack.shrink(i);
						if (stack.isEmpty())
							iterator.remove();

						output.shrink(i);
						if (output.isEmpty())
							break;
					}
				}
			}
			for (IRequest<DuctUnitItem, ItemStack> request : leftovers) {
				for (ItemStack leftover : request.getStacks()) {
					ItemStack output = leftover.copy();
					Iterator<ItemStack> iterator = input.getStacks().iterator();
					while (iterator.hasNext()) {
						ItemStack stack = iterator.next();
						if (crafter.getRequester().itemsIdentical(output, stack)) {
							int i = Math.min(output.getCount(), stack.getCount());

							stack.shrink(i);
							if (stack.isEmpty())
								iterator.remove();

							output.shrink(i);
							if (output.isEmpty())
								break;
						}
					}
				}
			}
			if (!input.getStacks().isEmpty()) {
				if (list.isEmpty())
					return Collections.singletonList(new Requests<>(this, Collections.singletonList(input)));
				list.get(0).getRequests().add(input);
			}
		}
		return list;
	}

	@Override
	protected ResourceLocation getId() {
		return new ResourceLocation("item");
	}

	@Override
	public boolean isStuck() {
		return !progress && !output.isEmpty() && (sub.isEmpty() || sub.stream().anyMatch(Process::isStuck));
	}

	@Override
	public void updateOutput() {
		if (!output.isEmpty()) {
			if (crafter.getRequester() instanceof CrafterItem) {
				for (Pair<ItemStack, IRequester<DuctUnitItem, ItemStack>> pair : ((CrafterItem) crafter.getRequester()).registry) {
					if (pair.getRight() == this)
						break;
					if (pair.getLeft() != null || (pair.getRight() instanceof ProcessItem && !((ProcessItem) pair.getRight()).output.isEmpty()))
						return;
				}
			}

			Route route = getDuct().getRoute(destination.getDuct());
			if (route == null) {
				failed = true;
				return;
			}

			IItemHandler inv = getDuct().tileCache[getSide()].getItemHandler(getSide() ^ 1);
			for (int i = 0; i < inv.getSlots(); i++) {
				ItemStack stack = inv.getStackInSlot(i);
				if (stack.isEmpty() || !ItemHelper.itemsIdentical(stack, output))
					continue;

				TravelingItem ti = NetworkUtils.transfer(i, getDuct(), getSide(), destination.getDuct(), destination.getSide(), route, output.getCount(), destination.getType(), true);
				if (ti != null)
					break; // It was sent successfully
			}
		}
	}

	@Override
	public void updateInput() {
		// Copied from RequesterItem TODO: Make this generic
		// Check subprocesses
		linked.removeIf(IProcess::isDone);
		sub.removeIf(process -> process.isDone() || process.hasFailed());

		// Search for items
		Set<CrafterItem> crafters = new HashSet<>();
		for (Route<DuctUnitItem, GridItem> route : NetworkUtils.getRoutes(getDuct())) {
			DuctUnitItem end = route.endPoint;
			byte side = route.getLastSide();

			Attachment attachment = end.parent.getAttachment(side);
			if (attachment != null)
				if (attachment instanceof CrafterItem) {
					crafters.add((CrafterItem) attachment);
					continue;
				} else if (!attachment.canSend())
					continue;

			DuctUnitItem.Cache cache = end.tileCache[side];
			if (cache == null || (!end.isInput(side) && !end.isOutput(side)) || !end.parent.getConnectionType(side).allowTransfer)
				continue;

			IItemHandler inv = cache.getItemHandler(side ^ 1);
			if (inv == null)
				continue;

			Route route1 = end.getRoute(getDuct());
			if (route1 == null)
				continue;

			for (int slot = 0; slot < inv.getSlots(); slot++) {
				ItemStack item = inv.getStackInSlot(slot);
				if (item.isEmpty())
					continue;

				// Calculate amount required
				int amt = 0;
				for (ItemStack stack : input.getStacks())
					if (crafter.getRequester().itemsIdentical(stack, item))
						amt += stack.getCount();
				if (amt == 0)
					continue;

				for (ProcessItem process : sub)
					if (!process.output.isEmpty() && crafter.getRequester().itemsIdentical(process.output, item))
						amt -= process.output.getCount();
				if (amt <= 0)
					continue;

				// Try to send it
				TravelingItem ti = NetworkUtils.transfer(slot, end, side, getDuct(), getSide(), route1, amt, getType(), true);
				if (ti != null) {
					send(ti.stack.copy());
					return;
				}
			}
		}

		// No items found, let's see if any process has some leftovers
		for (CrafterItem crafter : crafters) {
			if (this.crafter.equals(new RequesterReference<>(crafter)))
				continue;

			for (ItemStack output : crafter.outputs) {
				if (output.isEmpty())
					continue;

				// Calculate amount required
				int amt = 0;
				for (ItemStack stack : input.getStacks())
					if (crafter.itemsIdentical(stack, output))
						amt += stack.getCount();
				if (amt == 0)
					continue;

				for (IRequest<DuctUnitItem, ItemStack> request : leftovers)
					for (ItemStack stack : request.getStacks())
						if (this.crafter.getRequester().itemsIdentical(stack, output))
							amt -= stack.getCount();
				for (ProcessItem process : sub)
					if (!process.output.isEmpty() && this.crafter.getRequester().itemsIdentical(process.output, output))
						amt -= process.output.getCount();
				if (amt <= 0)
					continue;

				output = output.copy();
				output = crafter.registerLeftover(output, this, true);
				if (output.isEmpty())
					continue;

				// Calculate maximum transfer
				output = NetworkUtils.maxTransfer(ItemHelper.cloneStack(output, amt), getDuct(), getSide(), getType(), true);
				if (output.isEmpty())
					continue;

				// Alright, let's do this!
				this.leftovers.add(new Request<>(crafter.baseTile.getWorld(), crafter, crafter.registerLeftover(output, this, false)));
				return;
			}
		}

		// No leftovers found, let's make some
		a:
		for (CrafterItem crafter : crafters) {
			if (this.crafter.equals(new RequesterReference<>(crafter)))
				continue;

			assert crafter.getDuct() != null;
			for (ItemStack output : crafter.outputs) {
				if (output.isEmpty())
					continue;

				// Calculate amount required
				int amt = 0;
				for (ItemStack stack : input.getStacks())
					if (crafter.itemsIdentical(stack, output))
						amt += stack.getCount();
				if (amt == 0)
					continue;

				for (IRequest<DuctUnitItem, ItemStack> request : leftovers)
					for (ItemStack stack : request.getStacks())
						if (this.crafter.getRequester().itemsIdentical(stack, output))
							amt -= stack.getCount();
				for (ProcessItem process : sub) {
					if (process.getCrafter().equals(new RequesterReference<>(crafter)) && process.isStuck())
						continue a;
					if (!process.output.isEmpty() && this.crafter.getRequester().itemsIdentical(process.output, output))
						amt -= process.output.getCount();
				}
				if (amt <= 0)
					continue;

				// Calculate maximum transfer
				ItemStack out = NetworkUtils.maxTransfer(ItemHelper.cloneStack(output, amt), getDuct(), getSide(), getType(), true);
				if (out.isEmpty())
					continue;

				// Alright, let's do this!
				int sum = (int) Math.ceil((double) out.getCount() / output.getCount());
				ProcessItem process = new ProcessItem(this, crafter, out, sum);
				sub.add(process);
				return;
			}
		}
	}

	public boolean addItem(ListIterator<TravelingItem> iterator, TravelingItem item) {
		if (!output.isEmpty() && ItemHelper.itemsIdentical(output, item.stack)) {
			// Reroute the item
			Route pass = getDuct().getRoute(destination.getDuct());
			if (pass == null) {
				failed = true;
				return false;
			}

			// Notify our parent
			if (destination instanceof ProcessItem)
				((ProcessItem) destination).send(ItemHelper.cloneStack(output, Math.min(item.stack.getCount(), output.getCount())));

			if (item.stack.getCount() > output.getCount()) {
				int amt = item.stack.getCount() - output.getCount();
				item.stack.shrink(amt);
				output.setCount(0);

				// Split the item
				Route route = item.myPath.copy();
				if (route.pathDirections.size() == 0)
					route.pathDirections.add(item.direction);
				else
					route.pathDirections.insert(0, item.direction);

				TravelingItem ti = new TravelingItem(ItemHelper.cloneStack(item.stack, amt), item.startX, item.startY, item.startZ, route, (byte) (getSide() ^ 1), (byte) item.step);
				getDuct().getGrid().poll(ti);

				iterator.add(ti);
				iterator.previous();
			} else
				output.shrink(item.stack.getCount());

			// Update path
			pass = pass.copy();
			pass.pathDirections.add(destination.getSide());
			item.direction = pass.getNextDirection();
			item.myPath = pass;
			item.step = ServoItem.speedBoost[destination.getType()];
			item.destX = pass.endPoint.x();
			item.destY = pass.endPoint.y();
			item.destZ = pass.endPoint.z();
			item.hasDest = true;
			getDuct().getGrid().poll(item);
			getDuct().getGrid().shouldRepoll = true;
			crafter.getRequester().getTile().markChunkDirty();
			return true;
		}
		return false;
	}

	public void getResult(DuctUnitItem end, int side, StackMap map) {
		if (!output.isEmpty() && destination != null && destination.getDuct() == end && destination.getSide() == side)
			map.addItemstack(output.copy(), side);
	}

	@Override
	public NBTTagCompound save() {
		NBTTagCompound tag = super.save();
		tag.setInteger("delay", delay);
		tag.setBoolean("progress", progress);
		return tag;
	}

	@Override
	public boolean isTick() {
		return (crafter.world.getTotalWorldTime() - delay) % ServoItem.tickDelays[getType()] == 0;
	}

	@Override
	public void removeLeftover(IRequester<DuctUnitItem, ItemStack> requester, ItemStack leftover) {
		Iterator<IRequest<DuctUnitItem, ItemStack>> iterator = leftovers.iterator();
		while (iterator.hasNext()) {
			IRequest<DuctUnitItem, ItemStack> next = iterator.next();
			if (next.getStart() == requester) {
				Iterator<ItemStack> it = next.getStacks().iterator();
				while (it.hasNext()) {
					ItemStack stack = it.next();
					if (ItemHelper.itemsIdentical(stack, leftover)) {
						stack.shrink(leftover.getCount());
						if (stack.isEmpty()) {
							it.remove();
							if (next.getStacks().isEmpty())
								iterator.remove();
						}
						return;
					}
				}
				return;
			}
		}
	}

	public void send(ItemStack item) {
		progress = true;
		for (Iterator<ItemStack> iterator = input.getStacks().iterator(); iterator.hasNext(); ) {
			ItemStack sent = iterator.next();
			if (crafter.getRequester().itemsIdentical(sent, item)) {
				int i = Math.min(sent.getCount(), item.getCount());

				sent.shrink(i);
				if (sent.isEmpty())
					iterator.remove();

				item.shrink(i);
				if (item.isEmpty())
					return;
			}
		}
	}

}
