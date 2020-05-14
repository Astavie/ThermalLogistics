package astavie.thermallogistics.process;

import astavie.thermallogistics.attachment.ICrafter;
import astavie.thermallogistics.util.Snapshot;
import astavie.thermallogistics.util.StackHandler;
import astavie.thermallogistics.util.collection.FluidList;
import astavie.thermallogistics.util.collection.StackList;
import astavie.thermallogistics.util.type.FluidType;
import astavie.thermallogistics.util.type.Type;
import cofh.thermaldynamics.duct.Attachment;
import cofh.thermaldynamics.duct.fluid.DuctUnitFluid;
import cofh.thermaldynamics.duct.fluid.FluidTankGrid;
import cofh.thermaldynamics.duct.fluid.GridFluid;
import cofh.thermaldynamics.duct.tiles.DuctUnit;
import cofh.thermaldynamics.util.ListWrapper;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import org.apache.commons.lang3.tuple.Pair;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

public class ProcessFluid extends Process<FluidStack> {

	public ProcessFluid(IProcessRequesterFluid requester) {
		super(requester);
	}

	private static FluidStack drain(FluidTankGrid tank, int maxPull, IFluidHandler inv, Function<Type<FluidStack>, Long> required, IFluidHandler ownHandler) {
		for (IFluidTankProperties properties : inv.getTankProperties()) {
			FluidStack item = properties.getContents();
			if (item == null)
				continue;

			// Check how much we want to drain
			FluidType type = new FluidType(item);
			int amount = (int) Math.min(maxPull, required.apply(type));
			if (amount == 0)
				continue;

			// Check how much we want to fill
			StackHandler.SIM = true;
			amount = ownHandler.fill(type.withAmount(amount), false);
			StackHandler.SIM = false;
			if (amount == 0)
				continue;

			// Check how much we can drain
			item = inv.drain(type.withAmount(amount), false);
			if (item == null)
				continue;

			// Check how much we can fill
			int input = tank.fill(item, false);
			if (input == 0)
				continue;

			// No turning back now
			return type.withAmount(tank.fill(inv.drain(type.withAmount(input), true), true));
		}

		return null;
	}

	private FluidTankGrid getTank() {
		return ((DuctUnitFluid) requester.getDuct()).getGrid().myTank;
	}

	private int getMaxPull(FluidTankGrid tank) {
		return (int) Math.ceil(tank.fluidThroughput * ((IProcessRequesterFluid) requester).throttle());
	}

	@Override
	protected boolean updateRetrieval(StackList<FluidStack> requests) {
		DuctUnitFluid.Cache ownCache = ((DuctUnitFluid) requester.getDuct()).tileCache[requester.getSide() ^ 1];
		if (ownCache == null)
			return false;

		IFluidHandler ownHandler = ownCache.getHandler(requester.getSide());
		if (ownHandler == null)
			return false;

		ListWrapper<Pair<DuctUnit, Byte>> sources = requester.getSources();
		for (Pair<DuctUnit, Byte> source : sources) {
			DuctUnitFluid endPoint = (DuctUnitFluid) source.getLeft();
			byte side = source.getRight();

			Attachment attachment = endPoint.parent.getAttachment(side);
			if (attachment != null && !attachment.canSend())
				continue;

			DuctUnitFluid.Cache cache = endPoint.tileCache[side];
			if (cache == null)
				continue;

			if ((!endPoint.isInput(side) && !endPoint.isOutput(side)))
				continue;

			IFluidHandler inv = cache.getHandler(side ^ 1);
			if (inv == null || inv.equals(ownHandler))
				continue;

			FluidTankGrid tank = getTank();
			FluidStack extract = drain(tank, getMaxPull(tank), inv, requests::amount, ownHandler);
			if (extract != null) {
				sources.advanceCursor();
				return true;
			}
		}

		return false;
	}

