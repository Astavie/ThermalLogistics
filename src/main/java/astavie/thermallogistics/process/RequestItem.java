package astavie.thermallogistics.process;

import astavie.thermallogistics.util.RequesterReference;
import cofh.core.network.PacketBase;
import cofh.core.util.helpers.ItemHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;

import java.util.Collection;
import java.util.Iterator;

public class RequestItem extends Request<ItemStack> {

	public RequestItem(RequesterReference<ItemStack> attachment, Collection<ItemStack> stacks) {
		super(attachment, stacks);
	}

	public RequestItem(RequesterReference<ItemStack> attachment) {
		super(attachment);
	}

	public RequestItem(RequesterReference<ItemStack> attachment, ItemStack stack) {
		super(attachment, stack);
	}

	private RequestItem(RequesterReference<ItemStack> attachment, long id) {
		super(attachment, id);
	}

	public static NBTTagCompound writeNBT(Request<ItemStack> request) {
		NBTTagList stacks = new NBTTagList();
		for (ItemStack stack : request.stacks)
			stacks.appendTag(stack.writeToNBT(new NBTTagCompound()));

		NBTTagCompound nbt = new NBTTagCompound();
		nbt.setTag("attachment", RequesterReference.writeNBT(request.attachment));
		nbt.setTag("stacks", stacks);
		nbt.setLong("id", request.id);
		return nbt;
	}

	public static RequestItem readNBT(NBTTagCompound nbt) {
		RequestItem request = new RequestItem(RequesterReference.readNBT(nbt.getCompoundTag("attachment")), nbt.getInteger("id"));

		NBTTagList stacks = nbt.getTagList("stacks", Constants.NBT.TAG_COMPOUND);
		for (int i = 0; i < stacks.tagCount(); i++)
			request.stacks.add(new ItemStack(stacks.getCompoundTagAt(i)));

		return request;
	}

	public static void writePacket(PacketBase packet, Request<ItemStack> request) {
		RequesterReference.writePacket(packet, request.attachment);
		packet.addLong(request.id);

		packet.addInt(request.stacks.size());
		for (ItemStack stack : request.stacks)
			packet.addItemStack(stack);
	}

	public static RequestItem readPacket(PacketBase packet) {
		RequestItem request = new RequestItem(RequesterReference.readPacket(packet), packet.getLong());

		int size = packet.getInt();
		for (int i = 0; i < size; i++)
			request.stacks.add(packet.getItemStack());

		return request;
	}

	@Override
	public void addStack(ItemStack stack) {
		for (ItemStack s : stacks) {
			if (ItemHelper.itemsIdentical(stack, s)) {
				s.grow(stack.getCount());
				return;
			}
		}
		stacks.add(stack.copy());
	}

	@Override
	public void decreaseStack(ItemStack stack) {
		for (Iterator<ItemStack> iterator = stacks.iterator(); iterator.hasNext(); ) {
			ItemStack s = iterator.next();
			if (ItemHelper.itemsIdentical(stack, s)) {
				s.shrink(stack.getCount());
				if (s.isEmpty())
					iterator.remove();
				return;
			}
		}
	}

	@Override
	public int amountRequired(ItemStack stack) {
		for (ItemStack item : stacks)
			if (ItemHelper.itemsIdentical(item, stack))
				return item.getCount();
		return 0;
	}

}
