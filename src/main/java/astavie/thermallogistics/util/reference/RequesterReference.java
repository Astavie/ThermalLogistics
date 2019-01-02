package astavie.thermallogistics.util.reference;

import astavie.thermallogistics.util.IProcessHolder;
import astavie.thermallogistics.util.IRequester;
import cofh.thermaldynamics.duct.Attachment;
import cofh.thermaldynamics.duct.tiles.TileGrid;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class RequesterReference<R extends IRequester<?, ?>> {

	public final World world;
	public final BlockPos pos;
	public final byte side;
	public final int index;

	private R cache = null;
	private long tick = -1;

	public RequesterReference(R requester) {
		this.world = requester.getDuct().world();
		this.pos = new BlockPos(requester.getBase());
		this.side = requester.getSide();
		this.index = requester.getIndex();

		cache = requester;
		tick = world.getTotalWorldTime();
	}

	public RequesterReference(World world, BlockPos pos, byte side, int index) {
		this.world = world;
		this.pos = pos;
		this.side = side;
		this.index = index;
	}

	public boolean isLoaded() {
		return world.isBlockLoaded(pos);
	}

	@SuppressWarnings("unchecked")
	public R getRequester() {
		if (world.getTotalWorldTime() == tick)
			return cache;

		R requester = null;

		TileEntity tile = world.getTileEntity(pos);
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
			tick = world.getTotalWorldTime();
		}

		return requester;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		} else if (obj instanceof RequesterReference) {
			RequesterReference reference = (RequesterReference) obj;
			return world == reference.world && pos == reference.pos && side == reference.side && index == reference.index;
		}
		return false;
	}

}
