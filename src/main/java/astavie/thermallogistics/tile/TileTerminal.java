package astavie.thermallogistics.tile;

import astavie.thermallogistics.ThermalLogistics;
import astavie.thermallogistics.block.BlockTerminal;
import astavie.thermallogistics.tile.inventory.InventorySpecial;
import cofh.CoFHCore;
import cofh.core.block.TileNameable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.ITickable;

import java.awt.*;
import java.util.HashSet;
import java.util.Set;

public class TileTerminal<I> extends TileNameable implements ITickable {

	public final InventorySpecial requester = new InventorySpecial(1, i -> i.getItem() == ThermalLogistics.Items.requester, null);

	private final Set<Container> registry = new HashSet<>();

	public void register(Container container) {
		if (!world.isRemote) {
			registry.add(container);
			setActive(true);
		}
	}

	public void remove(Container container) {
		if (!world.isRemote) {
			registry.remove(container);
			if (registry.isEmpty())
				setActive(false);
		}
	}

	private void setActive(boolean active) {
		IBlockState state = world.getBlockState(pos);
		if (state.getValue(BlockTerminal.ACTIVE) != active)
			world.setBlockState(pos, state.withProperty(BlockTerminal.ACTIVE, active), 2);
	}

	@Override
	protected Object getMod() {
		return CoFHCore.instance;
	}

	@Override
	protected String getModVersion() {
		return ThermalLogistics.MOD_VERSION;
	}

	@Override
	protected String getTileName() {
		return ThermalLogistics.Blocks.terminal_item.getTranslationKey() + ".name";
	}

	@Override
	public void update() {

	}

}
