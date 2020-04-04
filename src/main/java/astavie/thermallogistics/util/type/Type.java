package astavie.thermallogistics.util.type;

import astavie.thermallogistics.util.StackHandler;
import cofh.core.gui.GuiContainerCore;
import cofh.core.network.PacketBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;
import java.util.function.Function;

public interface Type<I> {

	I getAsStack();

	I withAmount(int amount);

	String getDisplayName();

	void writePacket(PacketBase packet);

	static Function<PacketBase, Type<?>> getReadFunction(int id) {
		if (id == 0) {
			return ItemType::readPacket;
		} else if (id == 1) {
			return FluidType::readPacket;
		} else {
			throw new IllegalArgumentException();
		}
	}

	int getPacketId();

	NBTTagCompound writeNbt();

	boolean references(I stack);

	int normalSize();

	int maxSize();

	boolean isNothing();

	@SideOnly(Side.CLIENT)
	default List<String> getTooltip(GuiContainerCore gui) {
		return StackHandler.getTooltip(gui, getAsStack());
	}

	@SideOnly(Side.CLIENT)
	default void render(GuiContainerCore gui, int x, int y) {
		StackHandler.render(gui, x, y, getAsStack(), null);
	}

}
