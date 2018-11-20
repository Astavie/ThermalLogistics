package astavie.thermallogistics.util.delegate;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;

public class DelegateFluid implements IDelegate<FluidStack> {

	public static final DelegateFluid INSTANCE = new DelegateFluid();

	@Override
	public boolean isNull(FluidStack stack) {
		return stack == null || stack.amount == 0;
	}

	@Override
	public NBTTagCompound writeStack(FluidStack stack) {
		return stack.writeToNBT(new NBTTagCompound());
	}

	@Override
	public FluidStack readStack(NBTTagCompound tag) {
		return FluidStack.loadFluidStackFromNBT(tag);
	}

}
