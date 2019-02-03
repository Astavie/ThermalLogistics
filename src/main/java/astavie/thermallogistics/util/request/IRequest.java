package astavie.thermallogistics.util.request;

import astavie.thermallogistics.util.IRequester;
import astavie.thermallogistics.util.delegate.IDelegate;
import astavie.thermallogistics.util.reference.RequesterReference;
import cofh.core.network.PacketBase;
import cofh.thermaldynamics.duct.tiles.DuctUnit;
import cofh.thermaldynamics.duct.tiles.TileGrid;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;
import java.util.List;

public interface IRequest<T extends DuctUnit<T, ?, ?>, I> extends Comparable<IRequest<T, I>> {

	static <T extends DuctUnit<T, ?, ?>, I> NBTTagCompound writeNbt(IRequest<T, I> request, IDelegate<I> delegate) {
		NBTTagCompound tag = new NBTTagCompound();
		tag.setLong("age", request.getAge());

		if (request.getStartReference() != null) {
			RequesterReference<IRequester<T, I>> start = request.getStartReference();

			NBTTagCompound n = new NBTTagCompound();
			n.setInteger("x", start.pos.getX());
			n.setInteger("y", start.pos.getY());
			n.setInteger("z", start.pos.getZ());
			n.setByte("side", start.side);
			if (start.index != -1)
				n.setInteger("index", start.index);

			tag.setTag("start", n);
		} else if (request.getStart() != null)
			tag.setTag("start", IRequester.writeNbt(request.getStart(), true));

		NBTTagList list = new NBTTagList();
		for (I stack : request.getStacks())
			list.appendTag(delegate.writeNbt(stack));

		tag.setTag("stacks", list);
		return tag;
	}

	static <T extends DuctUnit<T, ?, ?>, I> void writePacket(IRequest<T, I> request, IDelegate<I> delegate, PacketBase packet) {
		packet.addLong(request.getAge());

		if (request.getStartReference() != null) {
			packet.addBool(true);

			RequesterReference<IRequester<T, I>> start = request.getStartReference();
			packet.addInt(start.pos.getX());
			packet.addInt(start.pos.getY());
			packet.addInt(start.pos.getZ());
			packet.addInt(start.side);
			packet.addInt(start.index);
		} else if (request.getStart() != null) {
			packet.addBool(true);
			IRequester.writePacket(packet, request.getStart());
		} else packet.addBool(false);

		IRequester<T, I> start = request.getStart();
		if (start != null) {
			packet.addBool(true);

			BlockPos pos = start.getBase();
			TileEntity tile = Minecraft.getMinecraft().player.world.getTileEntity(pos);
			if (tile instanceof TileGrid)
				pos = pos.offset(EnumFacing.byIndex(start.getSide()));

			IBlockState state = Minecraft.getMinecraft().player.world.getBlockState(pos);
			//noinspection deprecation
			packet.addItemStack(state.getBlock().getItem(Minecraft.getMinecraft().player.world, pos, state));

			if (tile instanceof TileGrid) {
				packet.addBool(true);
				packet.addItemStack(((TileGrid) tile).getAttachment(start.getSide()).getPickBlock());
			} else packet.addBool(false);
		} else packet.addBool(false);

		packet.addInt(request.getStacks().size());
		for (I stack : request.getStacks())
			delegate.writePacket(packet, stack);
	}

	List<I> getStacks();

	@Nullable
	IRequester<T, I> getStart();

	@Nullable
	default RequesterReference<IRequester<T, I>> getStartReference() {
		return null;
	}

	long getAge();

	@Override
	default int compareTo(IRequest<T, I> o) {
		return Long.compare(getAge(), o.getAge());
	}

	// Client-only

	default ItemStack getBlock() {
		return ItemStack.EMPTY;
	}

	default ItemStack getAttachment() {
		return ItemStack.EMPTY;
	}

}
