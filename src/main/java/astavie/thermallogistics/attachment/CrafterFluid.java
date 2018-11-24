package astavie.thermallogistics.attachment;

import astavie.thermallogistics.ThermalLogistics;
import astavie.thermallogistics.compat.ICrafterWrapper;
import astavie.thermallogistics.event.EventHandler;
import astavie.thermallogistics.process.ProcessFluid;
import astavie.thermallogistics.util.IRequester;
import astavie.thermallogistics.util.NetworkUtils;
import astavie.thermallogistics.util.delegate.DelegateClientFluid;
import astavie.thermallogistics.util.delegate.DelegateFluid;
import codechicken.lib.fluid.FluidUtils;
import cofh.core.network.PacketBase;
import cofh.core.network.PacketHandler;
import cofh.core.util.helpers.FluidHelper;
import cofh.thermaldynamics.duct.attachments.filter.IFilterFluid;
import cofh.thermaldynamics.duct.attachments.filter.IFilterItems;
import cofh.thermaldynamics.duct.attachments.servo.ServoFluid;
import cofh.thermaldynamics.duct.fluid.DuctUnitFluid;
import cofh.thermaldynamics.duct.fluid.FluidTankGrid;
import cofh.thermaldynamics.duct.tiles.DuctToken;
import cofh.thermaldynamics.duct.tiles.TileGrid;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.util.*;

public class CrafterFluid extends Crafter<ProcessFluid, DuctUnitFluid, FluidStack> {

	public static final ResourceLocation ID = new ResourceLocation(ThermalLogistics.MODID, "crafter_fluid");

	public final List<FluidStack> leftovers = new LinkedList<>();
	public final List<Pair<FluidStack, IRequester<DuctUnitFluid, FluidStack>>> registry = new LinkedList<>();

	private final IFilterFluid filter = new FilterFluid();
	public FluidStack[] inputs;
	public FluidStack[] outputs;

	private NBTTagList _registry;

	public CrafterFluid(TileGrid tile, byte side) {
		super(tile, side);
	}

	public CrafterFluid(TileGrid tile, byte side, int type) {
		super(tile, side, type);

		int max = type + 1;
		inputs = new FluidStack[max * 2];
		outputs = new FluidStack[max];
	}

	@Override
	public ProcessFluid createLinkedProcess(int sum) {
		return new ProcessFluid(null, this, null, sum);
	}

	@Override
	public int amountRequired(FluidStack item) {
		int amt = 0;
		for (FluidStack input : inputs)
			if (input != null && itemsIdentical(item, input))
				amt += input.amount;
		return amt;
	}

	@Override
	public boolean itemsIdentical(FluidStack a, FluidStack b) {
		return a.getFluid() == b.getFluid() && (values[2] || FluidStack.areFluidStackTagsEqual(a, b));
	}

	@Override
	public DelegateFluid getDelegate() {
		return DelegateFluid.INSTANCE;
	}

	@Override
	public DelegateClientFluid getClientDelegate() {
		return DelegateClientFluid.INSTANCE;
	}

	@Override
	public FluidStack[] getInputs() {
		return inputs;
	}

	@Override
	public Collection<FluidStack> getInputs(ProcessFluid process) {
		Collection<FluidStack> inputs = new LinkedList<>();
		a:
		for (FluidStack fluid : this.inputs) {
			if (fluid == null || fluid.amount == 0)
				continue;
			for (FluidStack input : inputs) {
				if (FluidHelper.isFluidEqual(fluid, input)) {
					input.amount += fluid.amount * process.getSum();
					continue a;
				}
			}
			inputs.add(FluidUtils.copy(fluid, fluid.amount * process.getSum()));
		}
		return inputs;
	}

	@Override
	public FluidStack[] getOutputs() {
		return outputs;
	}

	public PacketBase getPacket(FluidStack stack, boolean input, int slot) {
		PacketBase packet = getNewPacket();
		packet.addByte(6);
		packet.addFluidStack(stack);
		packet.addBool(input);
		packet.addInt(slot);
		return packet;
	}

