package astavie.thermallogistics.process;

import net.minecraftforge.fluids.FluidStack;

public interface IProcessRequesterFluid extends IProcessRequester<FluidStack> {

	int tickDelay();

	float throttle();

	int maxSize();

}
