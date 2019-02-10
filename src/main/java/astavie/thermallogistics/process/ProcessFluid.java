package astavie.thermallogistics.process;

import astavie.thermallogistics.attachment.ICrafter;
import astavie.thermallogistics.attachment.IRequester;
import astavie.thermallogistics.util.RequesterReference;
import codechicken.lib.fluid.FluidUtils;
import cofh.core.util.helpers.FluidHelper;
import cofh.thermaldynamics.duct.Attachment;
import cofh.thermaldynamics.duct.fluid.DuctUnitFluid;
import cofh.thermaldynamics.duct.fluid.FluidTankGrid;
import cofh.thermaldynamics.duct.fluid.GridFluid;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;

public class ProcessFluid extends Process<FluidStack> {

	public ProcessFluid(IRequester<FluidStack> requester) {
		super(requester);
	}

	public static boolean checkRequests(IRequester<FluidStack> requester, List<Request<FluidStack>> requests, BiFunction<IRequester<FluidStack>, IRequester<FluidStack>, List<FluidStack>> function) {
		boolean changed = false;
		for (Iterator<Request<FluidStack>> iterator = requests.iterator(); iterator.hasNext(); ) {
			Request<FluidStack> request = iterator.next();
			if (request.attachment.isLoaded()) {
				IRequester<FluidStack> attachment = request.attachment.getAttachment();
				if (attachment == null || attachment.isDisabled()) {
					iterator.remove();
					requester.markDirty();
				} else {
					List<FluidStack> list = function.apply(attachment, requester);

					a:
					for (Iterator<FluidStack> iterator1 = request.stacks.iterator(); iterator1.hasNext(); ) {
						FluidStack stack = iterator1.next();
						for (FluidStack compare : list) {
							if (FluidHelper.isFluidEqual(stack, compare)) {
								if (stack.amount > compare.amount) {
									stack.amount = compare.amount;
									changed = true;
									requester.markDirty();
								}
								continue a;
							}
						}

						iterator1.remove();
						requester.markDirty();
					}

					if (request.stacks.isEmpty()) {
						iterator.remove();
						requester.markDirty();
					}
				}
			}
		}
		return changed;
	}

	@Override
	public NBTTagList writeNbt() {
		NBTTagList requests = new NBTTagList();
		for (Request<FluidStack> request : this.requests)
			requests.appendTag(RequestFluid.writeNBT(request));
		return requests;
	}

	@Override
	public void readNbt(NBTTagList nbt) {
		requests.clear();
		for (int i = 0; i < nbt.tagCount(); i++)
			requests.add(RequestFluid.readNBT(nbt.getCompoundTagAt(i)));
	}

	@Override
	public void tick() {
		// Check requests
		checkRequests(requester, requests, IRequester::getOutputTo);

		TileEntity tile = requester.getCachedTile();
		if (tile == null || !tile.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, EnumFacing.byIndex(requester.getSide() ^ 1)))
			return;

