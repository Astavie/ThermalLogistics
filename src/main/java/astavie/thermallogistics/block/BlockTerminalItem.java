package astavie.thermallogistics.block;

import astavie.thermallogistics.ThermalLogistics;
import astavie.thermallogistics.tile.TileTerminalItem;
import cofh.core.util.CoreUtils;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class BlockTerminalItem extends BlockTerminal {

	public BlockTerminalItem() {
		super("terminal_item");
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
	public TileEntity createTileEntity(World world, IBlockState state) {
		return new TileTerminalItem();
	}

	@Override
	public boolean preInit() {
		TileEntity.register(ThermalLogistics.MODID + ":" + name, TileTerminalItem.class);
		return super.preInit();
	}

}
