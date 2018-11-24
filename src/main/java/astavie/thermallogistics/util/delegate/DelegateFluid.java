package astavie.thermallogistics.util.delegate;

import cofh.core.network.PacketBase;
import cofh.core.util.helpers.FluidHelper;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;

import java.util.Iterator;

public class DelegateFluid implements IDelegate<FluidStack> {

	public static final DelegateFluid INSTANCE = new DelegateFluid();

	@Override
	public boolean isNull(FluidStack stack) {
		return stack == null || stack.amount == 0;
	}

	@Override
	public FluidStack copy(FluidStack stack) {
		return stack.copy();
	}

	@Override
	public void truncate(Iterable<FluidStack> iterable) {
		Iterator<FluidStack> a = iterable.iterator();
		while (a.hasNext()) {
			FluidStack fa = a.next();
			for (FluidStack fb : iterable) {
				if (fa == fb)
					break;

				if (FluidHelper.isFluidEqual(fa, fb)) {
					fb.amount += fa.amount;
					a.remove();
					break;
				}
			}
		}
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
