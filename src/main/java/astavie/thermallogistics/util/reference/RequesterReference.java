package astavie.thermallogistics.util.reference;

import astavie.thermallogistics.event.EventHandler;
import astavie.thermallogistics.util.IProcessHolder;
import astavie.thermallogistics.util.IRequester;
import cofh.thermaldynamics.duct.Attachment;
import cofh.thermaldynamics.duct.tiles.TileGrid;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.DimensionManager;

public class RequesterReference<R extends IRequester<?, ?>> {

	public final int dim;
	public final BlockPos pos;
	public final byte side;
	public final int index;

	private R cache = null;
	private long tick = -1;

	public RequesterReference(R requester) {
		this.dim = requester.getDuct().world().provider.getDimension();
		this.pos = new BlockPos(requester.getBase());
		this.side = requester.getSide();
		this.index = requester.getIndex();

		cache = requester;
		tick = EventHandler.time;
	}

	public RequesterReference(int dim, BlockPos pos, byte side, int index) {
		this.dim = dim;
		this.pos = pos;
		this.side = side;
		this.index = index;
	}

	public boolean isLoaded() {
		return DimensionManager.getWorld(dim).isBlockLoaded(pos);
	}

	@SuppressWarnings("unchecked")
	public R getRequester() {
		if (EventHandler.time == tick)
			return cache;

		R requester = null;

		TileEntity tile = DimensionManager.getWorld(dim).getTileEntity(pos);
		if (tile != null) {
			if (index != -1 && tile instanceof IProcessHolder)
				requester = (R) ((IProcessHolder) tile).getProcesses().get(index);
			else if (tile instanceof IRequester)
				requester = (R) tile;
			else if (tile instanceof TileGrid) {
				Attachment attachment = ((TileGrid) tile).getAttachment(side);
				if (attachment != null) {
					if (index != -1 && attachment instanceof IProcessHolder)
						requester = (R) ((IProcessHolder) attachment).getProcesses().get(index);
					else if (attachment instanceof IRequester)
						requester = (R) attachment;
				}
			}
		}

		if (requester != null) {
			cache = requester;
			tick = EventHandler.time;
		}

		return requester;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		} else if (obj instanceof RequesterReference) {
			RequesterReference reference = (RequesterReference) obj;
			return dim == reference.dim && pos == reference.pos && side == reference.side && index == reference.index;
		}
		return false;
	}

}
