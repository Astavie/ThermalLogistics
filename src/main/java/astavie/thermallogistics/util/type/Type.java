package astavie.thermallogistics.util.type;

import cofh.core.network.PacketBase;

public interface Type<I> {

	I getAsStack();

	I withAmount(int amount);

	String getDisplayName();

	void writePacket(PacketBase packet);

}
