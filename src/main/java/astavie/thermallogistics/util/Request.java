package astavie.thermallogistics.util;

import cofh.thermaldynamics.duct.tiles.DuctUnit;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class Request<T extends DuctUnit<T, ?, ?>, I> implements Iterable<I> {

	private final long birth;

	private final IDestination<T, I> start;
	private final IDestination<T, I> end;

	private final List<I> stacks = NonNullList.create();

	public Request(@Nullable IDestination<T, I> start, IDestination<T, I> end, Collection<I> stacks) {
		//noinspection unchecked
		this.birth = end.getDuct().world().getTotalWorldTime();
		this.start = start;
		this.end = end;
		this.stacks.addAll(stacks);
	}

	public Request(@Nullable IDestination<T, I> start, IDestination<T, I> end, I stack) {
		this(start, end, Collections.singleton(stack));
	}

	public Request(World world, NBTTagCompound tag) {
		this.birth = tag.getLong("birth");
		if (tag.hasKey("start"))
			this.start = IDestination.read(world, tag.getCompoundTag("start"));
		else
			this.start = null;
		this.end = IDestination.read(world, tag.getCompoundTag("end"));

		NBTTagList list = tag.getTagList("stacks", Constants.NBT.TAG_COMPOUND);
		for (int i = 0; i < list.tagCount(); i++)
			stacks.add(end.getDelegate().readStack(list.getCompoundTagAt(i)));
	}

	public NBTTagCompound write() {
		NBTTagCompound tag = new NBTTagCompound();
		tag.setLong("birth", birth);
		if (start != null)
			tag.setTag("start", IDestination.write(start));
		tag.setTag("end", IDestination.write(end));

		NBTTagList list = new NBTTagList();
		for (I stack : stacks)
			list.appendTag(end.getDelegate().writeStack(stack));

		tag.setTag("stacks", list);
		return tag;
	}

	public void addStack(I stack) {
		this.stacks.add(stack);
	}

	@Override
	public Iterator<I> iterator() {
		return stacks.iterator();
	}

	@Nullable
	public IDestination<T, I> getStart() {
		return start;
	}

	public IDestination<T, I> getEnd() {
		return end;
	}

	public long getAge() {
		//noinspection unchecked
		return end.getDuct().world().getTotalWorldTime() - birth;
	}

}
