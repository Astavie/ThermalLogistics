package astavie.thermallogistics.attachment;

import astavie.thermallogistics.ThermalLogistics;
import astavie.thermallogistics.client.TLTextures;
import astavie.thermallogistics.process.IProcessRequesterItem;
import astavie.thermallogistics.process.ProcessItem;
import astavie.thermallogistics.process.Request;
import astavie.thermallogistics.util.RequesterReference;
import astavie.thermallogistics.util.collection.ItemList;
import astavie.thermallogistics.util.collection.ListWrapperWrapper;
import astavie.thermallogistics.util.collection.StackList;
import astavie.thermallogistics.util.type.Type;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.vec.Translation;
import codechicken.lib.vec.Vector3;
import codechicken.lib.vec.uv.IconTransformation;
import cofh.thermaldynamics.duct.attachments.retriever.RetrieverItem;
import cofh.thermaldynamics.duct.item.DuctUnitItem;
import cofh.thermaldynamics.duct.tiles.DuctUnit;
import cofh.thermaldynamics.duct.tiles.TileGrid;
import cofh.thermaldynamics.render.RenderDuct;
import cofh.thermaldynamics.util.ListWrapper;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.items.IItemHandler;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class RequesterItem extends RetrieverItem implements IProcessRequesterItem {

	public static final ResourceLocation ID = new ResourceLocation(ThermalLogistics.MOD_ID, "requester_item");

	private final ProcessItem process = new ProcessItem(this);

	public RequesterItem(TileGrid tile, byte side) {
		super(tile, side);
	}

	public RequesterItem(TileGrid tile, byte side, int type) {
		super(tile, side, type);
	}

	@Override
	public String getInfo() {
		return "tab." + ThermalLogistics.MOD_ID + ".requesterItem";
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
	public void handleItemSending() {
		process.update();
	}

	@Override
	public boolean referencedBy(RequesterReference<?> reference) {
		return reference.dim == baseTile.world().provider.getDimension() && reference.pos.equals(baseTile.getPos()) && reference.side == side;
	}

	@Override
	public RequesterReference<ItemStack> createReference() {
		return new RequesterReference<>(baseTile.world().provider.getDimension(), baseTile.getPos(), side);
	}

	@Override
	public void onFail(@Nullable RequesterReference<ItemStack> crafter, Type<ItemStack> type, long amount) {
		// TODO
	}

	@Override
	public ItemStack getIcon() {
		return getPickBlock();
	}

	@Override
	public ItemStack getTileIcon() {
		return myTile == null ? ItemStack.EMPTY : myTile.getBlockType().getItem(myTile.getWorld(), myTile.getPos(), myTile.getWorld().getBlockState(myTile.getPos()));
	}

	@Override
	public StackList<ItemStack> getRequestedStacks() {
		return new ItemList();
	}

	@Override
	public void onCrafterSend(ICrafter<ItemStack> crafter, Type<ItemStack> type, long amount) {
		// TODO
	}

	@Override
	public BlockPos getDestination() {
		return baseTile.getPos().offset(EnumFacing.byIndex(side));
	}

	@Nullable
	@Override
	public DuctUnit<?, ?, ?> getDuct() {
		return itemDuct;
	}

	@Override
	public byte getSide() {
		return (byte) (side ^ 1);
	}

	@Override
	public int maxSize() {
		return getMaxSend();
	}

	@Override
	public boolean multiStack() {
		return multiStack[type];
	}

	@Override
	public byte speedBoost() {
		return getSpeed();
	}

	@Override
	public Map<RequesterReference<ItemStack>, StackList<ItemStack>> getRequests() {
		return new HashMap<>(); // TODO
	}

	@Override
	public ListWrapper<Pair<DuctUnit, Byte>> getSources() {
		return new ListWrapperWrapper<>(routesWithInsertSideList, r -> Pair.of(r.endPoint, r.getLastSide()));
	}

	@Override
	public boolean hasWants() {
		return true;
	}

	@Override
	public long amountRequired(Type<ItemStack> type) {
		return filter.matchesFilter(type.getAsStack()) ? Math.max(0, filter.getMaxStock() - amountInside(type)) : 0;
	}

	private long amountInside(Type<ItemStack> type) {
		DuctUnitItem.Cache cache = itemDuct.tileCache[side];
		if (cache == null)
			return 0;

		IItemHandler inv = cache.getItemHandler(side ^ 1);
		if (inv == null)
			return 0;

		return DuctUnitItem.getNumItems(inv, side ^ 1, type.getAsStack(), Integer.MAX_VALUE);
	}

	@Override
	public void addRequest(Request<ItemStack> request) {
		// TODO
	}

}
