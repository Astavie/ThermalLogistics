package astavie.thermallogistics.attachment;

import astavie.thermallogistics.ThermalLogistics;
import astavie.thermallogistics.event.EventHandler;
import astavie.thermallogistics.item.ItemRequester;
import astavie.thermallogistics.process.IProcess;
import astavie.thermallogistics.process.ProcessItem;
import astavie.thermallogistics.proxy.ProxyClient;
import astavie.thermallogistics.util.IDestination;
import astavie.thermallogistics.util.IProcessLoader;
import astavie.thermallogistics.util.NetworkUtils;
import astavie.thermallogistics.util.Request;
import astavie.thermallogistics.util.delegate.DelegateClientItem;
import astavie.thermallogistics.util.delegate.DelegateItem;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.vec.Translation;
import codechicken.lib.vec.Vector3;
import codechicken.lib.vec.uv.IconTransformation;
import cofh.core.util.helpers.ItemHelper;
import cofh.thermaldynamics.duct.Attachment;
import cofh.thermaldynamics.duct.attachments.retriever.RetrieverItem;
import cofh.thermaldynamics.duct.attachments.servo.ServoItem;
import cofh.thermaldynamics.duct.item.DuctUnitItem;
import cofh.thermaldynamics.duct.item.TravelingItem;
import cofh.thermaldynamics.duct.tiles.TileGrid;
import cofh.thermaldynamics.multiblock.Route;
import cofh.thermaldynamics.render.RenderDuct;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.items.IItemHandler;

import java.util.*;

public class RequesterItem extends RetrieverItem implements IProcessLoader, IDestination<DuctUnitItem, ItemStack> {

	public static final ResourceLocation ID = new ResourceLocation(ThermalLogistics.MODID, "requester_item");

	private final Set<ItemStack> leftovers = new HashSet<>();
	private final List<ProcessItem> processes = new LinkedList<>();
	private NBTTagList _processes;

	public RequesterItem(TileGrid tile, byte side) {
		super(tile, side);
	}

	public RequesterItem(TileGrid tile, byte side, int type) {
		super(tile, side, type);
		filter.handleFlagByte(24); // Whitelist by default
	}

	@Override
	public void readFromNBT(NBTTagCompound tag) {
		super.readFromNBT(tag);

		NBTTagList leftovers = tag.getTagList("leftovers", Constants.NBT.TAG_COMPOUND);
		for (int i = 0; i < leftovers.tagCount(); i++)
			this.leftovers.add(new ItemStack(leftovers.getCompoundTagAt(i))); // TODO: leftovers is useless in this class

		_processes = tag.getTagList("Processes", Constants.NBT.TAG_COMPOUND);
		EventHandler.LOADERS.add(this);
	}

	@Override
	public void writeToNBT(NBTTagCompound tag) {
		super.writeToNBT(tag);

		NBTTagList leftovers = new NBTTagList();
		for (ItemStack stack: this.leftovers)
			leftovers.appendTag(stack.writeToNBT(new NBTTagCompound()));

		NBTTagList processes = new NBTTagList();
		for (ProcessItem process : this.processes)
			processes.appendTag(process.save());

		tag.setTag("leftovers", leftovers);
		tag.setTag("Processes", processes);
	}

	@Override
	public ItemStack getDisplayStack() {
		return getPickBlock();
	}

	@Override
	public Collection<Request<DuctUnitItem, ItemStack>> getRequests() {
		return Collections.emptyList();
	}

	@Override
	public DelegateItem getDelegate() {
		return DelegateItem.INSTANCE;
	}

	@Override
	public DelegateClientItem getClientDelegate() {
		return DelegateClientItem.INSTANCE;
	}

	@Override
	public ResourceLocation getId() {
		return ID;
	}

	@Override
	public ItemStack getPickBlock() {
		return new ItemStack(ThermalLogistics.requester, 1, type);
	}

	@Override
	public String getName() {
		return "item.logistics.requester." + ItemRequester.NAMES[type] + ".name";
	}

