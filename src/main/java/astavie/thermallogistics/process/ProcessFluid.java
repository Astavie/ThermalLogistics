package astavie.thermallogistics.process;

import astavie.thermallogistics.attachment.CrafterFluid;
import astavie.thermallogistics.util.IProcessHolder;
import astavie.thermallogistics.util.IRequester;
import astavie.thermallogistics.util.NetworkUtils;
import astavie.thermallogistics.util.request.IRequest;
import astavie.thermallogistics.util.request.Request;
import codechicken.lib.fluid.FluidUtils;
import cofh.core.util.helpers.FluidHelper;
import cofh.thermaldynamics.duct.Attachment;
import cofh.thermaldynamics.duct.attachments.servo.ServoFluid;
import cofh.thermaldynamics.duct.fluid.DuctUnitFluid;
import cofh.thermaldynamics.duct.fluid.FluidTankGrid;
import cofh.thermaldynamics.duct.fluid.GridFluid;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class ProcessFluid extends Process<IProcessHolder<ProcessFluid, DuctUnitFluid, FluidStack>, ProcessFluid, DuctUnitFluid, FluidStack> {

	private final Set<FluidStack> fluidStacks = new HashSet<>();

	private final FluidStack start;
	private boolean progress = false;
	private boolean idle = true;

	public ProcessFluid(IRequester<DuctUnitFluid, FluidStack> destination, CrafterFluid crafter, FluidStack output, int sum) {
		this(destination, (IProcessHolder<ProcessFluid, DuctUnitFluid, FluidStack>) crafter, output, sum);

		FluidStack stack = output == null ? null : output.copy();
		for (FluidStack item : crafter.outputs) {
			if (item == null)
				continue;
			item = FluidUtils.copy(item, item.amount * sum);
			if (FluidHelper.isFluidEqual(item, stack)) {
				int amt = Math.min(item.amount, stack.amount);
				item.amount -= amt;
				stack.amount -= amt;
				if (item.amount > 0)
					crafter.addLeftover(item);
			} else
				crafter.addLeftover(item);
		}
	}

	public ProcessFluid(IRequester<DuctUnitFluid, FluidStack> destination, IProcessHolder<ProcessFluid, DuctUnitFluid, FluidStack> crafter, FluidStack output, int sum) {
		super(destination, crafter, output, sum);
		this.start = output == null ? null : output.copy();
	}

	public ProcessFluid(World world, NBTTagCompound tag) {
		super(world, tag);
		this.start = FluidStack.loadFluidStackFromNBT(tag.getCompoundTag("start"));
		this.progress = tag.getBoolean("progress");

		NBTTagList fluidStacks = tag.getTagList("fluidStacks", Constants.NBT.TAG_COMPOUND);
		for (int i = 0; i < fluidStacks.tagCount(); i++)
			this.fluidStacks.add(FluidStack.loadFluidStackFromNBT(fluidStacks.getCompoundTagAt(i)));
	}

	@Override
	public Collection<IRequest<DuctUnitFluid, FluidStack>> getRequests() {
		Collection<IRequest<DuctUnitFluid, FluidStack>> collection = super.getRequests();
		if (idle) {
			IRequest<DuctUnitFluid, FluidStack> input = this.input.copy(getDelegate());
			for (ProcessFluid process : sub) {
				FluidStack output = process.output.copy();
				Iterator<FluidStack> iterator = input.getStacks().iterator();
				while (iterator.hasNext()) {
					FluidStack stack = iterator.next();
					if (crafter.itemsIdentical(output, stack)) {
						int i = Math.min(output.amount, stack.amount);

						stack.amount -= i;
						if (stack.amount <= 0)
							iterator.remove();

						output.amount -= i;
						if (output.amount <= 0)
							break;
					}
				}
			}
			for (IRequest<DuctUnitFluid, FluidStack> request : leftovers) {
				for (FluidStack leftover : request.getStacks()) {
					FluidStack output = leftover.copy();
					Iterator<FluidStack> iterator = input.getStacks().iterator();
					while (iterator.hasNext()) {
						FluidStack stack = iterator.next();
						if (crafter.itemsIdentical(output, stack)) {
							int i = Math.min(output.amount, stack.amount);

							stack.amount -= i;
							if (stack.amount <= 0)
								iterator.remove();

							output.amount -= i;
							if (output.amount <= 0)
								break;
						}
					}
				}
			}
			collection.add(input);
		}
		return collection;
	}

	@Override
	protected ResourceLocation getId() {
		return new ResourceLocation("fluid");
	}

	@Override
	public boolean isStuck() {
		return !progress && output != null && output.amount > 0 && (sub.isEmpty() || sub.stream().anyMatch(Process::isStuck));
	}

	@Override
	public void updateOutput() {
		// Copied from ServoFluid
		if (output != null && output.amount > 0) {
			if (crafter instanceof CrafterFluid && ((CrafterFluid) crafter).registry.get(0).getRight() != this)
				return;

			FluidTankGrid myTank = getDuct().getGrid().myTank;
			int maxInput = Math.min((int) Math.ceil(myTank.fluidThroughput * ServoFluid.throttle[destination.getType()]), output.amount);
			IFluidHandler ductHandler = getDuct().getFluidCapability(EnumFacing.VALUES[getSide()]);
			if (ductHandler == null) {
				failed = true;
				return;
			}

			DuctUnitFluid.Cache cache = crafter.getDuct().tileCache[crafter.getSide()];
			if (cache == null) {
				failed = true;
				return;
			}

			IFluidHandler tileHandler = cache.getHandler(getSide() ^ 1);
			if (tileHandler == null) {
				failed = true;
				return;
			}

			FluidStack drainFluid = tileHandler.drain(maxInput, false);
			if (drainFluid != null && !this.crafter.itemsIdentical(drainFluid, output))
				return;

			maxInput = myTank.fill(drainFluid, false);
			output.amount -= myTank.fill(tileHandler.drain(maxInput, true), true);
		}
	}

	@Override
	public void updateInput() {
		linked.removeIf(IProcess::isDone);
		sub.removeIf(process -> process.hasFailed() || (process.isDone() && waiting(process.getOutput()) == 0));

		// Copied from RequesterFluid TODO: Make this generic
		GridFluid grid = getDuct().getGrid();
		FluidTankGrid tank = grid.myTank;
		int maxInput = (int) Math.ceil(tank.fluidThroughput * ServoFluid.throttle[getType()]);
		int c = maxInput;

		if (tank.getFluid() != null)
			if (crafter.amountRequired(this, tank.getFluid()) == 0)
				return;
		Set<CrafterFluid> crafters = new HashSet<>();
		for (Iterator<DuctUnitFluid> iterator = grid.nodeSet.iterator(); iterator.hasNext() && maxInput > 0; ) {
			DuctUnitFluid fluidDuct = iterator.next();
			for (int k = 0; k < 6 && maxInput > 0; k++) {
				int side = (k + fluidDuct.internalSideCounter) % 6;
				DuctUnitFluid.Cache cache = fluidDuct.tileCache[side];
				if (cache == null || (!fluidDuct.isOutput(side) && !fluidDuct.isInput(side)))
					continue;

				Attachment attachment = fluidDuct.parent.getAttachment(side);
				if (attachment != null)
					if (attachment instanceof CrafterFluid) {
						crafters.add((CrafterFluid) attachment);
						continue;
					} else if (!attachment.canSend())
						continue;

				IFluidHandler handler = cache.getHandler(side ^ 1);
				if (handler == null)
					continue;

				FluidStack drainFluid = handler.drain(maxInput, false);
				if (drainFluid != null) {
					int input = tank.fill(drainFluid, false);
					if (crafter.amountRequired(this, drainFluid) > 0) {
						maxInput -= tank.fill(handler.drain(input, true), true);
						if (grid.toDistribute > 0 && tank.getFluid() != null) {
							GridFluid otherGrid = fluidDuct.getGrid();
							if (otherGrid != null)
								getDuct().transfer(side, Math.min(otherGrid.myTank.getFluid().amount, otherGrid.toDistribute), false, otherGrid.myTank.getFluid(), true);
						}
					}
				}
			}
		}
		if (maxInput == c) {
			idle = true;

			// No fluid found, let's see if any process has some leftovers
			for (CrafterFluid crafter : crafters) {
				if (crafter == this.crafter)
					continue;

				for (FluidStack output : crafter.outputs) {
					if (output == null)
						continue;

					// Calculate amount required
					int required = 0;
					for (FluidStack stack : input.getStacks())
						if (crafter.itemsIdentical(stack, output))
							required += stack.amount;
					if (required == 0)
						continue;

					long amt = NetworkUtils.getFluid(getDuct().getGrid(), output) + waiting(output);
					if (amt >= required)
						continue;

					required -= amt;
					for (IRequest<DuctUnitFluid, FluidStack> request : leftovers)
						for (FluidStack stack : request.getStacks())
							if (this.crafter.itemsIdentical(stack, output))
								required -= stack.amount;
					for (ProcessFluid process : sub)
						if (this.crafter.itemsIdentical(process.output, output))
							required -= process.output.amount;
					if (required <= 0)
						continue;

					output = crafter.registerLeftover(FluidUtils.copy(output, required), this, false);
					if (output == null)
						continue;

					// Alright, let's do this!
					idle = false;

					this.leftovers.add(new Request<>(crafter.baseTile.getWorld(), crafter, output.copy()));
					return;
				}
			}

			a:
			for (CrafterFluid crafter : crafters) {
				if (crafter == this.crafter)
					continue;

				for (FluidStack fluid : crafter.outputs) {
					if (fluid == null)
						continue;

					int required = 0;
					for (FluidStack stack : input.getStacks())
						if (crafter.itemsIdentical(stack, output))
							required += stack.amount;
					if (required == 0)
						continue;

					long amt = NetworkUtils.getFluid(getDuct().getGrid(), fluid) + waiting(fluid);
					if (amt >= required)
						continue;

					required -= amt;
					for (IRequest<DuctUnitFluid, FluidStack> request : leftovers)
						for (FluidStack stack : request.getStacks())
							if (this.crafter.itemsIdentical(stack, output))
								required -= stack.amount;
					for (ProcessFluid process : sub) {
						if (process.getCrafter() == crafter && process.isStuck())
							continue a;
						if (this.crafter.itemsIdentical(process.getOutput(), fluid))
							required -= process.getOutput().amount;
					}

					if (required <= 0)
						continue;

					// Alright, let's do this!
					idle = false;

					int sum = (int) Math.ceil((double) required / fluid.amount);
					sub.add(new ProcessFluid(this, crafter, FluidUtils.copy(fluid, required), sum));
					return;
				}
			}
		} else {
			progress = true;
			idle = false;
		}
	}

	public int waiting(FluidStack stack) {
		if (crafter.amountRequired(this, stack) == 0)
			return 0;
		int i = 0;
		for (ProcessFluid process : sub)
			if (this.crafter.itemsIdentical(process.getOutput(), stack))
				i += process.start.amount - process.getOutput().amount;
		for (FluidStack fluid : fluidStacks)
			if (this.crafter.itemsIdentical(fluid, stack))
				i += fluid.amount;
		return Math.max(i, 0);
	}

	public int addFluid(FluidStack fluid, boolean fill) {
		int required = 0;
		for (FluidStack stack : input.getStacks())
			if (crafter.itemsIdentical(stack, fluid))
				required += stack.amount;

		if (required > 0) {
			idle = false;

			int i = Math.min(required, fluid.amount);
			if (fill) {
				for (Iterator<FluidStack> iterator = fluidStacks.iterator(); iterator.hasNext(); ) {
					FluidStack sent = iterator.next();
					if (FluidHelper.isFluidEqual(sent, fluid)) {
						sent.amount -= i;
						if (sent.amount <= 0)
							iterator.remove();
						break;
					}
				}
				int j = i;
				for (Iterator<FluidStack> iterator = input.getStacks().iterator(); iterator.hasNext(); ) {
					FluidStack sent = iterator.next();
					if (crafter.itemsIdentical(sent, fluid)) {
						int a = Math.min(sent.amount, j);

						sent.amount -= a;
						if (sent.amount <= 0)
							iterator.remove();

						j -= a;
						if (j <= 0)
							break;
					}
				}
				crafter.getTile().markChunkDirty();
			}
			return i;
		}
		return 0;
	}

	@Override
	public NBTTagCompound save() {
		NBTTagList fluidStacks = new NBTTagList();
		for (FluidStack stack : this.fluidStacks)
			fluidStacks.appendTag(stack.writeToNBT(new NBTTagCompound()));

		NBTTagCompound tag = super.save();
		if (start != null)
			tag.setTag("start", start.writeToNBT(new NBTTagCompound()));
		tag.setBoolean("progress", progress);
		tag.setTag("fluidStacks", fluidStacks);
		return tag;
	}

	@Override
	public boolean isTick() {
		return true;
	}

	@Override
	public void removeLeftover(IRequester<DuctUnitFluid, FluidStack> requester, FluidStack leftover) {
		Iterator<IRequest<DuctUnitFluid, FluidStack>> iterator = leftovers.iterator();
		while (iterator.hasNext()) {
			IRequest<DuctUnitFluid, FluidStack> next = iterator.next();
			if (next.getStart() == requester) {
				Iterator<FluidStack> it = next.getStacks().iterator();
				while (it.hasNext()) {
					FluidStack stack = it.next();
					if (FluidHelper.isFluidEqual(stack, leftover)) {
						stack.amount -= leftover.amount;
						if (stack.amount <= 0) {
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

	public void send(FluidStack drained) {
		idle = false;
		for (FluidStack sent : fluidStacks) {
			if (FluidHelper.isFluidEqual(sent, drained)) {
				sent.amount += drained.amount;
				return;
			}
		}
		fluidStacks.add(drained);
	}

}
