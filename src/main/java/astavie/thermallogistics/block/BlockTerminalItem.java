package astavie.thermallogistics.block;

import astavie.thermallogistics.tile.TileTerminalItem;
import cofh.core.util.CoreUtils;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class BlockTerminalItem extends BlockTerminal {

	public BlockTerminalItem(String name, String type) {
		super(name, type);
	}

	@Override
	public void breakBlock(World world, BlockPos pos, IBlockState state) {
		TileTerminalItem tile = (TileTerminalItem) world.getTileEntity(pos);
		for (int i = 0; i < tile.inventory.getSizeInventory(); i++)
			CoreUtils.dropItemStackIntoWorldWithVelocity(tile.inventory.getStackInSlot(i), world, pos);
		super.breakBlock(world, pos, state);
	}

	@Nullable
	@Override
	public TileEntity createTileEntity(@Nonnull World world, @Nonnull IBlockState state) {
		return new TileTerminalItem();
	}

}