	@Override
	protected boolean updateWants() {
		// Check if there are interesting fluids
		FluidList stacks = Snapshot.INSTANCE.getFluids((GridFluid) requester.getDuct().getGrid());
		if (stacks.types().stream().anyMatch(type -> stacks.amount(type) > 0 && requester.amountRequired(type) > 0)) {

			// Try items
			DuctUnitFluid.Cache ownCache = ((DuctUnitFluid) requester.getDuct()).tileCache[requester.getSide() ^ 1];
			if (ownCache == null)
				return false;

			IFluidHandler ownHandler = ownCache.getHandler(requester.getSide());
			if (ownHandler == null)
				return false;

			ListWrapper<Pair<DuctUnit, Byte>> sources = requester.getSources();

			for (Pair<DuctUnit, Byte> source : sources) {
				DuctUnitFluid endPoint = (DuctUnitFluid) source.getLeft();
				byte side = source.getRight();

				Attachment attachment = endPoint.parent.getAttachment(side);
				if (attachment != null && !attachment.canSend())
					continue;

				DuctUnitFluid.Cache cache = endPoint.tileCache[side];
				if (cache == null)
					continue;

				if ((!endPoint.isInput(side) && !endPoint.isOutput(side)))
					continue;

				IFluidHandler inv = cache.getHandler(side ^ 1);
				if (inv == null || inv.equals(ownHandler))
					continue;

				FluidTankGrid tank = getTank();
				FluidStack extract = drain(tank, getMaxPull(tank), inv, requester::amountRequired, ownHandler);
				if (extract != null) {
					sources.advanceCursor();
					return true;
				}
			}

			// Let's not do this every tick
		} else if (requester.getDuct().world().getTotalWorldTime() % ((IProcessRequesterFluid) requester).tickDelay() == 0) {

			// Try crafters
			Proposal<FluidStack> proposal = new Proposal<>(null, null, 0);
			requestFirstRequester(proposal, type -> Math.min(requester.amountRequired(type), ((IProcessRequesterFluid) requester).maxSize()), false);

			List<Request<FluidStack>> requests = new LinkedList<>();
			for (Proposal<FluidStack> prop : proposal.children) {
				requests.add(new Request<>(prop.type, prop.amount, new Source<>(requester.getSide(), prop.me), 0));
			}

			if (requests.size() > 0) {
				for (Request<FluidStack> request : requests) {
					requester.addRequest(request);
				}
				return true;
			}

		}

		return false;
	}

	@Override
	protected boolean attemptPull(ICrafter<FluidStack> crafter, StackList<FluidStack> stacks) {
		DuctUnitFluid.Cache ownCache = ((DuctUnitFluid) requester.getDuct()).tileCache[requester.getSide() ^ 1];
		if (ownCache == null)
			return false;

		IFluidHandler ownHandler = ownCache.getHandler(requester.getSide());
		if (ownHandler == null)
			return false;

		DuctUnitFluid duct = (DuctUnitFluid) crafter.getDuct();
		if (duct == null)
			return false;

		byte side = crafter.getSide();

		DuctUnitFluid.Cache cache = duct.tileCache[side ^ 1];
		if (cache == null)
			return false;

		IFluidHandler inv = cache.getHandler(side);
		if (inv == null)
			return false;

		FluidTankGrid tank = getTank();
		FluidStack extract = drain(tank, getMaxPull(tank), inv, stacks::amount, ownHandler);
		if (extract != null) {
			crafter.finish(requester, new FluidType(extract), extract.amount);
			requester.onCrafterSend(crafter, new FluidType(extract), extract.amount);

			return true;
		}

		return false;
	}

	@Override
	public void findCrafter(Predicate<ICrafter<FluidStack>> predicate, boolean advanceCursor) {
		ListWrapper<Pair<DuctUnit, Byte>> sources = requester.getSources();
		for (Pair<DuctUnit, Byte> source : sources) {
			DuctUnitFluid fluidDuct = (DuctUnitFluid) source.getLeft();
			byte side = source.getRight();

			Attachment attachment = fluidDuct.parent.getAttachment(side);
			if (attachment != null && StackHandler.forEachCrafter(attachment, predicate)) {
				if (advanceCursor) {
					sources.advanceCursor();
				}
				break;
			}

			DuctUnitFluid.Cache cache = fluidDuct.tileCache[side];
			if (cache == null) {
				if (advanceCursor) {
					sources.advanceCursor();
				}
				continue;
			}

			if (StackHandler.forEachCrafter(cache.tile, predicate)) {
				if (advanceCursor) {
					sources.advanceCursor();
				}
				break;
			}

			if (advanceCursor) {
				// Always advance the cursor so Round-Robin works well
				sources.advanceCursor();
			}
		}
	}

}
