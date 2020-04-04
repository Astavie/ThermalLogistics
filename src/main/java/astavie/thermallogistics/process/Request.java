package astavie.thermallogistics.process;

import astavie.thermallogistics.util.collection.StackList;
import astavie.thermallogistics.util.type.Type;
import cofh.core.network.PacketBase;
import net.minecraft.nbt.NBTTagCompound;

import java.util.function.Function;

public class Request<I> {

	public final Type<I> type;
	public final int index;

	public final StackList<I> missing;
	public final boolean complex;

	public long amount;
	public Source<I> source;

	public Request(Type<I> type, long amount, Source<I> source, int index) {
		this.type = type;
		this.amount = amount;
		this.source = source;
		this.index = index;

		this.missing = null;
		this.complex = false;
	}

	public Request(Type<I> type, long amount, int index, StackList<I> missing, boolean complex) {
		this.type = type;
		this.amount = amount;
		this.source = null;
		this.index = index;

		this.missing = missing;
		this.complex = complex;
	}

	public static <I> Request<I> readPacket(PacketBase packet, Function<PacketBase, Type<I>> func, Function<PacketBase, StackList<I>> func2) {
		Type<I> type = func.apply(packet);
		long amount = packet.getLong();
		int index = packet.getInt();

		if (packet.getBool()) {
			return new Request<>(type, amount, index, packet.getBool() ? func2.apply(packet) : null, packet.getBool());
		} else {
			return new Request<>(type, amount, Source.readPacket(packet), index);
		}
	}

	public static <I> void writePacket(PacketBase packet, Request<I> request, int index) {
		request.type.writePacket(packet);
		packet.addLong(request.amount);
		packet.addInt(index);

		packet.addBool(request.isError());

		if (request.isError()) {
			packet.addBool(request.missing != null);
			if (request.missing != null) {
				request.missing.writePacket(packet);
			}
			packet.addBool(request.complex);
		} else {
			if (request.source != null) {
				Source.writePacket(packet, request.source);
			} else {
				packet.addBool(false);
			}
		}
	}

	public static <I> Request<I> readNBT(NBTTagCompound nbt, Function<NBTTagCompound, Type<I>> func, int index) {
		Type<I> type = func.apply(nbt);
		long amount = nbt.getLong("Count");
		Source<I> source = nbt.hasKey("source") ? Source.readNbt(nbt.getTag("source")) : null;

		return new Request<>(type, amount, source, index);
	}

	public static NBTTagCompound writeNBT(Request<?> request) {
		// We don't save errors because that just takes up more memory
		NBTTagCompound nbt = request.type.writeNbt();
		nbt.setLong("Count", request.amount);
		if (request.source != null) {
			nbt.setTag("source", Source.writeNbt(request.source));
		}

		return nbt;
	}

	public boolean isError() {
		return missing != null || complex;
	}

}
