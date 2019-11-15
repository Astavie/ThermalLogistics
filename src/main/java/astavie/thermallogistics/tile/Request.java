package astavie.thermallogistics.tile;

import astavie.thermallogistics.util.type.Type;
import cofh.core.network.PacketBase;
import net.minecraft.nbt.NBTTagCompound;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

public class Request<I> {

	public final Type<I> type;
	public final byte side;
	public final int index;

	public final List<List<Pair<Type<I>, Long>>> error;

	public long amount;

	public Request(Type<I> type, long amount, byte side, int index) {
		this.type = type;
		this.amount = amount;
		this.side = side;
		this.index = index;
		this.error = Collections.emptyList();
	}

	public Request(Type<I> type, long amount, int index, List<List<Pair<Type<I>, Long>>> error) {
		this.type = type;
		this.amount = amount;
		this.side = 0;
		this.index = index;
		this.error = error;
	}

	public static <I> Request<I> readPacket(PacketBase packet, Function<PacketBase, Type<I>> func) {
		Type<I> type = func.apply(packet);
		long amount = packet.getLong();
		int index = packet.getInt();

		int size = packet.getInt();

		List<List<Pair<Type<I>, Long>>> lists = new LinkedList<>();

		for (int i = 0; i < size; i++) {
			List<Pair<Type<I>, Long>> list = new LinkedList<>();

			int size2 = packet.getInt();
			for (int j = 0; j < size2; j++) {
				Type<I> type2 = func.apply(packet);
				long amount2 = packet.getLong();
				list.add(Pair.of(type2, amount2));
			}

			lists.add(list);
		}

		return new Request<>(type, amount, index, lists);
	}

	public static <I> void writePacket(PacketBase packet, Request<I> request, int index) {
		request.type.writePacket(packet);
		packet.addLong(request.amount);
		packet.addInt(index);

		packet.addInt(request.error.size());
		for (List<Pair<Type<I>, Long>> list : request.error) {
			packet.addInt(list.size());
			for (Pair<Type<I>, Long> pair : list) {
				pair.getLeft().writePacket(packet);
				packet.addLong(pair.getRight());
			}
		}

		// Not writing side because the client doesn't need to know that
		// TODO: But because of this the order is different on the client than on the server
	}

	public static <I> Request<I> readNBT(NBTTagCompound nbt, Function<NBTTagCompound, Type<I>> func, int index) {
		Type<I> type = func.apply(nbt);
		long amount = nbt.getLong("Count");
		byte side = nbt.getByte("side");
		return new Request<>(type, amount, side, index);
	}

	public static NBTTagCompound writeNBT(Request<?> request) {
		// We don't save errors because that just takes up more memory
		NBTTagCompound nbt = request.type.writeNbt();
		nbt.setLong("Count", request.amount);
		nbt.setByte("side", request.side);
		return nbt;
	}

	public boolean isError() {
		return !error.isEmpty();
	}

}
