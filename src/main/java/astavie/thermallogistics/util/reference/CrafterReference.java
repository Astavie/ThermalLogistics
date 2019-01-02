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

	public CrafterReference(C crafter) {
		this.world = crafter.getTile().getWorld();
		this.pos = crafter.getBase();
		this.side = crafter.getSide();
	}

	public boolean isLoaded() {
		return world.isBlockLoaded(pos);
	}

	@SuppressWarnings("unchecked")
	public C getCrafter() {
		TileEntity tile = world.getTileEntity(pos);
		if (tile instanceof TileGrid) {
			Attachment attachment = ((TileGrid) tile).getAttachment(side);
			if (attachment instanceof Crafter)
				return (C) attachment;
		}
		return null;
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
