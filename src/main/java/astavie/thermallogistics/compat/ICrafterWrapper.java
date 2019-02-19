package astavie.thermallogistics.compat;

import astavie.thermallogistics.attachment.ICrafter;
import net.minecraft.tileentity.TileEntity;

public interface ICrafterWrapper<T extends TileEntity> {

	default <I> void populateCast(TileEntity tile, byte side, ICrafter.Recipe<I> recipe, Class<I> itemClass) {
		//noinspection unchecked
		populate((T) tile, side, recipe, itemClass);
	}

	<I> void populate(T tile, byte side, ICrafter.Recipe<I> recipe, Class<I> itemClass);

}