	@Override
	public void handleItemSending() {
		Set<CrafterItem> crafters = new HashSet<>();
		for (Route route : routesWithInsertSideList) {
			DuctUnitItem end = (DuctUnitItem) route.endPoint;

			byte side = route.getLastSide();
			if ((!end.isInput(side) && !end.isOutput(side)) || !end.parent.getConnectionType(side).allowTransfer)
				continue;

			Attachment attachment = end.parent.getAttachment(side);
			if (attachment != null)
				if (attachment instanceof CrafterItem) {
					crafters.add((CrafterItem) attachment);
					continue;
				} else if (!attachment.canSend())
					continue;

			DuctUnitItem.Cache cache = end.tileCache[side];
			if (cache == null)
				continue;

			IItemHandler endCache = cache.getItemHandler(side ^ 1);
			if (endCache == null)
				continue;

			Route route1 = end.getRoute(itemDuct);
			if (route1 == null)
				continue;

			for (int slot = 0; slot < endCache.getSlots(); slot++) {
				ItemStack item = endCache.getStackInSlot(slot);
				if (item.isEmpty() || !filter.matchesFilter(item) || !cache.filter.matchesFilter(item))
					continue;

				TravelingItem ti = NetworkUtils.transfer(slot, end, side, itemDuct, this.side, route1, 0, type, false);
				if (ti != null)
					return;
			}
		}
		for (CrafterItem crafter : crafters) {
			for (ItemStack output: crafter.outputs) {
				if (output.isEmpty() || !filter.matchesFilter(output))
					continue;

				output = output.copy();
				output = crafter.registerLeftover(output, this, true);
				if (output.isEmpty())
					continue;

				output = NetworkUtils.maxTransfer(output, itemDuct, side, type, false);
				if (output.isEmpty())
					continue;

				// Alright, let's do this!
				output = crafter.registerLeftover(output, this, false);
				baseTile.markChunkDirty();

				for (ItemStack leftover : leftovers) {
					if (ItemHelper.itemsIdentical(output, leftover)) {
						leftover.grow(output.getCount());
						return;
					}
				}
				leftovers.add(output.copy());
				return;
			}
		}
		for (CrafterItem crafter : crafters) {
			a:
			for (ItemStack output: crafter.outputs) {
				for (ProcessItem process : processes)
					if (process.getCrafter() == crafter && process.isStuck())
						continue a;

				if (output.isEmpty() || !filter.matchesFilter(output))
					continue;

				// Calculate maximum transfer
				output = NetworkUtils.maxTransfer(output, itemDuct, side, type, false);
				if (output.isEmpty())
					continue;

				// Alright, let's do this!
				ProcessItem process = new ProcessItem(this, crafter, output, 1);
				processes.add(process);
				baseTile.markChunkDirty();
				return;
			}
		}
	}

	@Override
	public void loadProcesses() {
		if (_processes != null) {
			for (int i = 0; i < _processes.tagCount(); i++) {
				ProcessItem process = new ProcessItem(baseTile.world(), _processes.getCompoundTagAt(i));
				process.setDestination(this);
				processes.add(process);
			}
			_processes = null;
		}
	}

	@Override
	public void tick(int pass) {
		if (processes.size() > 0) {
			if (pass == 0 && (!isPowered || !isValidInput)) {
				processes.forEach(IProcess::remove);
				processes.clear();
			} else {
				processes.removeIf(IProcess::isRemoved);
			}
			baseTile.markChunkDirty();
		}
		super.tick(pass);
	}

	@Override
	public boolean render(IBlockAccess world, BlockRenderLayer layer, CCRenderState ccRenderState) {
		if (layer != BlockRenderLayer.SOLID)
			return false;
		Translation trans = Vector3.fromTileCenter(baseTile).translation();
		RenderDuct.modelConnection[isPowered ? 1 : 2][side].render(ccRenderState, trans, new IconTransformation(ProxyClient.REQUESTER[stuffed ? 1 : 0][type]));
		return true;
	}

	@Override
	public DuctUnitItem getDuct() {
		return itemDuct;
	}

	@Override
	public byte getSide() {
		return side;
	}

	@Override
	public int getType() {
		return type;
	}

	@Override
	public boolean isInvalid() {
		return baseTile.isInvalid() || baseTile.getAttachment(side) != this;
	}

	@Override
	public boolean isTick() {
		return baseTile.world().getTotalWorldTime() % ServoItem.tickDelays[type] == 0;
	}

	@Override
	public void removeLeftover(ItemStack leftover) {
		Iterator<ItemStack> iterator = leftovers.iterator();
		while (iterator.hasNext()) {
			ItemStack next = iterator.next();
			if (ItemHelper.itemsIdentical(next, leftover)) {
				next.shrink(leftover.getCount());
				if (next.isEmpty())
					iterator.remove();
			}
		}
	}

}
