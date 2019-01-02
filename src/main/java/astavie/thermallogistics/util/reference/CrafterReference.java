package astavie.thermallogistics.util.reference;

import astavie.thermallogistics.attachment.Crafter;
import cofh.thermaldynamics.duct.Attachment;
import cofh.thermaldynamics.duct.tiles.TileGrid;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class CrafterReference<C extends Crafter<?, ?, ?>> {

	public final World world;
	public final BlockPos pos;
	public final byte side;

	private C cache = null;
	private long tick = -1;

	public CrafterReference(C crafter) {
		this.world = crafter.getTile().getWorld();
		this.pos = crafter.getBase();
		this.side = crafter.getSide();

		cache = crafter;
		tick = world.getTotalWorldTime();
	}

	public CrafterReference(World world, BlockPos pos, byte side) {
		this.world = world;
		this.pos = pos;
		this.side = side;
	}

	public boolean isLoaded() {
		return world.isBlockLoaded(pos);
	}

	@SuppressWarnings("unchecked")
	public C getCrafter() {
		if (world.getTotalWorldTime() == tick)
			return cache;

		C crafter = null;

		TileEntity tile = world.getTileEntity(pos);
		if (tile instanceof TileGrid) {
			Attachment attachment = ((TileGrid) tile).getAttachment(side);
			if (attachment instanceof Crafter)
				crafter = (C) attachment;
		}

		if (crafter != null) {
			cache = crafter;
			tick = world.getTotalWorldTime();
		}

		return crafter;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		} else if (obj instanceof CrafterReference) {
			CrafterReference reference = (CrafterReference) obj;
			return world == reference.world && pos == reference.pos && side == reference.side;
		}
		return false;
	}

}
