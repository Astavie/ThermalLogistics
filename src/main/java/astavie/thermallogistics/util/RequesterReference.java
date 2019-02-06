package astavie.thermallogistics.util;

import astavie.thermallogistics.attachment.IRequester;
import cofh.core.network.PacketBase;
import cofh.thermaldynamics.duct.Attachment;
import cofh.thermaldynamics.duct.tiles.TileGrid;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.common.FMLCommonHandler;

public class RequesterReference<I> {

	public static final RequesterReference EMPTY = new RequesterReference(0, BlockPos.ORIGIN, (byte) 0);

	public final int dim;
	public final BlockPos pos;
	public final byte side;

	private long tick;
	private IRequester<I> cache;

	private ItemStack icon = ItemStack.EMPTY;
	private ItemStack tile = ItemStack.EMPTY;

	public RequesterReference(int dim, BlockPos pos, byte side) {
		this.dim = dim;
		this.pos = pos;
		this.side = side;
	}

	public static NBTTagCompound writeNBT(RequesterReference<?> reference) {
		NBTTagCompound nbt = new NBTTagCompound();
		nbt.setInteger("dim", reference.dim);
		nbt.setInteger("x", reference.pos.getX());
		nbt.setInteger("y", reference.pos.getY());
		nbt.setInteger("z", reference.pos.getZ());
		nbt.setByte("side", reference.side);
		return nbt;
	}

	public static <I> RequesterReference<I> readNBT(NBTTagCompound nbt) {
		return new RequesterReference<>(nbt.getInteger("dim"), new BlockPos(nbt.getInteger("x"), nbt.getInteger("y"), nbt.getInteger("z")), nbt.getByte("side"));
	}

	public static void writePacket(PacketBase packet, RequesterReference<?> reference) {
		if (reference == EMPTY) {
			packet.addBool(false);
			return;
		}

		packet.addBool(true);

		packet.addInt(reference.dim);
		packet.addCoords(reference.pos.getX(), reference.pos.getY(), reference.pos.getZ());
		packet.addByte(reference.side);

		IRequester<?> requester = reference.getAttachment();
		packet.addItemStack(requester.getIcon());
		packet.addItemStack(requester.getTileIcon());
	}

	public static <I> RequesterReference<I> readPacket(PacketBase packet) {
		if (packet.getBool()) {
			RequesterReference<I> reference = new RequesterReference<>(packet.getInt(), packet.getCoords(), packet.getByte());
			reference.icon = packet.getItemStack();
			reference.tile = packet.getItemStack();

			return reference;
		}

		//noinspection unchecked
		return (RequesterReference<I>) EMPTY;
	}

	public boolean isLoaded() {
		World world = DimensionManager.getWorld(dim);
		return world != null && world.isBlockLoaded(pos);
	}

	public boolean isLoaded(World world) {
		return world.provider.getDimension() == dim && world.isBlockLoaded(pos);
	}

	public IRequester<I> getAttachment() {
		return getAttachment(FMLCommonHandler.instance().getMinecraftServerInstance().getWorld(dim));
	}

	@SuppressWarnings("unchecked")
	public IRequester<I> getAttachment(World world) {
		if (world.provider.getDimension() != dim)
			return null;

		if (world.getTotalWorldTime() == tick)
			return cache;

		tick = world.getTotalWorldTime();
		cache = null;

		TileEntity tile = world.getTileEntity(pos);

		try {
			if (tile instanceof IRequester) {
				cache = (IRequester<I>) tile;
			} else if (tile instanceof TileGrid) {
				Attachment attachment = ((TileGrid) tile).getAttachment(side);
				if (attachment instanceof IRequester)
					cache = (IRequester<I>) attachment;
			}
		} catch (ClassCastException ignore) {
			cache = null;
		}

		return cache;
	}

	public boolean references(IRequester<I> requester) {
		return dim == requester.getTile().getWorld().provider.getDimension() && pos.equals(requester.getTile().getPos()) && side == requester.getSide();
	}

	public ItemStack getIcon() {
		return icon;
	}

	public ItemStack getTileIcon() {
		return tile;
	}

}
