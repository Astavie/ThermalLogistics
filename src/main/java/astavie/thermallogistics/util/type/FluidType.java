package astavie.thermallogistics.util.type;

import codechicken.lib.fluid.FluidUtils;
import net.minecraftforge.fluids.FluidStack;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class FluidType implements Type<FluidStack> {

	private final FluidStack compare;

	public FluidType(FluidStack compare) {
		this.compare = FluidUtils.copy(compare, 1);
	}

	@Override
	public FluidStack getAsStack() {
		return compare;
	}

	@Override
	public FluidStack withAmount(int amount) {
		return FluidUtils.copy(compare, amount);
	}

	@Override
	public String getDisplayName() {
		return compare.getUnlocalizedName();
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof FluidType && compare.isFluidEqual(((FluidType) obj).compare);
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder().append(compare.getFluid()).append(compare.tag).build();
	}

}
