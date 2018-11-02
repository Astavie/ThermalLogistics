package astavie.thermallogistics.process;

import astavie.thermallogistics.ThermalLogistics;
import astavie.thermallogistics.attachment.Crafter;
import astavie.thermallogistics.event.EventHandler;
import astavie.thermallogistics.util.IDestination;
import astavie.thermallogistics.util.IProcessHolder;
import cofh.thermaldynamics.duct.tiles.DuctUnit;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;

import java.util.HashSet;
import java.util.Set;

public abstract class Process<C extends IProcessHolder<P, T, I>, P extends IProcess<P, T, I>, T extends DuctUnit<T, ?, ?>, I> implements IProcess<P, T, I>, IDestination<T, I> {

	protected final C crafter;
	protected final I output;

	protected final Set<IProcess> linked = new HashSet<>();
	protected final Set<P> sub = new HashSet<>();

	protected final Set<I> sent = new HashSet<>();
	protected final Set<I> leftovers = new HashSet<>();

	protected final int sum;

	private final Set<IProcess> dependent = new HashSet<>();

	protected IDestination<T, I> destination;
	protected boolean failed = false;

	private boolean removed = false;

	public Process(IDestination<T, I> destination, C crafter, I output, int sum) {
		this.destination = destination;
		this.crafter = crafter;
		this.output = output;
		this.sum = sum;

		//noinspection unchecked
		crafter.addProcess((P) this);
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
		this.output = tag.hasKey("output") ? readItem(tag.getCompoundTag("output")) : null;
		this.sum = tag.getInteger("sum");

		//noinspection unchecked
		crafter.addProcess((P) this);
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

		NBTTagList sent = tag.getTagList("sent", Constants.NBT.TAG_COMPOUND);
		for (int i = 0; i < sent.tagCount(); i++)
			this.sent.add(readItem(sent.getCompoundTagAt(i)));

		NBTTagList leftovers = tag.getTagList("leftovers", Constants.NBT.TAG_COMPOUND);
		for (int i = 0; i < leftovers.tagCount(); i++)
			this.leftovers.add(readItem(leftovers.getCompoundTagAt(i)));
	}

	@Override
	public void addDependent(IProcess other) {
		dependent.add(other);
	}

	@Override
	public void fail() {
		dependent.forEach(IProcess::setFailed);
	}

	protected abstract I readItem(NBTTagCompound tag);

	protected abstract NBTTagCompound writeItem(I output);

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
		NBTTagCompound c = crafter.write();

		NBTTagList linked = new NBTTagList();
		for (IProcess process : this.linked)
			linked.appendTag(process.save());

		NBTTagList sub = new NBTTagList();
		for (IProcess<P, T, I> process : this.sub)
			sub.appendTag(process.save());

		NBTTagList sent = new NBTTagList();
		for (I stack : this.sent)
			sent.appendTag(writeItem(stack));

		NBTTagList leftovers = new NBTTagList();
		for (I stack : this.leftovers)
			leftovers.appendTag(writeItem(stack));

		NBTTagCompound tag = new NBTTagCompound();
		tag.setString("id", getId().toString());
		tag.setTag("crafter", c);
		if (output != null)
			tag.setTag("output", writeItem(output));
		tag.setInteger("sum", sum);
		tag.setTag("linked", linked);
		tag.setTag("sub", sub);
		tag.setTag("sent", sent);
		tag.setTag("leftovers", leftovers);
		return tag;
	}

	@Override
	public P setDestination(IDestination<T, I> destination) {
		this.destination = destination;
		//noinspection unchecked
		return (P) this;
	}

	@Override
	public T getDuct() {
		return crafter.getDuct();
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

}
