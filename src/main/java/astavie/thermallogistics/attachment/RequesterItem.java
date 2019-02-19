package astavie.thermallogistics.attachment;

import astavie.thermallogistics.ThermalLogistics;
import astavie.thermallogistics.client.TLTextures;
import astavie.thermallogistics.process.ProcessItem;
import astavie.thermallogistics.process.Request;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.vec.Translation;
import codechicken.lib.vec.Vector3;
import codechicken.lib.vec.uv.IconTransformation;
import cofh.core.util.helpers.ItemHelper;
import cofh.thermaldynamics.duct.attachments.retriever.RetrieverItem;
import cofh.thermaldynamics.duct.item.DuctUnitItem;
import cofh.thermaldynamics.duct.item.GridItem;
import cofh.thermaldynamics.duct.item.StackMap;
import cofh.thermaldynamics.duct.tiles.DuctUnit;
import cofh.thermaldynamics.duct.tiles.TileGrid;
import cofh.thermaldynamics.multiblock.Route;
import cofh.thermaldynamics.render.RenderDuct;
import cofh.thermaldynamics.util.ListWrapper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.items.IItemHandler;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class RequesterItem extends RetrieverItem implements IRequester<ItemStack> {

	public static final ResourceLocation ID = new ResourceLocation(ThermalLogistics.MOD_ID, "requester_item");

	private final ProcessItem process = new ProcessItem(this);

	public RequesterItem(TileGrid tile, byte side) {
		super(tile, side);
	}

	public RequesterItem(TileGrid tile, byte side, int type) {
		super(tile, side, type);
		filter.handleFlagByte(24); // Whitelist by default
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
		process.tick();
	}

	@Override
	public void claim(ICrafter<ItemStack> crafter, ItemStack stack) {
		for (Iterator<Request<ItemStack>> iterator = process.requests.iterator(); iterator.hasNext(); ) {
			Request<ItemStack> request = iterator.next();
			if (request.attachment.references(crafter)) {
				request.decreaseStack(stack);
				if (request.stacks.isEmpty())
					iterator.remove();
				return;
			}
		}
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
	public List<ItemStack> getInputFrom(IRequester<ItemStack> requester) {
		return process.getStacks(requester);
	}

	@Override
	public List<ItemStack> getOutputTo(IRequester<ItemStack> requester) {
		return Collections.emptyList();
	}

	@Override
	public boolean isEnabled() {
		return isPowered;
	}

	@Override
	public int amountRequired(ItemStack stack) {
		if (!filter.matchesFilter(stack))
			return 0;

		int required = filter.getMaxStock();

		// Items in inventory
		IItemHandler inv = getCachedInv();
		if (inv == null)
			return 0;

		for (int i = 0; i < inv.getSlots(); i++)
			if (ItemHelper.itemsIdentical(inv.getStackInSlot(i), stack))
				required -= inv.getStackInSlot(i).getCount();

		// Items in requests
		for (ItemStack item : process.getStacks())
			if (ItemHelper.itemsIdentical(item, stack))
				required -= item.getCount();

		// Items traveling
		StackMap map = itemDuct.getGrid().travelingItems.get(itemDuct.pos().offset(EnumFacing.byIndex(side)));
		if (map != null)
			for (ItemStack item : map.getItems())
				if (ItemHelper.itemsIdentical(item, stack))
					required -= item.getCount();

		return required < 0 ? 0 : required;
	}

	@Override
	public int getIndex() {
		return 0;
	}

	@Override
	public float getThrottle() {
		return 0;
	}

	@Override
	public DuctUnit getDuct() {
		return itemDuct;
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
	public ListWrapper<Route<DuctUnitItem, GridItem>> getRoutes() {
		return routesWithInsertSideList;
	}

	@Override
	public boolean hasMultiStack() {
		return multiStack[type];
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
	public void onFinishCrafting(IRequester<ItemStack> requester, ItemStack stack) {
	}

	@Override
	public void onFinishCrafting(int index, int recipes) {
	}

	@Override
	public void markDirty() {
		baseTile.markChunkDirty();
	}

}
