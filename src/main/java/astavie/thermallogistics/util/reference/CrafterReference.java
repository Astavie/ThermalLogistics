package astavie.thermallogistics.util.reference;

import astavie.thermallogistics.attachment.Crafter;
import astavie.thermallogistics.event.EventHandler;
import cofh.thermaldynamics.duct.Attachment;
import cofh.thermaldynamics.duct.tiles.TileGrid;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.DimensionManager;

public class CrafterReference<C extends Crafter<?, ?, ?>> {

	public final int dim;
	public final BlockPos pos;
	public final byte side;

	private C cache = null;
	private long tick = -1;

	public CrafterReference(C crafter) {
		this.dim = crafter.getTile().getWorld().provider.getDimension();
		this.pos = crafter.getBase();
		this.side = crafter.getSide();

		cache = crafter;
		tick = EventHandler.time;
	}

	public CrafterReference(int dim, BlockPos pos, byte side) {
		this.dim = dim;
		this.pos = pos;
		this.side = side;
	}

	public boolean isLoaded() {
		return DimensionManager.getWorld(dim).isBlockLoaded(pos);
	}

	@SuppressWarnings("unchecked")
	public C getCrafter() {
		if (EventHandler.time == tick)
			return cache;

		C crafter = null;

		TileEntity tile = DimensionManager.getWorld(dim).getTileEntity(pos);
		if (tile instanceof TileGrid) {
			Attachment attachment = ((TileGrid) tile).getAttachment(side);
			if (attachment instanceof Crafter)
				crafter = (C) attachment;
		}

		if (crafter != null) {
			cache = crafter;
			tick = EventHandler.time;
		}

		return crafter;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		} else if (obj instanceof CrafterReference) {
			CrafterReference reference = (CrafterReference) obj;
			return dim == reference.dim && pos == reference.pos && side == reference.side;
		}
		return false;
	}

}
