package astavie.thermallogistics.tile;

import astavie.thermallogistics.util.type.Type;
import cofh.core.network.PacketBase;
import net.minecraft.nbt.NBTTagCompound;

import java.util.function.Function;

public class Request<I> {

	public final Type<I> type;
	public final byte side;
	public final String error; // TODO: Turn this into a translatable thing
	public long amount;

	public Request(Type<I> type, long amount, byte side) {
		this.type = type;
		this.amount = amount;
		this.side = side;
		this.error = "";
	}

	public Request(Type<I> type, long amount, String error) {
		this.type = type;
		this.amount = amount;
		this.side = 0;
		this.error = error;
	}

	public static <I> Request<I> readPacket(PacketBase packet, Function<PacketBase, Type<I>> func) {
		Type<I> type = func.apply(packet);
		long amount = packet.getLong();
		String error = packet.getString();
		return new Request<>(type, amount, error);
	}

	public static void writePacket(PacketBase packet, Request<?> request) {
		request.type.writePacket(packet);
		packet.addLong(request.amount);
		packet.addString(request.error);
		// Not writing side because the client doesn't need to know that
		// TODO: But because of this the order is different on the client than on the server
	}

	public static <I> Request<I> readNBT(NBTTagCompound nbt, Function<NBTTagCompound, Type<I>> func) {
		Type<I> type = func.apply(nbt);
		long amount = nbt.getLong("Count");
		byte side = nbt.getByte("side");
		String error = nbt.getString("error");

		if (error.isEmpty()) {
			return new Request<>(type, amount, side);
		} else {
			return new Request<>(type, amount, error);
		}
	}

	public static NBTTagCompound writeNBT(Request<?> request) {
		NBTTagCompound nbt = request.type.writeNbt();
		nbt.setLong("Count", request.amount);
		nbt.setByte("side", request.side);
		nbt.setString("error", request.error);
		return nbt;
	}

	public boolean isError() {
		return !error.isEmpty();
	}

}
