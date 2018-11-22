package astavie.thermallogistics.process;

import astavie.thermallogistics.ThermalLogistics;
import astavie.thermallogistics.attachment.Crafter;
import astavie.thermallogistics.event.EventHandler;
import astavie.thermallogistics.util.IProcessHolder;
import astavie.thermallogistics.util.IRequester;
import astavie.thermallogistics.util.delegate.IDelegate;
import astavie.thermallogistics.util.delegate.IDelegateClient;
import astavie.thermallogistics.util.request.IRequest;
import astavie.thermallogistics.util.request.Request;
import cofh.thermaldynamics.duct.tiles.DuctUnit;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nullable;
import java.util.*;

public abstract class Process<C extends IProcessHolder<P, T, I>, P extends IProcess<P, T, I>, T extends DuctUnit<T, ?, ?>, I> implements IProcess<P, T, I> {

	protected final C crafter;
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
		this.crafter = crafter;
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
		this.crafter = (C) IProcessHolder.read(world, tag.getCompoundTag("crafter"));
		this.output = tag.hasKey("output") ? getDelegate().readNbt(tag.getCompoundTag("output")) : null;
		this.input = new Request<>(world, getDelegate(), tag.getCompoundTag("input"));
		this.birth = tag.getLong("birth");
		this.sum = tag.getInteger("sum");

		//noinspection unchecked
		crafter.addProcess((P) this, tag.getInteger("index"));
		crafter.getTile().markChunkDirty();

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
		return crafter.getBase();
	}

	@Override
	public long getAge() {
		return crafter.getTile().getWorld().getTotalWorldTime() - birth;
	}

	@Override
	public Collection<IRequest<T, I>> getRequests() {
		Collection<IRequest<T, I>> collection = new ArrayList<>(sub);
		collection.addAll(leftovers);
		return collection;
	}

	@Override
	public IDelegate<I> getDelegate() {
		return crafter.getDelegate();
	}

	@Override
	public IDelegateClient<I, ?> getClientDelegate() {
		return crafter.getClientDelegate();
	}

	@Override
	public ItemStack getDisplayStack() {
		return crafter.getDisplayStack();
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

	public C getCrafter() {
		return crafter;
	}

	@Override
	public boolean hasFailed() {
		return failed || crafter.isInvalid() || (destination != null && destination.isInvalid());
	}

	@Override
	public void setFailed() {
		failed = true;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void update() {
		if (getDuct().getGrid() == null)
			getDuct().formGrid();

		if (isTick()) {
			updateInput();
			crafter.getTile().markChunkDirty();
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

		//noinspection SuspiciousMethodCalls, unchecked
		crafter.removeProcess((P) this);
		EventHandler.PROCESSES.remove(this);

		linked.forEach(IProcess::remove);
		sub.forEach(IProcess::remove);
	}

	@Override
	public boolean isRemoved() {
		return removed;
	}

	@Override
	public boolean isInvalid() {
		//noinspection SuspiciousMethodCalls
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
		return crafter.getDuct();
	}

	@Override
	public int getIndex() {
		//noinspection SuspiciousMethodCalls
		return crafter.getProcesses().indexOf(this);
	}

	@Override
	public byte getSide() {
		return crafter.getSide();
	}

	@Override
	public int getType() {
		return crafter.getType();
	}

	@Override
	public boolean isLoaded() {
		return crafter.getTile().getWorld().isBlockLoaded(crafter.getTile().getPos());
	}

	public int getSum() {
		return sum;
	}

}