		IFluidHandler myHandler = tile.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, EnumFacing.byIndex(requester.getSide() ^ 1));
		if (myHandler == null)
			return;

		GridFluid grid = ((DuctUnitFluid) requester.getDuct()).getGrid();
		FluidTankGrid tank = grid.myTank;

		int maxInput = (int) Math.ceil(tank.fluidThroughput * requester.getThrottle());
		int c = maxInput;

		for (Iterator<Request<FluidStack>> iterator = requests.iterator(); iterator.hasNext() && maxInput > 0; ) {
			Request<FluidStack> request = iterator.next();
			if (!request.attachment.isLoaded())
				continue;

			IRequester<FluidStack> requester = request.attachment.getAttachment();
			if (requester == null)
				continue;

			DuctUnitFluid endPoint = (DuctUnitFluid) requester.getDuct();
			int side = requester.getSide();

			DuctUnitFluid.Cache cache = endPoint.tileCache[side];
			if (cache == null || !endPoint.isOutput(side) && !endPoint.isInput(side))
				continue;

			IFluidHandler handler = cache.getHandler(side ^ 1);
			if (handler == null)
				continue;

			FluidStack drainFluid = handler.drain(maxInput, false);
			if (drainFluid == null)
				continue;

			int input = Math.min(tank.fill(drainFluid, false), request.getCount(drainFluid));
			if (input == 0)
				continue;

			input = tank.fill(handler.drain(input, true), true);
			maxInput -= input;

			FluidStack fluid = FluidUtils.copy(drainFluid, input);
			requester.onFinishCrafting(this.requester, fluid);

			request.decreaseStack(fluid);
			if (request.stacks.isEmpty())
				iterator.remove();

			this.requester.markDirty();

			if (grid.toDistribute > 0 && tank.getFluid() != null) {
				GridFluid other = endPoint.getGrid();
				if (other != null)
					((DuctUnitFluid) requester.getDuct()).transfer(side, Math.min(other.myTank.getFluid().amount, other.toDistribute), false, other.myTank.getFluid(), true);
			}
		}

		if (maxInput == 0)
			return;

		// Check fluids
		List<ICrafter<FluidStack>> crafters = NonNullList.create();

		for (Iterator<DuctUnitFluid> iterator = grid.nodeSet.iterator(); iterator.hasNext() && maxInput > 0; ) {
			DuctUnitFluid fluidDuct = iterator.next();
			for (int k = 0; k < 6 && maxInput > 0; k++) {
				int side = (k + fluidDuct.internalSideCounter) % 6;

				DuctUnitFluid.Cache cache = fluidDuct.tileCache[side];
				if (cache == null)
					continue;

				Attachment attachment = fluidDuct.parent.getAttachment(side);
				if (attachment instanceof ICrafter)
					//noinspection unchecked
					crafters.add((ICrafter<FluidStack>) attachment);

				if (attachment != null && !attachment.canSend())
					continue;

				if (cache.tile instanceof ICrafter)
					//noinspection unchecked
					crafters.add((ICrafter<FluidStack>) cache.tile);

				if (!fluidDuct.isOutput(side) && !fluidDuct.isInput(side))
					continue;

				IFluidHandler handler = cache.getHandler(side ^ 1);
				if (handler == null || handler.equals(myHandler))
					continue;

				FluidStack drainFluid = handler.drain(maxInput, false);
				if (drainFluid == null)
					continue;

				int input = tank.fill(drainFluid, false);
				if (requester.amountRequired(drainFluid) == 0)
					continue;

				maxInput -= tank.fill(handler.drain(input, true), true);
				if (grid.toDistribute > 0 && tank.getFluid() != null) {
					GridFluid other = fluidDuct.getGrid();
					if (other != null)
						((DuctUnitFluid) requester.getDuct()).transfer(side, Math.min(other.myTank.getFluid().amount, other.toDistribute), false, other.myTank.getFluid(), true);
				}
			}
		}

		if (maxInput != c || tank.getFluid() != null || requester.getDuct().world().getTotalWorldTime() % requester.tickDelay() != 0)
			return;

		// Check crafters
		for (ICrafter<FluidStack> crafter : crafters) {
			if (crafter == requester || crafter.isDisabled())
				continue;

			Set<RequesterReference<FluidStack>> blacklist = crafter.getBlacklist();
			if (blacklist.stream().anyMatch(reference -> reference.references(requester)))
				continue;

			for (FluidStack stack : crafter.getOutputs()) {
				int amount = requester.amountRequired(stack);
				if (amount == 0)
					continue;

				stack = stack.copy();
				stack.amount = amount;

				if (!crafter.request(requester, stack))
					continue;

				// No turning back now
				requester.markDirty();

				for (Request<FluidStack> request : requests) {
					if (request.attachment.references(crafter)) {
						request.addStack(stack);
						request.blacklist.addAll(blacklist);
						return;
					}
				}

				Request<FluidStack> request = new RequestFluid(crafter.getReference(), stack);
				request.blacklist.add(crafter.getReference());
				request.blacklist.addAll(blacklist);

				requests.add(request);
				return;
			}
		}
	}

}
