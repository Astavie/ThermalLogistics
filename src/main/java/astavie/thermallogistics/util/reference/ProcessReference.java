package astavie.thermallogistics.util.reference;

import astavie.thermallogistics.process.IProcess;
import astavie.thermallogistics.util.IProcessHolder;
import cofh.thermaldynamics.duct.Attachment;
import cofh.thermaldynamics.duct.tiles.TileGrid;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ProcessReference<P extends IProcess<?, ?, ?>> {

	public final World world;
	public final BlockPos pos;
	public final byte side;
	public final int index;

	private P cache = null;
	private long tick = -1;

	public ProcessReference(P process) {
		this.world = process.getDuct().world();
		this.pos = new BlockPos(process.getBase());
		this.side = process.getSide();
		this.index = process.getIndex();

		cache = process;
		tick = world.getTotalWorldTime();
	}

	public ProcessReference(World world, BlockPos pos, byte side, int index) {
		this.world = world;
		this.pos = pos;
		this.side = side;
		this.index = index;
	}

	public boolean isLoaded() {
		return world.isBlockLoaded(pos);
	}

	@SuppressWarnings("unchecked")
	public P getProcess() {
		if (world.getTotalWorldTime() == tick)
			return cache;

		P process = null;

		TileEntity tile = world.getTileEntity(pos);
		if (tile != null) {
			if (tile instanceof IProcessHolder) {
				process = (P) ((IProcessHolder) tile).getProcesses().get(index);
			} else if (tile instanceof TileGrid) {
				Attachment attachment = ((TileGrid) tile).getAttachment(side);
				if (attachment instanceof IProcessHolder)
					process = (P) ((IProcessHolder) attachment).getProcesses().get(index);
			}
		}

		if (process != null) {
			cache = process;
			tick = world.getTotalWorldTime();
		}

		return process;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		} else if (obj instanceof ProcessReference) {
			ProcessReference reference = (ProcessReference) obj;
			return world == reference.world && pos == reference.pos && side == reference.side && index == reference.index;
		}
		return false;
	}

}
