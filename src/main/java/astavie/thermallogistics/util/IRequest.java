package astavie.thermallogistics.util;

import astavie.thermallogistics.util.delegate.IDelegate;
import cofh.core.network.PacketBase;
import cofh.thermaldynamics.duct.tiles.DuctUnit;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Collection;

public interface IRequest<T extends DuctUnit<T, ?, ?>, I> {

	static <T extends DuctUnit<T, ?, ?>, I> NBTTagCompound writeNbt(IRequest<T, I> request, IDelegate<I> delegate) {
		NBTTagCompound tag = new NBTTagCompound();
		tag.setLong("age", request.getAge());
		if (request.getStart() != null)
			tag.setTag("start", IRequester.write(request.getStart()));

		NBTTagList list = new NBTTagList();
		for (I stack : request.getStacks())
			list.appendTag(delegate.writeNbt(stack));

		tag.setTag("stacks", list);
		return tag;
	}

	static <T extends DuctUnit<T, ?, ?>, I> void writePacket(IRequest<T, I> request, IDelegate<I> delegate, PacketBase packet) {
		packet.addLong(request.getAge());
		packet.addBool(request.getStart() != null);
		if (request.getStart() != null) {
			try {
				packet.writeNBT(IRequester.write(request.getStart()));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		packet.addInt(request.getStacks().size());
		for (I stack : request.getStacks())
			delegate.writePacket(packet, stack);
	}

	Collection<I> getStacks();

	@Nullable
	IRequester<T, I> getStart();

	long getAge();

}
