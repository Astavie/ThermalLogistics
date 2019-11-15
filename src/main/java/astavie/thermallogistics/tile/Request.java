package astavie.thermallogistics.tile;

import astavie.thermallogistics.attachment.ICrafter;
import astavie.thermallogistics.util.RequesterReference;
import astavie.thermallogistics.util.type.Type;
import cofh.core.network.PacketBase;
import net.minecraft.nbt.NBTTagCompound;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

public class Request<I> {

	// Server-side only

	public final byte side;
	public final ICrafter<I> crafter; // Doesn't have to be a reference because everything in a grid is loaded at once

	// Both sides

	public final Type<I> type;
	public final int index;

	public final List<List<Pair<Type<I>, Long>>> error;

	public long amount;

	public Request(Type<I> type, long amount, byte side, @Nullable ICrafter<I> crafter, int index) {
		this.type = type;
		this.amount = amount;
		this.side = side;
		this.crafter = crafter;
		this.index = index;
		this.error = Collections.emptyList();
	}

	public Request(Type<I> type, long amount, int index, List<List<Pair<Type<I>, Long>>> error) {
		this.type = type;
		this.amount = amount;
		this.side = 0;
		this.crafter = null;
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
	}

	public static <I> Request<I> readNBT(NBTTagCompound nbt, Function<NBTTagCompound, Type<I>> func, int index) {
		Type<I> type = func.apply(nbt);
		long amount = nbt.getLong("Count");
		byte side = nbt.getByte("side");
		ICrafter<I> crafter = nbt.hasKey("crafter") ? (ICrafter<I>) RequesterReference.readNBT(nbt.getCompoundTag("crafter")).get() : null;

		return new Request<>(type, amount, side, crafter, index);
	}

	public static NBTTagCompound writeNBT(Request<?> request) {
		// We don't save errors because that just takes up more memory
		NBTTagCompound nbt = request.type.writeNbt();
		nbt.setLong("Count", request.amount);
		nbt.setByte("side", request.side);
		if (request.crafter != null) {
			nbt.setTag("crafter", RequesterReference.writeNBT(request.crafter.createReference()));
		}

		return nbt;
	}

	public boolean isError() {
		return !error.isEmpty();
	}

}
