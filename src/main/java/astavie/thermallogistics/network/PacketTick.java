package astavie.thermallogistics.network;

import astavie.thermallogistics.event.EventHandler;
import cofh.core.network.PacketBase;
import cofh.core.network.PacketHandler;
import net.minecraft.entity.player.EntityPlayer;

public class PacketTick extends PacketBase {

	public PacketTick(long time) {
		addLong(time);
	}

	public PacketTick() {
	}

	public static void initialize() {
		PacketHandler.INSTANCE.registerPacket(PacketTick.class);
	}

	@Override
	public void handlePacket(EntityPlayer player, boolean isServer) {
		EventHandler.time = getLong();
	}

}
