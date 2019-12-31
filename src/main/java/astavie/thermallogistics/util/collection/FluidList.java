package astavie.thermallogistics.util.collection;

import astavie.thermallogistics.util.type.FluidType;
import astavie.thermallogistics.util.type.Type;
import cofh.core.network.PacketBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;

public class FluidList extends StackList<FluidStack> {

	@Override
	public Type<FluidStack> getType(FluidStack stack) {
		return new FluidType(stack);
	}

	@Override
	public int getAmount(FluidStack stack) {
		return stack.amount;
	}

	@Override
	public Type<FluidStack> readType(PacketBase packet) {
		return FluidType.readPacket(packet);
	}

	@Override
	public Type<FluidStack> readType(NBTTagCompound tag) {
		return FluidType.readNbt(tag);
	}

	@Override
	public StackList<FluidStack> copy() {
		FluidList list = new FluidList();
		list.map.putAll(map);
		return list;
	}

}
