package astavie.thermallogistics.util;

import astavie.thermallogistics.ThermalLogistics;
import astavie.thermallogistics.container.ContainerCrafter;
import astavie.thermallogistics.container.ContainerTerminal;
import cofh.core.network.PacketHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;


@Mod.EventBusSubscriber(modid = ThermalLogistics.MOD_ID)
public class EventHandler {

	@SubscribeEvent
	public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
		if (event.phase == TickEvent.Phase.START && !event.player.world.isRemote && event.player.world.getTotalWorldTime() % ThermalLogistics.INSTANCE.refreshDelay == 0) {
			if (event.player.openContainer instanceof ContainerCrafter)
				((ContainerCrafter) event.player.openContainer).crafter.sync(event.player);
			else if (event.player.openContainer instanceof ContainerTerminal)
				PacketHandler.sendTo(((ContainerTerminal) event.player.openContainer).tile.getSyncPacket(), event.player);
		}
	}

}
