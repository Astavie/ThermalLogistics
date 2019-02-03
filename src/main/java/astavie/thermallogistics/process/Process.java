package astavie.thermallogistics.process;

import astavie.thermallogistics.ThermalLogistics;
import astavie.thermallogistics.attachment.Crafter;
import astavie.thermallogistics.event.EventHandler;
import astavie.thermallogistics.util.IProcessHolder;
import astavie.thermallogistics.util.IRequester;
import astavie.thermallogistics.util.delegate.IDelegate;
import astavie.thermallogistics.util.delegate.IDelegateClient;
import astavie.thermallogistics.util.reference.RequesterReference;
import astavie.thermallogistics.util.request.IRequest;
import astavie.thermallogistics.util.request.Request;
import astavie.thermallogistics.util.request.Requests;
import cofh.thermaldynamics.duct.tiles.DuctUnit;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nullable;
import java.util.*;

public abstract class Process<C extends IProcessHolder<P, T, I>, P extends IProcess<P, T, I>, T extends DuctUnit<T, ?, ?>, I> implements IProcess<P, T, I> {

	protected final RequesterReference<C> crafter;
	protected final I output;

	protected final Set<IProcess> linked = new HashSet<>();
	protected final Set<P> sub = new HashSet<>();

	protected final Request<T, I> input;
	protected final List<IRequest<T, I>> leftovers = new LinkedList<>();

	protected final long birth;
	protected final int sum;

	private final Set<IProcess> dependent = new HashSet<>();

	protected IRequester<T, I> destination;
	protected boolean failed = false;

	private boolean removed = false;

	public Process(@Nullable IRequester<T, I> destination, C crafter, I output, int sum) {
		this.destination = destination;
		this.crafter = new RequesterReference<>(crafter);
		this.output = output;

		this.sum = sum;
		this.birth = crafter.getTile().getWorld().getTotalWorldTime();

		//noinspection unchecked
		this.input = new Request<>(crafter.getTile().getWorld(), crafter, crafter.getInputs((P) this));

		//noinspection unchecked
		crafter.addProcess((P) this, -1);
		crafter.getTile().markChunkDirty();

		EventHandler.PROCESSES.add(this);

		if (destination != null) {
			for (Crafter c : crafter.getLinked()) {
				if (c != crafter) {
					IProcess process = c.createLinkedProcess(sum);
					process.addDependent(this);
					linked.add(process);
				}
			}
		}
	}

	public Process(World world, NBTTagCompound tag) {
		//noinspection unchecked
		C holder = (C) IProcessHolder.read(world, tag.getCompoundTag("crafter"));

		this.crafter = new RequesterReference<>(holder);
		this.output = tag.hasKey("output") ? getDelegate().readNbt(tag.getCompoundTag("output")) : null;
		this.input = new Request<>(world, getDelegate(), tag.getCompoundTag("input"));
		this.birth = tag.getLong("birth");
		this.sum = tag.getInteger("sum");

		//noinspection unchecked
		holder.addProcess((P) this, tag.getInteger("index"));
		holder.getTile().markChunkDirty();

		EventHandler.PROCESSES.add(this);

		NBTTagList linked = tag.getTagList("linked", Constants.NBT.TAG_COMPOUND);
		for (int i = 0; i < linked.tagCount(); i++) {
			IProcess process = ThermalLogistics.readProcess(world, linked.getCompoundTagAt(i));
			process.addDependent(this);
			this.linked.add(process);
		}

		NBTTagList sub = tag.getTagList("sub", Constants.NBT.TAG_COMPOUND);
		for (int i = 0; i < sub.tagCount(); i++)
			//noinspection unchecked
			this.sub.add((P) ThermalLogistics.readProcess(world, sub.getCompoundTagAt(i)).setDestination(this));

		NBTTagList leftovers = tag.getTagList("leftovers", Constants.NBT.TAG_COMPOUND);
		for (int i = 0; i < leftovers.tagCount(); i++)
			this.leftovers.add(new Request<>(world, getDelegate(), leftovers.getCompoundTagAt(i)));
	}

	@Override
	public BlockPos getBase() {
		return crafter.pos;
	}

	@Override
	public long getAge() {
		return crafter.world.getTotalWorldTime() - birth;
	}

