package astavie.thermallogistics.process;

import astavie.thermallogistics.attachment.CrafterItem;
import astavie.thermallogistics.util.*;
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

import java.util.HashSet;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Set;

public class ProcessItem extends Process<IProcessHolder<ProcessItem, DuctUnitItem, ItemStack>, ProcessItem, DuctUnitItem, ItemStack> {

	private final int delay;

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
	}

	@Override
	protected ResourceLocation getId() {
		return new ResourceLocation("item");
	}

	@Override
	public boolean isStuck() {
		return sent.isEmpty() && !output.isEmpty() && (sub.isEmpty() || sub.stream().anyMatch(Process::isStuck));
	}

	@Override
	public boolean isDone() {
		if (!sub.stream().allMatch(IProcess::isDone))
			return false;
		if (!linked.stream().allMatch(IProcess::isDone))
			return false;

		if (destination == null || output.isEmpty()) {
			Set<ItemStack> clone = new HashSet<>();
			sent.forEach(i -> clone.add(i.copy()));
			for (ItemStack stack : crafter.getInputs(this)) {
				if (stack.isEmpty())
					continue;

				stack = ItemHelper.cloneStack(stack, stack.getCount() * sum);
				for (ItemStack s : clone) {
					if (!s.isEmpty() && crafter.itemsIdentical(stack, s)) {
						int amt = Math.min(s.getCount(), stack.getCount());
						stack.shrink(amt);
						s.shrink(amt);
					}
					if (stack.isEmpty())
						break;
				}
				if (!stack.isEmpty())
					return false;
			}
			return true;
		} else return false;
	}

	@Override
	public void updateOutput() {
		if (!output.isEmpty()) {
			if (crafter instanceof CrafterItem && ((CrafterItem) crafter).registry.get(0).getRight() != this)
				return;

			Route route = crafter.getDuct().getRoute(destination.getDuct());
			if (route == null) {
				failed = true;
				return;
			}

			IItemHandler inv = crafter.getDuct().tileCache[crafter.getSide()].getItemHandler(crafter.getSide() ^ 1);
			for (int i = 0; i < inv.getSlots(); i++) {
				ItemStack stack = inv.getStackInSlot(i);
				if (stack.isEmpty() || !ItemHelper.itemsIdentical(stack, output))
					continue;

				TravelingItem ti = NetworkUtils.transfer(i, crafter.getDuct(), crafter.getSide(), destination.getDuct(), destination.getSide(), route, output.getCount(), destination.getType(), true);
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
		for (Route<DuctUnitItem, GridItem> route : NetworkUtils.getRoutes(crafter.getDuct())) {
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

			Route route1 = end.getRoute(crafter.getDuct());
			if (route1 == null)
				continue;

			for (int slot = 0; slot < inv.getSlots(); slot++) {
				ItemStack item = inv.getStackInSlot(slot);
				if (item.isEmpty())
					continue;

				// Calculate amount required
				int amt = crafter.amountRequired(this, item) * sum;
				if (amt == 0)
					continue;

				for (ItemStack stack : sent)
					if (crafter.itemsIdentical(stack, item))
						amt -= stack.getCount();
				for (ProcessItem process : sub)
					if (!process.output.isEmpty() && crafter.itemsIdentical(process.output, item))
						amt -= process.output.getCount();
				if (amt <= 0)
					continue;

				// Try to send it
				TravelingItem ti = NetworkUtils.transfer(slot, end, side, crafter.getDuct(), crafter.getSide(), route1, amt, crafter.getType(), false);
				if (ti != null) {
					sent.add(ti.stack.copy());
					return;
				}
			}
		}

		// No items found, let's see if any process has some leftovers
		for (CrafterItem crafter : crafters) {
			if (crafter == this.crafter)
				continue;

			for (ItemStack output : crafter.outputs) {
				if (output.isEmpty())
					continue;

				// Calculate amount required
				int amt = this.crafter.amountRequired(this, output) * sum;
				if (amt == 0)
					continue;

				for (ItemStack stack : sent)
					if (this.crafter.itemsIdentical(stack, output))
						amt -= stack.getCount();
				for (IRequest<DuctUnitItem, ItemStack> request : leftovers)
					for (ItemStack stack : request.getStacks())
						if (this.crafter.itemsIdentical(stack, output))
							amt -= stack.getCount();
				for (ProcessItem process : sub)
					if (!process.output.isEmpty() && this.crafter.itemsIdentical(process.output, output))
						amt -= process.output.getCount();
				if (amt <= 0)
					continue;

				output = output.copy();
				output = crafter.registerLeftover(output, this, true);
				if (output.isEmpty())
					continue;

				// Calculate maximum transfer
				output = NetworkUtils.maxTransfer(ItemHelper.cloneStack(output, amt), this.crafter.getDuct(), this.crafter.getSide(), this.crafter.getType(), false);
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
			if (crafter == this.crafter)
				continue;

			assert crafter.getDuct() != null;
			for (ItemStack output : crafter.outputs) {
				if (output.isEmpty())
					continue;

				// Calculate amount required
				int amt = this.crafter.amountRequired(this, output) * sum;
				if (amt == 0)
					continue;

				for (ItemStack stack : sent)
					if (this.crafter.itemsIdentical(stack, output))
						amt -= stack.getCount();
				for (IRequest<DuctUnitItem, ItemStack> request : leftovers)
					for (ItemStack stack : request.getStacks())
						if (this.crafter.itemsIdentical(stack, output))
							amt -= stack.getCount();
				for (ProcessItem process : sub) {
					if (process.getCrafter() == crafter && process.isStuck())
						continue a;
					if (!process.output.isEmpty() && this.crafter.itemsIdentical(process.output, output))
						amt -= process.output.getCount();
				}
				if (amt <= 0)
					continue;

				// Calculate maximum transfer
				ItemStack out = NetworkUtils.maxTransfer(ItemHelper.cloneStack(output, amt), this.crafter.getDuct(), this.crafter.getSide(), this.crafter.getType(), false);
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
			Route pass = crafter.getDuct().getRoute(destination.getDuct());
			if (pass == null) {
				failed = true;
				return false;
			}

			// Notify our parent
			if (destination instanceof ProcessItem)
				((ProcessItem) destination).sent.add(ItemHelper.cloneStack(output, Math.min(item.stack.getCount(), output.getCount())));

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

				TravelingItem ti = new TravelingItem(ItemHelper.cloneStack(item.stack, amt), item.startX, item.startY, item.startZ, route, (byte) (crafter.getSide() ^ 1), (byte) item.step);
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
			crafter.getTile().markChunkDirty();
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
		return tag;
	}

	@Override
	public boolean isTick() {
		return (crafter.getTile().getWorld().getTotalWorldTime() - delay) % ServoItem.tickDelays[crafter.getType()] == 0;
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
		sent.add(item);
	}

}
