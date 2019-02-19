package astavie.thermallogistics.process;

import astavie.thermallogistics.util.RequesterReference;
import cofh.core.network.PacketBase;
import cofh.core.util.helpers.FluidHelper;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fluids.FluidStack;

import java.util.Iterator;

public class RequestFluid extends Request<FluidStack> {

	public RequestFluid(RequesterReference<FluidStack> attachment) {
		super(attachment);
	}

	public RequestFluid(RequesterReference<FluidStack> attachment, FluidStack stack) {
		super(attachment, stack.copy());
	}

	private RequestFluid(RequesterReference<FluidStack> attachment, long id) {
		super(attachment, id);
	}

	public static NBTTagCompound writeNBT(Request<FluidStack> request) {
		NBTTagList stacks = new NBTTagList();
		for (FluidStack stack : request.stacks)
			stacks.appendTag(stack.writeToNBT(new NBTTagCompound()));

		NBTTagList blacklist = new NBTTagList();
		for (RequesterReference<FluidStack> reference : request.blacklist)
			blacklist.appendTag(RequesterReference.writeNBT(reference));

		NBTTagCompound nbt = new NBTTagCompound();
		nbt.setTag("attachment", RequesterReference.writeNBT(request.attachment));
		nbt.setTag("stacks", stacks);
		nbt.setTag("blacklist", blacklist);
		nbt.setLong("id", request.id);
		return nbt;
	}

	public static RequestFluid readNBT(NBTTagCompound nbt) {
		RequestFluid request = new RequestFluid(RequesterReference.readNBT(nbt.getCompoundTag("attachment")), nbt.getInteger("id"));

		NBTTagList stacks = nbt.getTagList("stacks", Constants.NBT.TAG_COMPOUND);
		for (int i = 0; i < stacks.tagCount(); i++)
			request.stacks.add(FluidStack.loadFluidStackFromNBT(stacks.getCompoundTagAt(i)));

		NBTTagList blacklist = nbt.getTagList("blacklist", Constants.NBT.TAG_COMPOUND);
		for (int i = 0; i < blacklist.tagCount(); i++)
			request.blacklist.add(RequesterReference.readNBT(blacklist.getCompoundTagAt(i)));

		return request;
	}

	public static void writePacket(PacketBase packet, Request<FluidStack> request) {
		RequesterReference.writePacket(packet, request.attachment);
		packet.addLong(request.id);

		packet.addInt(request.stacks.size());
		for (FluidStack stack : request.stacks)
			packet.addFluidStack(stack);
	}

	public static RequestFluid readPacket(PacketBase packet) {
		RequestFluid request = new RequestFluid(RequesterReference.readPacket(packet), packet.getLong());

		int size = packet.getInt();
		for (int i = 0; i < size; i++)
			request.stacks.add(packet.getFluidStack());

		return request;
	}

	@Override
	public void addStack(FluidStack stack) {
		if (stack == null)
			return;
		for (FluidStack s : stacks) {
			if (FluidHelper.isFluidEqual(stack, s)) {
				s.amount += stack.amount;
				return;
			}
		}
		stacks.add(stack.copy());
	}

	@Override
	public void decreaseStack(FluidStack stack) {
		if (stack == null)
			return;
		for (Iterator<FluidStack> iterator = stacks.iterator(); iterator.hasNext(); ) {
			FluidStack s = iterator.next();
			if (FluidHelper.isFluidEqual(stack, s)) {
				s.amount -= stack.amount;
				if (s.amount <= 0)
					iterator.remove();
				return;
			}
		}
	}

	@Override
	public int getCount(FluidStack stack) {
		for (FluidStack item : stacks)
			if (FluidHelper.isFluidEqual(item, stack))
				return item.amount;
		return 0;
	}

}
