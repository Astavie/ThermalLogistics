package astavie.thermallogistics.util.delegate;

import cofh.core.network.PacketBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;

public class DelegateFluid implements IDelegate<FluidStack> {

	public static final DelegateFluid INSTANCE = new DelegateFluid();

	@Override
	public boolean isNull(FluidStack stack) {
		return stack == null || stack.amount == 0;
	}

	@Override
	public NBTTagCompound writeNbt(FluidStack stack) {
		return stack.writeToNBT(new NBTTagCompound());
	}

	@Override
	public void writePacket(PacketBase packet, FluidStack stack) {
		packet.addFluidStack(stack);
	}

	@Override
	public FluidStack readNbt(NBTTagCompound tag) {
		return FluidStack.loadFluidStackFromNBT(tag);
	}

	@Override
	public FluidStack readPacket(PacketBase packet) {
		return packet.getFluidStack();
	}

}
