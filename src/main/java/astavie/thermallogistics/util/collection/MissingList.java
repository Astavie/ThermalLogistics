package astavie.thermallogistics.util.collection;

import astavie.thermallogistics.util.type.Type;
import cofh.core.network.PacketBase;

import java.util.LinkedHashMap;
import java.util.Map;

public class MissingList {

	public final Map<Type<?>, Long> map = new LinkedHashMap<>();

	public MissingList() {
	}

	public MissingList(PacketBase packet) {
		read(packet);
	}

	public void add(Type<?> type, long amount) {
		map.put(type, map.getOrDefault(type, 0L) + amount);
	}

	public void read(PacketBase packet) {
		int size = packet.getInt();

		for (int i = 0; i < size; i++) {
			Type<?> type = Type.getReadFunction(packet.getInt()).apply(packet);
			long amount = packet.getLong();

			map.put(type, amount);
		}
	}

	public void write(PacketBase packet) {
		packet.addInt(map.size());

		for (Map.Entry<Type<?>, Long> entry : map.entrySet()) {
			packet.addInt(entry.getKey().getPacketId());
			entry.getKey().writePacket(packet);
			packet.addLong(entry.getValue());
		}
	}

	public boolean isEmpty() {
		return map.isEmpty();
	}

}
