package astavie.thermallogistics.event;

import astavie.thermallogistics.process.IProcess;
import astavie.thermallogistics.util.IProcessLoader;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.LinkedList;
import java.util.List;

public class EventHandler {

	public static final List<IProcessLoader> LOADERS = new LinkedList<>();
	public static final List<IProcess> PROCESSES = new LinkedList<>();

	public static long time = 0;

	public static WorldServer getWorld(int dim) {
		WorldServer world = DimensionManager.getWorld(dim);
		if (world == null) {
			DimensionManager.initDimension(dim);
			return DimensionManager.getWorld(dim);
		}
		return world;
	}

	public static boolean isBlockLoaded(int dim, BlockPos pos) {
		World world = DimensionManager.getWorld(dim);
		return world != null && world.isBlockLoaded(pos);
	}

	@SubscribeEvent
	public void onTick(TickEvent.ServerTickEvent event) {
		if (event.phase == TickEvent.Phase.START) {
			// Update process loaders
			LOADERS.forEach(IProcessLoader::loadProcesses);
			LOADERS.forEach(IProcessLoader::postLoad);
			LOADERS.clear();

			// Remove processes that are unloaded, done or have failed
			for (int i = 0; i < PROCESSES.size(); i++) {
				IProcess process = PROCESSES.get(i);
				if (process.shouldUnload()) {
					process.unload();
					i--;
				} else if (process.hasFailed()) {
					process.fail();
					process.remove();
					i--;
				} else if (process.isDone()) {
					process.remove();
					i--;
				}
			}

			// Update processes
			//noinspection ForLoopReplaceableByForEach
			for (int i = 0, processesSize = PROCESSES.size(); i < processesSize; i++) {
				IProcess process = PROCESSES.get(i);
				if (process.isLoaded() && !process.hasFailed())
					process.update();
			}
		} else time++;
	}

}