	@Override
	public List<Requests<T, I>> getRequests() {
		List<IRequest<T, I>> list = new LinkedList<>();
		for (P process : sub)
			if (!getDelegate().isNull(process.getOutput()))
				list.add(process);
		list.addAll(leftovers);

		if (list.isEmpty())
			return Collections.emptyList();

		Requests<T, I> requests = new Requests<>(this, list);
		requests.condense(crafter.world, getDelegate());
		return Collections.singletonList(requests);
	}

	@Override
	public IDelegate<I> getDelegate() {
		return crafter.getRequester().getDelegate();
	}

	@Override
	public IDelegateClient<I, ?> getClientDelegate() {
		return crafter.getRequester().getClientDelegate();
	}

	@Override
	public void addDependent(IProcess other) {
		dependent.add(other);
	}

	@Override
	public void fail() {
		dependent.forEach(IProcess::setFailed);
	}

	protected abstract ResourceLocation getId();

	public abstract boolean isStuck();

	public RequesterReference<C> getCrafter() {
		return crafter;
	}

	@Override
	public boolean hasFailed() {
		return failed || crafter.getRequester() == null || (destination != null && destination.isInvalid());
	}

	@Override
	public void setFailed() {
		failed = true;
	}

	@Override
	public void update() {
		if (getDuct().getGrid() == null)
			getDuct().formGrid();

		if (isTick()) {
			updateInput();
			crafter.getRequester().getTile().markChunkDirty();
		}
		if (destination != null && destination.isTick() && !hasFailed()) {
			updateOutput();
			destination.getDuct().parent.markChunkDirty();
		}
	}

	protected abstract void updateOutput();

	protected abstract void updateInput();

	@Override
	public boolean isDone() {
		return sub.stream().allMatch(IProcess::isDone) && linked.stream().allMatch(IProcess::isDone) && (destination == null || getDelegate().isNull(output)) && input.getStacks().isEmpty();
	}

	@Override
	public I getOutput() {
		return output;
	}

	@Override
	public void remove() {
		removed = true;

		//noinspection unchecked
		crafter.getRequester().removeProcess((P) this);
		EventHandler.PROCESSES.remove(this);

		linked.forEach(IProcess::remove);
		sub.forEach(IProcess::remove);
	}

	@Override
	public void unload() {
		EventHandler.PROCESSES.remove(this);

		linked.forEach(IProcess::unload);
		sub.forEach(IProcess::unload);
	}

	@Override
	public boolean isRemoved() {
		return removed;
	}

	@Override
	public boolean isInvalid() {
		return hasFailed() || isRemoved();
	}

	@Override
	public NBTTagCompound save() {
		NBTTagCompound c = IProcessHolder.write(crafter);

		NBTTagList linked = new NBTTagList();
		for (IProcess process : this.linked)
			linked.appendTag(process.save());

		NBTTagList sub = new NBTTagList();
		for (IProcess<P, T, I> process : this.sub)
			sub.appendTag(process.save());

		NBTTagList leftovers = new NBTTagList();
		for (IRequest<T, I> stack : this.leftovers)
			leftovers.appendTag(IRequest.writeNbt(stack, getDelegate()));

		NBTTagCompound tag = new NBTTagCompound();
		tag.setString("id", getId().toString());
		tag.setTag("crafter", c);
		tag.setInteger("index", getIndex());
		if (output != null)
			tag.setTag("output", getDelegate().writeNbt(output));

		tag.setTag("input", IRequest.writeNbt(input, getDelegate()));
		tag.setLong("birth", birth);
		tag.setInteger("sum", sum);
		tag.setTag("linked", linked);
		tag.setTag("sub", sub);
		tag.setTag("leftovers", leftovers);
		return tag;
	}

	@Override
	public P setDestination(IRequester<T, I> destination) {
		this.destination = destination;
		//noinspection unchecked
		return (P) this;
	}

	@Override
	public T getDuct() {
		return crafter.getRequester().getDuct();
	}

	@Override
	public int getIndex() {
		//noinspection SuspiciousMethodCalls
		return crafter.getRequester().getProcesses().indexOf(this);
	}

	@Override
	public byte getSide() {
		return crafter.side;
	}

	@Override
	public int getType() {
		return crafter.getRequester().getType();
	}

	@Override
	public boolean isLoaded() {
		return crafter.isLoaded();
	}

	@Override
	public boolean shouldUnload() {
		return destination != null && !(destination instanceof IProcess) && !destination.getDuct().world().isBlockLoaded(destination.getBase());
	}

	public int getSum() {
		return sum;
	}

}
