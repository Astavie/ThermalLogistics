package astavie.thermallogistics.util;

import astavie.thermallogistics.attachment.IRequester;
import astavie.thermallogistics.attachment.IRequesterContainer;
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

	public final int dim;
	public final BlockPos pos;
	public final byte side;
	public final int index;

	private long tick;
	private IRequester<I> cache;

	// TODO: Maybe remove this?
	private ItemStack icon = ItemStack.EMPTY;
	private ItemStack tile = ItemStack.EMPTY;

	public RequesterReference(int dim, BlockPos pos) {
		this(dim, pos, (byte) 0, 0);
	}

	public RequesterReference(int dim, BlockPos pos, byte side) {
		this(dim, pos, side, 0);
	}

	public RequesterReference(int dim, BlockPos pos, byte side, int index) {
		this.dim = dim;
		this.pos = pos;
		this.side = side;
		this.index = index;
	}

	public static NBTTagCompound writeNBT(RequesterReference<?> reference) {
		NBTTagCompound nbt = new NBTTagCompound();
		if (reference == null) {
			return nbt;
		}

		nbt.setInteger("dim", reference.dim);
		nbt.setInteger("x", reference.pos.getX());
		nbt.setInteger("y", reference.pos.getY());
		nbt.setInteger("z", reference.pos.getZ());
		nbt.setByte("side", reference.side);
		nbt.setInteger("index", reference.index);
		return nbt;
	}

	public static <I> RequesterReference<I> readNBT(NBTTagCompound nbt) {
		if (nbt.isEmpty()) {
			return null;
		} else {
			return new RequesterReference<>(nbt.getInteger("dim"), new BlockPos(nbt.getInteger("x"), nbt.getInteger("y"), nbt.getInteger("z")), nbt.getByte("side"), nbt.getInteger("index"));
		}
	}

	public static void writePacket(PacketBase packet, RequesterReference<?> reference) {
		packet.addInt(reference.dim);
		packet.addCoords(reference.pos.getX(), reference.pos.getY(), reference.pos.getZ());
		packet.addByte(reference.side);
		packet.addInt(reference.index);

		IRequester<?> requester = reference.get();
		packet.addItemStack(requester.getIcon());
		packet.addItemStack(requester.getTileIcon());
	}

	public static <I> RequesterReference<I> readPacket(PacketBase packet) {
		RequesterReference<I> reference = new RequesterReference<>(packet.getInt(), packet.getCoords(), packet.getByte(), packet.getInt());
		reference.icon = packet.getItemStack();
		reference.tile = packet.getItemStack();

		return reference;
	}

	public boolean isLoaded() {
		World world = DimensionManager.getWorld(dim);
		return world != null && world.isBlockLoaded(pos);
	}

	public boolean isLoaded(World world) {
		return world.provider.getDimension() == dim && world.isBlockLoaded(pos);
	}

	public IRequester<I> get() {
		return get(FMLCommonHandler.instance().getMinecraftServerInstance().getWorld(dim));
	}

	public IRequesterContainer<I> getContainer() {
		return getContainer(FMLCommonHandler.instance().getMinecraftServerInstance().getWorld(dim));
	}

	@SuppressWarnings("unchecked")
	public IRequester<I> get(World world) {
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
			} else if (tile instanceof IRequesterContainer) {
				cache = (IRequester<I>) ((IRequesterContainer) tile).getRequesters().get(index);
			} else if (tile instanceof TileGrid) {
				Attachment attachment = ((TileGrid) tile).getAttachment(side);
				if (attachment instanceof IRequester)
					cache = (IRequester<I>) attachment;
				else if (attachment instanceof IRequesterContainer)
					cache = (IRequester<I>) ((IRequesterContainer) attachment).getRequesters().get(index);
			}
		} catch (ClassCastException ignore) {
			cache = null;
		}

		return cache;
	}

	@SuppressWarnings("unchecked")
	public IRequesterContainer<I> getContainer(World world) {
		if (world.provider.getDimension() != dim)
			return null;

		TileEntity tile = world.getTileEntity(pos);

		try {
			if (tile instanceof IRequesterContainer) {
				return (IRequesterContainer<I>) tile;
			} else if (tile instanceof TileGrid) {
				Attachment attachment = ((TileGrid) tile).getAttachment(side);
				if (attachment instanceof IRequesterContainer)
					return (IRequesterContainer<I>) attachment;
			}
		} catch (ClassCastException ignore) {
		}

		return null;
	}

	public boolean references(IRequester<?> requester) {
		return requester != null && requester.referencedBy(this);
	}

	public boolean equals(Object object) {
		if (object instanceof RequesterReference) {
			RequesterReference<?> reference = (RequesterReference<?>) object;
			return reference.dim == dim && reference.pos.equals(pos) && reference.side == side && reference.index == index;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return ((Integer.hashCode(dim) * 31 + pos.hashCode()) * 31 + Integer.hashCode(side)) * 31 + Integer.hashCode(index);
	}

	public ItemStack getIcon() {
		return icon;
	}

	public ItemStack getTileIcon() {
		return tile;
	}

}
