package astavie.thermallogistics.attachment;

import astavie.thermallogistics.ThermalLogistics;
import astavie.thermallogistics.client.TLTextures;
import astavie.thermallogistics.process.ProcessFluid;
import astavie.thermallogistics.process.Request;
import codechicken.lib.fluid.FluidUtils;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.vec.Translation;
import codechicken.lib.vec.Vector3;
import codechicken.lib.vec.uv.IconTransformation;
import cofh.thermaldynamics.duct.attachments.retriever.RetrieverFluid;
import cofh.thermaldynamics.duct.attachments.servo.ServoItem;
import cofh.thermaldynamics.duct.fluid.DuctUnitFluid;
import cofh.thermaldynamics.duct.fluid.GridFluid;
import cofh.thermaldynamics.duct.item.DuctUnitItem;
import cofh.thermaldynamics.duct.item.GridItem;
import cofh.thermaldynamics.duct.tiles.TileGrid;
import cofh.thermaldynamics.multiblock.IGridTile;
import cofh.thermaldynamics.multiblock.Route;
import cofh.thermaldynamics.render.RenderDuct;
import cofh.thermaldynamics.util.ListWrapper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fluids.FluidStack;

import java.util.Collections;
import java.util.List;

public class RequesterFluid extends RetrieverFluid implements IRequester<FluidStack> {

	public static final ResourceLocation ID = new ResourceLocation(ThermalLogistics.MOD_ID, "requester_fluid");

	private final ProcessFluid process = new ProcessFluid(this);

	public RequesterFluid(TileGrid tile, byte side) {
		super(tile, side);
	}

	public RequesterFluid(TileGrid tile, byte side, int type) {
		super(tile, side, type);
		filter.handleFlagByte(24); // Whitelist by default
	}

	@Override
	public String getInfo() {
		return "tab." + ThermalLogistics.MOD_ID + ".requesterFluid";
	}

	@Override
	public ResourceLocation getId() {
		return ID;
	}

	@Override
	public ItemStack getPickBlock() {
		return new ItemStack(ThermalLogistics.Items.requester, 1, type);
	}

	@Override
	public String getName() {
		return getPickBlock().getTranslationKey() + ".name";
	}

	@Override
	public boolean render(IBlockAccess world, BlockRenderLayer layer, CCRenderState ccRenderState) {
		if (layer != BlockRenderLayer.SOLID)
			return false;
		Translation trans = Vector3.fromTileCenter(baseTile).translation();
		RenderDuct.modelConnection[isPowered ? 1 : 2][side].render(ccRenderState, trans, new IconTransformation(TLTextures.REQUESTER[stuffed ? 1 : 0][type]));
		return true;
	}

	@Override
	public void tick(int pass) {
		GridFluid grid = fluidDuct.getGrid();
		if (pass != 1 || grid == null || !isPowered || !isValidInput)
			return;

		process.tick();
	}

	@Override
	public void onNeighborChange() {
		boolean wasPowered = isPowered;
		super.onNeighborChange();
		if (wasPowered && !isPowered)
			process.requests.clear();
	}

	@Override
	public void checkSignal() {
		boolean wasPowered = isPowered;
		super.checkSignal();
		if (wasPowered && !isPowered)
			process.requests.clear();
	}

	@Override
	public void writeToNBT(NBTTagCompound tag) {
		super.writeToNBT(tag);
		tag.setTag("process", process.writeNbt());
	}

	@Override
	public void readFromNBT(NBTTagCompound tag) {
		super.readFromNBT(tag);
		process.readNbt(tag.getTagList("process", Constants.NBT.TAG_COMPOUND));
	}

	@Override
	public void writePortableData(EntityPlayer player, NBTTagCompound tag) {
		super.writePortableData(player, tag);
		tag.setString("DisplayType", new ItemStack(ThermalLogistics.Items.requester).getTranslationKey() + ".name");
	}

	@Override
	public List<FluidStack> getInputFrom(IRequester<FluidStack> requester) {
		return process.getStacks(requester);
	}

	@Override
	public List<FluidStack> getOutputTo(IRequester<FluidStack> requester) {
		return Collections.emptyList();
	}

	@Override
	public boolean isEnabled() {
		return isPowered;
	}

	@Override
	public int amountRequired(FluidStack stack) {
		DuctUnitFluid.Cache cache = fluidDuct.tileCache[side];
		if (cache == null)
			return 0;

		int required = cache.getHandler(side ^ 1).fill(FluidUtils.copy(stack, Integer.MAX_VALUE), false);
		for (Request<FluidStack> request : process.requests)
			required -= request.getCount(stack);

		return required;
	}

	@Override
	public int getMaxSend() {
		return 0;
	}

	@Override
	public float getThrottle() {
		return throttle[type];
	}

	@Override
	public IGridTile getDuct() {
		return fluidDuct;
	}

	@Override
	public TileEntity getTile() {
		return baseTile;
	}

	@Override
	public byte getSide() {
		return side;
	}

	@Override
	public byte getSpeed() {
		return 0;
	}

	@Override
	public ListWrapper<Route<DuctUnitItem, GridItem>> getRoutes() {
		return null;
	}

	@Override
	public boolean hasMultiStack() {
		return false;
	}

	@Override
	public TileEntity getCachedTile() {
		return myTile;
	}

	@Override
	public ItemStack getIcon() {
		return getPickBlock();
	}

	@Override
	public void onFinishCrafting(IRequester<FluidStack> requester, FluidStack stack) {
	}

	@Override
	public void onFinishCrafting(int index, int recipes) {
	}

	@Override
	public void onExtract(FluidStack stack) {
	}

	@Override
	public void markDirty() {
		baseTile.markChunkDirty();
	}

	@Override
	public int tickDelay() {
		return ServoItem.tickDelays[type];
	}

}
