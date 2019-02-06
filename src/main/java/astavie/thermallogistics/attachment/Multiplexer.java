package astavie.thermallogistics.attachment;

import astavie.thermallogistics.ThermalLogistics;
import astavie.thermallogistics.item.ItemMultiplexer;
import astavie.thermallogistics.proxy.ProxyClient;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.vec.Cuboid6;
import codechicken.lib.vec.Translation;
import codechicken.lib.vec.Vector3;
import codechicken.lib.vec.uv.IconTransformation;
import cofh.core.network.PacketBase;
import cofh.thermaldynamics.block.BlockDuct.ConnectionType;
import cofh.thermaldynamics.duct.Attachment;
import cofh.thermaldynamics.duct.tiles.TileGrid;
import cofh.thermaldynamics.render.RenderDuct;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.IBlockAccess;

import javax.annotation.Nonnull;
import java.util.LinkedList;
import java.util.List;

public class Multiplexer extends Attachment {

	public static final ResourceLocation ID = new ResourceLocation(ThermalLogistics.MODID, "multiplexer");

	public int type;

	public Multiplexer(TileGrid tile, byte side) {
		super(tile, side);
	}

	public Multiplexer(TileGrid tile, byte side, int type) {
		super(tile, side);
		this.type = type;
	}

	@Override
	public void readFromNBT(NBTTagCompound tag) {
		type = tag.getByte("type") % 5;
	}

	@Override
	public void writeToNBT(NBTTagCompound tag) {
		tag.setByte("type", (byte) type);
	}

	@Override
	public void addDescriptionToPacket(PacketBase packet) {
		packet.addByte(type);
	}

	@Override
	public void getDescriptionFromPacket(PacketBase packet) {
		type = packet.getByte();
	}

	@Override
	public String getName() {
		return "item.logistics.multiplexer." + ItemMultiplexer.NAMES[type] + ".name";
	}

	@Override
	public ResourceLocation getId() {
		return ID;
	}

	@Override
	public boolean isNode() {
		return true;
	}

	@Nonnull
	@Override
	public ConnectionType getNeighborType() {
		return ConnectionType.DUCT;
	}

	@Override
	public Cuboid6 getCuboid() {
		return TileGrid.subSelection[side].copy();
	}

	@Override
	public boolean canSend() {
		return false;
	}

	@Override
	public List<ItemStack> getDrops() {
		LinkedList<ItemStack> drops = new LinkedList<>();
		drops.add(getPickBlock());
		return drops;
	}

	@Override
	public ItemStack getPickBlock() {
		return new ItemStack(ThermalLogistics.multiplexer, 1, type);
	}

	@Override
	public boolean render(IBlockAccess world, BlockRenderLayer layer, CCRenderState ccRenderState) {
		if (layer != BlockRenderLayer.SOLID)
			return false;
		Translation trans = Vector3.fromTileCenter(baseTile).translation();
		RenderDuct.modelConnection[1][side].render(ccRenderState, trans, new IconTransformation(ProxyClient.MULTIPLEXER[0][type]));
		return true;
	}

}
