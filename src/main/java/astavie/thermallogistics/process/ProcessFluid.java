package astavie.thermallogistics.process;

import astavie.thermallogistics.attachment.CrafterFluid;
import astavie.thermallogistics.util.IDestination;
import astavie.thermallogistics.util.IProcessHolder;
import astavie.thermallogistics.util.NetworkUtils;
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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class ProcessFluid extends Process<IProcessHolder<ProcessFluid, DuctUnitFluid, FluidStack>, ProcessFluid, DuctUnitFluid, FluidStack> {

	private final Set<FluidStack> fluidStacks = new HashSet<>();

	private final FluidStack start;
	private boolean progress = false;

	public ProcessFluid(IDestination<DuctUnitFluid, FluidStack> destination, CrafterFluid crafter, FluidStack output, int sum) {
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

	public ProcessFluid(IDestination<DuctUnitFluid, FluidStack> destination, IProcessHolder<ProcessFluid, DuctUnitFluid, FluidStack> crafter, FluidStack output, int sum) {
		super(destination, crafter, output, sum);
		this.start = output == null ? null : output.copy();
	}

	public ProcessFluid(World world, NBTTagCompound tag) {
		super(world, tag);
		this.start = readItem(tag.getCompoundTag("start"));
		this.progress = tag.getBoolean("progress");

		NBTTagList fluidStacks = tag.getTagList("fluidStacks", Constants.NBT.TAG_COMPOUND);
		for (int i = 0; i < fluidStacks.tagCount(); i++)
			this.fluidStacks.add(readItem(fluidStacks.getCompoundTagAt(i)));
	}

	@Override
	protected FluidStack readItem(NBTTagCompound tag) {
		return FluidStack.loadFluidStackFromNBT(tag);
	}

	@Override
	protected NBTTagCompound writeItem(FluidStack output) {
		return output.writeToNBT(new NBTTagCompound());
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
	public boolean isDone() {
		if (!sub.stream().allMatch(IProcess::isDone))
			return false;
		if (!linked.stream().allMatch(IProcess::isDone))
			return false;

		if (destination == null || output == null || output.amount == 0) {
			Set<FluidStack> clone = new HashSet<>();
			sent.forEach(i -> clone.add(i.copy()));
			for (FluidStack stack : crafter.getInputs(this)) {
				if (stack == null)
					continue;

				stack = FluidUtils.copy(stack, stack.amount * sum);
				for (FluidStack s : clone) {
					if (s.amount != 0 && crafter.itemsIdentical(stack, s)) {
						int amt = Math.min(s.amount, stack.amount);
						stack.amount -= amt;
						s.amount -= amt;
					}
					if (stack.amount == 0)
						break;
				}
				if (stack.amount != 0)
					return false;
			}
			return true;
		} else return false;
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
			// No fluid found, let's see if any process has some leftovers
			for (CrafterFluid crafter : crafters) {
				if (crafter == this.crafter)
					continue;

				for (FluidStack output : crafter.outputs) {
					if (output == null)
						continue;

					// Calculate amount required
					int required = this.crafter.amountRequired(this, output) * sum;
					if (required == 0)
						continue;

					long amt = NetworkUtils.getFluid(getDuct().getGrid(), output) + waiting(output);
					if (amt >= required)
						continue;

					required -= amt;
					for (FluidStack stack : sent)
						if (this.crafter.itemsIdentical(stack, output))
							required -= stack.amount;
					for (FluidStack stack : leftovers)
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
					output = output.copy();
					for (FluidStack leftover : leftovers) {
						if (FluidHelper.isFluidEqual(output, leftover)) {
							leftover.amount += output.amount;
							return;
						}
					}
					this.leftovers.add(output);
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

					int required = this.crafter.amountRequired(this, fluid) * sum;
					if (required == 0)
						continue;

					long amt = NetworkUtils.getFluid(getDuct().getGrid(), fluid) + waiting(fluid);
					if (amt >= required)
						continue;

					required -= amt;
					for (FluidStack stack: sent)
						if (this.crafter.itemsIdentical(stack, fluid))
							required -= stack.amount;
					for (FluidStack stack : leftovers)
						if (this.crafter.itemsIdentical(stack, fluid))
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
					int sum = (int) Math.ceil((double) required / fluid.amount);
					sub.add(new ProcessFluid(this, crafter, FluidUtils.copy(fluid, required), sum));
					return;
				}
			}
		} else progress = true;
	}

	public int waiting(FluidStack stack) {
		if (crafter.amountRequired(this, stack) == 0)
			return 0;
		int i = 0;
		for (ProcessFluid process: sub)
			if (this.crafter.itemsIdentical(process.getOutput(), stack))
				i += process.start.amount - process.getOutput().amount;
		for (FluidStack fluid: fluidStacks)
			if (this.crafter.itemsIdentical(fluid, stack))
				i += fluid.amount;
		for (FluidStack fluid : sent)
			if (this.crafter.itemsIdentical(fluid, stack))
				i -= fluid.amount;
		return Math.max(i, 0);
	}

	public int addFluid(FluidStack fluid, boolean fill) {
		int required = crafter.amountRequired(this, fluid) * sum;
		for (FluidStack stack: sent)
			if (this.crafter.itemsIdentical(stack, fluid))
				required -= stack.amount;

		if (required > 0) {
			int i = Math.min(required, fluid.amount);
			if (fill) {
				for (FluidStack sent : sent) {
					if (FluidHelper.isFluidEqual(sent, fluid)) {
						sent.amount += i;
						crafter.getTile().markChunkDirty();
						return i;
					}
				}
				sent.add(FluidUtils.copy(fluid, i));
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
			fluidStacks.appendTag(writeItem(stack));

		NBTTagCompound tag = super.save();
		if (start != null)
			tag.setTag("start", writeItem(start));
		tag.setBoolean("progress", progress);
		tag.setTag("fluidStacks", fluidStacks);
		return tag;
	}

	@Override
	public boolean isTick() {
		return true;
	}

	@Override
	public void removeLeftover(FluidStack leftover) {
		Iterator<FluidStack> iterator = leftovers.iterator();
		while (iterator.hasNext()) {
			FluidStack next = iterator.next();
			if (FluidHelper.isFluidEqual(next, leftover)) {
				next.amount -= leftover.amount;
				if (next.amount <= 0)
					iterator.remove();
				return;
			}
		}
	}

	public void send(FluidStack drained) {
		for (FluidStack sent: fluidStacks) {
			if (FluidHelper.isFluidEqual(sent, drained)) {
				sent.amount += drained.amount;
				return;
			}
		}
		fluidStacks.add(drained);
	}

}
