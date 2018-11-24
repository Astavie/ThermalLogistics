package astavie.thermallogistics.util.request;

import astavie.thermallogistics.util.IRequester;
import astavie.thermallogistics.util.delegate.IDelegate;
import cofh.core.network.PacketBase;
import cofh.thermaldynamics.duct.tiles.DuctUnit;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import javax.annotation.Nullable;
import java.util.List;

public interface IRequest<T extends DuctUnit<T, ?, ?>, I> extends Comparable<IRequest<T, I>> {

	static <T extends DuctUnit<T, ?, ?>, I> NBTTagCompound writeNbt(IRequest<T, I> request, IDelegate<I> delegate) {
		NBTTagCompound tag = new NBTTagCompound();
		tag.setLong("age", request.getAge());
		if (request.getStart() != null)
			tag.setTag("start", IRequester.writeNbt(request.getStart(), true));

		NBTTagList list = new NBTTagList();
		for (I stack : request.getStacks())
			list.appendTag(delegate.writeNbt(stack));

		tag.setTag("stacks", list);
		return tag;
	}

	static <T extends DuctUnit<T, ?, ?>, I> void writePacket(IRequest<T, I> request, IDelegate<I> delegate, PacketBase packet) {
		packet.addLong(request.getAge());
		packet.addBool(request.getStart() != null);
		if (request.getStart() != null)
			IRequester.writePacket(packet, request.getStart());

		packet.addInt(request.getStacks().size());
		for (I stack : request.getStacks())
			delegate.writePacket(packet, stack);
	}

	List<I> getStacks();

	@Nullable
	IRequester<T, I> getStart();

	long getAge();

	@Override
	default int compareTo(IRequest<T, I> o) {
		return Long.compare(getAge(), o.getAge());
	}

}
