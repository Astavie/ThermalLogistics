package astavie.thermallogistics.attachment;

import astavie.thermallogistics.ThermalLogistics;
import astavie.thermallogistics.client.TLTextures;
import astavie.thermallogistics.process.IProcessRequesterFluid;
import astavie.thermallogistics.process.ProcessFluid;
import astavie.thermallogistics.process.Request;
import astavie.thermallogistics.util.RequesterReference;
import astavie.thermallogistics.util.StackHandler;
import astavie.thermallogistics.util.collection.FluidList;
import astavie.thermallogistics.util.collection.StackList;
import astavie.thermallogistics.util.type.Type;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.vec.Translation;
import codechicken.lib.vec.Vector3;
import codechicken.lib.vec.uv.IconTransformation;
import cofh.thermaldynamics.duct.attachments.retriever.RetrieverFluid;
import cofh.thermaldynamics.duct.attachments.servo.ServoFluid;
import cofh.thermaldynamics.duct.attachments.servo.ServoItem;
import cofh.thermaldynamics.duct.fluid.DuctUnitFluid;
import cofh.thermaldynamics.duct.fluid.GridFluid;
import cofh.thermaldynamics.duct.tiles.DuctUnit;
import cofh.thermaldynamics.duct.tiles.TileGrid;
import cofh.thermaldynamics.render.RenderDuct;
import cofh.thermaldynamics.util.ListWrapper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class RequesterFluid extends RetrieverFluid implements IProcessRequesterFluid {

	public static final ResourceLocation ID = new ResourceLocation(ThermalLogistics.MOD_ID, "requester_fluid");

	private final ProcessFluid process = new ProcessFluid(this);

	private Map<RequesterReference<FluidStack>, StackList<FluidStack>> requests = new HashMap<>();

	public RequesterFluid(TileGrid tile, byte side) {
		super(tile, side);
	}

	public RequesterFluid(TileGrid tile, byte side, int type) {
		super(tile, side, type);
		filter.handleFlagByte(24); // Whitelist by default
	}

	public static ListWrapper<Pair<DuctUnit<?, ?, ?>, Byte>> getSources(DuctUnitFluid fluidDuct, byte s) {
		LinkedList<Pair<DuctUnit<?, ?, ?>, Byte>> list = new LinkedList<>();

		for (DuctUnitFluid duct : fluidDuct.getGrid().nodeSet) {

			for (byte side = 0; side < 6; side++) {
				// Ignore self
				if (duct == fluidDuct && side == s)
					continue;

				DuctUnitFluid.Cache cache = duct.tileCache[side];
				if (cache == null || (!duct.isOutput(side) && !duct.isInput(side))) {
					continue;
				}

				list.add(Pair.of(duct, side));
			}

		}

		ListWrapper<Pair<DuctUnit<?, ?, ?>, Byte>> wrapper = new ListWrapper<>();
		wrapper.setList(list, ListWrapper.SortType.NORMAL);

		return wrapper;
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
		if (pass != 1 || grid == null || !isPowered || !isValidInput) {
			return;
		}

		process.update(false);
	}

	@Override
	public void writeToNBT(NBTTagCompound tag) {
		super.writeToNBT(tag);
		tag.setTag("requests", StackHandler.writeRequestMap(requests));
	}

	@Override
	public void readFromNBT(NBTTagCompound tag) {
		super.readFromNBT(tag);
		requests = StackHandler.readRequestMap(tag.getTagList("requests", Constants.NBT.TAG_COMPOUND), FluidList::new);
	}

	@Override
	public boolean referencedBy(RequesterReference<?> reference) {
		return reference.dim == baseTile.world().provider.getDimension() && reference.pos.equals(baseTile.getPos()) && reference.side == side;
	}

	@Override
	public RequesterReference<FluidStack> createReference() {
		return new RequesterReference<>(baseTile.world().provider.getDimension(), baseTile.getPos(), side);
	}

	@Override
	public void onFail(RequesterReference<FluidStack> crafter, Type<FluidStack> type, long amount) {
		if (requests.containsKey(crafter)) {
			StackList<FluidStack> list = requests.get(crafter);

			if (list != null) {
				list.remove(type, amount);

				if (list.isEmpty()) {
					requests.remove(crafter);
					markDirty();
				}
			}
		}
	}

	@Override
	public ItemStack getIcon() {
		return getPickBlock();
	}

	@Override
	@SuppressWarnings("deprecation")
	public ItemStack getTileIcon() {
		return myTile == null ? ItemStack.EMPTY : myTile.getBlockType().getItem(myTile.getWorld(), myTile.getPos(), myTile.getWorld().getBlockState(myTile.getPos()));
	}

	@Override
	public StackList<FluidStack> getRequestedStacks() {
		return new FluidList();
	}

	@Override
	public StackList<FluidStack> getRequestedStacks(ICrafter<FluidStack> crafter) {
		return requests.getOrDefault(crafter.createReference(), new FluidList());
	}

	@Override
	public void onFail(Type<FluidStack> type, long amount) {
	}

	@Override
	public void onCrafterSend(ICrafter<FluidStack> crafter, Type<FluidStack> type, long amount) {
		onFail(crafter.createReference(), type, amount); // Basically the same thing
	}

	@Override
	public BlockPos getDestination() {
		return baseTile.getPos().offset(EnumFacing.byIndex(side));
	}

	@Nullable
	@Override
	public DuctUnit<?, ?, ?> getDuct() {
		return fluidDuct;
	}

	@Override
	public byte getSide() {
		return (byte) (side ^ 1);
	}

	@Override
	public Map<RequesterReference<FluidStack>, StackList<FluidStack>> getRequests() {
		Map<RequesterReference<FluidStack>, StackList<FluidStack>> copy = new HashMap<>();

		for (Map.Entry<RequesterReference<FluidStack>, StackList<FluidStack>> entry : requests.entrySet())
			copy.put(entry.getKey(), entry.getValue().copy());

		return copy;
	}

	@Override
	public ListWrapper<Pair<DuctUnit<?, ?, ?>, Byte>> getSources() {
		return getSources(fluidDuct, side);
	}

	@Override
	public boolean hasWants() {
		return true;
	}

	@Override
	public long amountRequired(Type<FluidStack> type) {
		return filter.allowFluid(type.getAsStack()) ? maxFill(type) : 0;
	}

	private int maxFill(Type<FluidStack> type) {
		// Get max fill
		DuctUnitFluid.Cache cache = fluidDuct.tileCache[side];
		if (cache == null)
			return 0;

		IFluidHandler inv = cache.getHandler(side ^ 1);
		if (inv == null)
			return 0;

		int max = inv.fill(type.withAmount(Integer.MAX_VALUE), false);

		// Subtract existing requests
		GridFluid grid = fluidDuct.getGrid();
		if (grid != null && type.references(grid.getFluid()))
			max -= grid.getFluid().amount;

		for (StackList<FluidStack> list : requests.values())
			max -= list.amount(type);

		return Math.max(max, 0);
	}

	@Override
	public void addRequest(Request<FluidStack> request) {
		if (!request.isError()) {
			requests.computeIfAbsent(request.source.crafter, c -> new FluidList());
			requests.get(request.source.crafter).add(request.type, request.amount);
			markDirty();
		}
	}

	public void markDirty() {
		baseTile.markChunkDirty();
	}

	@Override
	public int tickDelay() {
		return ServoItem.tickDelays[type];
	}

	@Override
	public float throttle() {
		return ServoFluid.throttle[type];
	}

	@Override
	public int maxSize() {
		return ServoItem.maxSize[type] * Fluid.BUCKET_VOLUME;
	}

}
