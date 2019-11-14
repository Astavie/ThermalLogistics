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
	protected int getAmount(FluidStack stack) {
		return stack.amount;
	}

	@Override
	protected Type<FluidStack> readType(PacketBase packet) {
		return FluidType.readPacket(packet);
	}

	@Override
	protected NBTTagCompound writeType(Type<FluidStack> type) {
		return FluidType.writeNbt((FluidType) type);
	}

	@Override
	protected Type<FluidStack> readType(NBTTagCompound tag) {
		return FluidType.readNbt(tag);
	}

}
