package astavie.thermallogistics.tile;

import astavie.thermallogistics.block.BlockTerminal;
import astavie.thermallogistics.tile.inventory.InvWrapperInsert;
import codechicken.lib.inventory.InventorySimple;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class TileTerminalItem extends TileTerminal<ItemStack> {

	public final InventorySimple inventory = new InventorySimple(27);

	@Override
	public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
		return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY && facing != world.getBlockState(pos).getValue(BlockTerminal.DIRECTION).getFace() || super.hasCapability(capability, facing);
	}

	@Nullable
	@Override
	public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
		if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY && facing != world.getBlockState(pos).getValue(BlockTerminal.DIRECTION).getFace())
			return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(new InvWrapperInsert(inventory));
		return super.getCapability(capability, facing);
	}

}