	@Override
	protected boolean isValidTile(TileEntity tile) {
		return tile.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, EnumFacing.VALUES[side ^ 1]);
	}

	@Override
	protected void setAutoInput(ICrafterWrapper wrapper) {
		Arrays.fill(inputs, null);
		List<FluidStack> list = wrapper.getInputs(FluidStack.class);
		for (int i = 0; i < list.size(); i++) {
			if (i >= inputs.length)
				break;
			inputs[i] = list.get(i);
		}
	}

	@Override
	protected void setAutoOutput(ICrafterWrapper wrapper) {
		Arrays.fill(outputs, null);
		List<FluidStack> list = wrapper.getOutputs(FluidStack.class);
		for (int i = 0; i < list.size(); i++) {
			if (i >= outputs.length)
				break;
			outputs[i] = list.get(i);
		}
	}

	@Override
	protected void handleInfoPacket(byte message, PacketBase payload) {
		switch (message) {
			case 6:
				FluidStack stack = payload.getFluidStack();
				FluidStack[] inventory = payload.getBool() ? inputs : outputs;
				inventory[payload.getInt()] = stack;
				baseTile.markChunkDirty();
				break;
		}
	}

	@Override
	public void readFromNBT(NBTTagCompound tag) {
		super.readFromNBT(tag);

		NBTTagList leftovers = tag.getTagList("leftovers", Constants.NBT.TAG_COMPOUND);
		for (int i = 0; i < leftovers.tagCount(); i++)
			this.leftovers.add(FluidStack.loadFluidStackFromNBT(leftovers.getCompoundTagAt(i)));

		_registry = tag.getTagList("registry", Constants.NBT.TAG_COMPOUND);
		EventHandler.LOADERS.add(this);
	}

	@Override
	public void writeToNBT(NBTTagCompound tag) {
		super.writeToNBT(tag);

		NBTTagList leftovers = new NBTTagList();
		for (FluidStack stack : this.leftovers)
			leftovers.appendTag(stack.writeToNBT(new NBTTagCompound()));

		NBTTagList registry = new NBTTagList();
		for (Pair<FluidStack, IRequester<DuctUnitFluid, FluidStack>> pair : this.registry) {
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
	public void postLoad() {
		if (_registry != null) {
			for (int i = 0; i < _registry.tagCount(); i++) {
				NBTTagCompound tag = _registry.getCompoundTagAt(i);
				NBTTagCompound item = tag.getCompoundTag("item");
				NBTTagCompound destination = tag.getCompoundTag("destination");

				//noinspection unchecked
				this.registry.add(Pair.of(item.isEmpty() ? null : FluidStack.loadFluidStackFromNBT(item), IRequester.readNbt(baseTile.world(), destination)));
			}
			_registry = null;
		}
		super.postLoad();
	}

	public void addLeftover(FluidStack stack) {
		for (FluidStack leftover : leftovers) {
			if (FluidHelper.isFluidEqual(stack, leftover)) {
				leftover.amount += stack.amount;
				baseTile.markChunkDirty();
				return;
			}
		}
		leftovers.add(stack);
		baseTile.markChunkDirty();
	}

	public FluidStack registerLeftover(FluidStack stack, IRequester<DuctUnitFluid, FluidStack> destination, boolean simulate) {
		Iterator<FluidStack> iterator = leftovers.iterator();
		while (iterator.hasNext()) {
			FluidStack next = iterator.next();
			if (FluidHelper.isFluidEqual(next, stack)) {
				int amt = Math.min(next.amount, stack.amount);
				if (!simulate) {
					next.amount -= amt;
					if (next.amount <= 0)
						iterator.remove();
				}

				stack = FluidUtils.copy(stack, amt);
				if (!simulate) {
					registry.add(Pair.of(stack, destination));
					baseTile.markChunkDirty();
				}
				return stack;
			}
		}
		return null;
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
		inputs = new FluidStack[max * 2];
		outputs = new FluidStack[max];

		NBTTagList inputs = tag.getTagList("Inputs", Constants.NBT.TAG_COMPOUND);
		for (int i = 0; i < inputs.tagCount(); i++) {
			NBTTagCompound compound = inputs.getCompoundTagAt(i);
			this.inputs[compound.getInteger("Slot")] = FluidStack.loadFluidStackFromNBT(compound);
		}

		NBTTagList outputs = tag.getTagList("Outputs", Constants.NBT.TAG_COMPOUND);
		for (int i = 0; i < outputs.tagCount(); i++) {
			NBTTagCompound compound = outputs.getCompoundTagAt(i);
			this.outputs[compound.getInteger("Slot")] = FluidStack.loadFluidStackFromNBT(compound);
		}
	}

	@Override
	public void addProcess(ProcessFluid process, int index) {
		super.addProcess(process, index);
		registry.add(Pair.of(null, process));
	}

	@Override
	public void removeProcess(ProcessFluid process) {
		super.removeProcess(process);
		for (Iterator<Pair<FluidStack, IRequester<DuctUnitFluid, FluidStack>>> iterator = registry.iterator(); iterator.hasNext(); )
			if (iterator.next().getRight() == process)
				iterator.remove();
	}

	@Override
	public void tick(int pass) {
		super.tick(pass);
		if (pass == 0 && getDuct().tileCache[side] != null && !(getDuct().tileCache[side] instanceof CacheReplace))
			getDuct().tileCache[side] = new CacheReplace(getDuct().tileCache[side]);

		if (pass == 1) {
			DuctUnitFluid.Cache cache = getDuct().tileCache[side];
			if (cache != null) {
				IFluidHandler handler = cache.getHandler(side ^ 1);
				Iterator<Pair<FluidStack, IRequester<DuctUnitFluid, FluidStack>>> leftoverIterator = registry.iterator();
				while (leftoverIterator.hasNext()) {
					Pair<FluidStack, IRequester<DuctUnitFluid, FluidStack>> leftover = leftoverIterator.next();
					if (leftover.getLeft() == null || !leftover.getRight().isTick())
						break;

					FluidTankGrid myTank = getDuct().getGrid().myTank;
					int maxDrain = Math.min((int) Math.ceil(myTank.fluidThroughput * ServoFluid.throttle[leftover.getRight().getType()]), leftover.getLeft().amount);

					FluidStack drainFluid = handler.drain(maxDrain, false);
					if (!FluidHelper.isFluidEqual(drainFluid, leftover.getLeft()))
						break;

					maxDrain = myTank.fill(drainFluid, false);
					if (maxDrain <= 0)
						break;

					// Alright, let's do this!
					FluidStack drained = FluidUtils.copy(leftover.getLeft(), myTank.fill(handler.drain(maxDrain, true), true));
					if (leftover.getRight() instanceof ProcessFluid)
						((ProcessFluid) leftover.getRight()).send(drained);

					leftover.getRight().removeLeftover(this, drained);
					leftover.getLeft().amount -= drained.amount;
					if (leftover.getLeft().amount <= 0)
						leftoverIterator.remove();
					baseTile.markChunkDirty();
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

	private int onFill(FluidStack fluid, boolean fill) {
		int i = 0;
		for (ProcessFluid process : processes) {
			int amount = process.addFluid(fluid, fill);
			if (amount > 0) {
				i += amount;
				if (amount == fluid.amount)
					break;
				fluid = FluidUtils.copy(fluid, fluid.amount - amount);
			}
		}
		return i;
	}

	@Override
	public Crafter.Cache createCache() {
		return new Cache();
	}

	@Override
	public ResourceLocation getId() {
		return ID;
	}

	@Override
	public DuctToken tickUnit() {
		return DuctToken.FLUID;
	}

	@Override
	public IFilterItems getItemFilter() {
		return null;
	}

	@Override
	public IFilterFluid getFluidFilter() {
		return filter;
	}

	@Override
	public boolean isTick() {
		return true;
	}

	private class FilterFluid implements IFilterFluid {

		@Override
		public boolean allowFluid(FluidStack fluid) {
			for (FluidStack input : inputs)
				if (input != null && itemsIdentical(input, fluid))
					return true;
			return false;
		}

	}

	private class Cache implements Crafter.Cache {

		private final FluidStack[] inputs;
		private final FluidStack[] outputs;

		private Cache() {
			inputs = new FluidStack[CrafterFluid.this.inputs.length];
			outputs = new FluidStack[CrafterFluid.this.outputs.length];
		}

		@Override
		public void detectAndSendChanges(EntityPlayer player) {
			for (int i = 0; i < inputs.length; i++) {
				if (!NetworkUtils.areFluidStacksEqual(inputs[i], CrafterFluid.this.inputs[i])) {
					inputs[i] = CrafterFluid.this.inputs[i];
					PacketHandler.sendTo(getPacket(inputs[i], true, i), player);
				}
			}
			for (int i = 0; i < outputs.length; i++) {
				if (!NetworkUtils.areFluidStacksEqual(outputs[i], CrafterFluid.this.outputs[i])) {
					outputs[i] = CrafterFluid.this.outputs[i];
					PacketHandler.sendTo(getPacket(outputs[i], false, i), player);
				}
			}
		}

	}

	private class CacheReplace extends DuctUnitFluid.Cache {

		public CacheReplace(DuctUnitFluid.Cache cache) {
			super(cache.tile, cache.filter);
		}

		@Override
		public IFluidHandler getHandler(int side) {
			return new FluidHandlerReplace(super.getHandler(side));
		}

	}

	private class FluidHandlerReplace implements IFluidHandler {

		private final IFluidHandler handler;

		public FluidHandlerReplace(IFluidHandler handler) {
			this.handler = handler;
		}

		@Override
		public IFluidTankProperties[] getTankProperties() {
			return handler.getTankProperties();
		}

		@Override
		public int fill(FluidStack resource, boolean doFill) {
			if (!isInvalid()) {
				int amount = handler.fill(resource, false);
				if (amount > 0)
					return handler.fill(FluidUtils.copy(resource, onFill(FluidUtils.copy(resource, amount), doFill)), doFill);
			}
			return handler.fill(resource, doFill);
		}

		@Nullable
		@Override
		public FluidStack drain(FluidStack resource, boolean doDrain) {
			return handler.drain(resource, doDrain);
		}

		@Nullable
		@Override
		public FluidStack drain(int maxDrain, boolean doDrain) {
			return handler.drain(maxDrain, doDrain);
		}

	}

}
