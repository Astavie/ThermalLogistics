package astavie.thermallogistics.util.request;

import astavie.thermallogistics.util.IRequester;
import astavie.thermallogistics.util.delegate.IDelegate;
import cofh.core.network.PacketBase;
import cofh.thermaldynamics.duct.tiles.DuctUnit;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class Request<T extends DuctUnit<T, ?, ?>, I> implements IRequest<T, I> {

	private final World world;
	private final IRequester<T, I> start;
	private final List<I> stacks = NonNullList.create();

	private long birth;

	public Request(World world, @Nullable IRequester<T, I> start, Collection<I> stacks) {
		//noinspection unchecked
		this.world = world;
		this.birth = world.getTotalWorldTime();
		this.start = start;
		this.stacks.addAll(stacks);
	}

	public Request(World world, @Nullable IRequester<T, I> start, I stack) {
		this(world, start, Collections.singleton(stack));
	}

	public Request(World world, IDelegate<I> delegate, NBTTagCompound tag) {
		this.world = world;
		this.birth = world.getTotalWorldTime() - tag.getLong("birth");

		if (tag.hasKey("start"))
			this.start = IRequester.read(world, tag.getCompoundTag("start"));
		else
			this.start = null;

		NBTTagList list = tag.getTagList("stacks", Constants.NBT.TAG_COMPOUND);
		for (int i = 0; i < list.tagCount(); i++)
			stacks.add(delegate.readNbt(list.getCompoundTagAt(i)));
	}

	public Request(World world, IDelegate<I> delegate, PacketBase packet) {
		this.world = world;
		this.birth = world.getTotalWorldTime() - packet.getLong();

		if (packet.getBool()) {
			NBTTagCompound tag = null;
			try {
				tag = packet.readNBT();
			} catch (IOException e) {
				e.printStackTrace();
			}
			this.start = IRequester.read(world, tag);
		} else this.start = null;

		int size = packet.getInt();
		for (int i = 0; i < size; i++)
			stacks.add(delegate.readPacket(packet));
	}

	@Override
	public List<I> getStacks() {
		return stacks;
	}

	@Override
	@Nullable
	public IRequester<T, I> getStart() {
		return start;
	}

	@Override
	public long getAge() {
		//noinspection unchecked
		return world.getTotalWorldTime() - birth;
	}

	public Request<T, I> copy(IDelegate<I> delegate) {
		Request<T, I> clone = new Request<>(world, start, stacks.stream().map(delegate::copy).collect(Collectors.toList()));
		clone.birth = birth;
		return clone;
	}

}
