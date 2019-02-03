package astavie.thermallogistics.attachment;

import astavie.thermallogistics.ThermalLogistics;
import astavie.thermallogistics.event.EventHandler;
import astavie.thermallogistics.gui.client.GuiRequester;
import astavie.thermallogistics.item.ItemRequester;
import astavie.thermallogistics.process.IProcess;
import astavie.thermallogistics.process.ProcessFluid;
import astavie.thermallogistics.proxy.ProxyClient;
import astavie.thermallogistics.util.IProcessLoader;
import astavie.thermallogistics.util.IRequester;
import astavie.thermallogistics.util.delegate.DelegateClientFluid;
import astavie.thermallogistics.util.delegate.DelegateFluid;
import astavie.thermallogistics.util.reference.RequesterReference;
import astavie.thermallogistics.util.request.IRequest;
import astavie.thermallogistics.util.request.Request;
import astavie.thermallogistics.util.request.Requests;
import codechicken.lib.fluid.FluidUtils;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.vec.Translation;
import codechicken.lib.vec.Vector3;
import codechicken.lib.vec.uv.IconTransformation;
import cofh.core.network.PacketBase;
import cofh.core.network.PacketHandler;
import cofh.core.util.helpers.FluidHelper;
import cofh.thermaldynamics.duct.Attachment;
import cofh.thermaldynamics.duct.attachments.retriever.RetrieverFluid;
import cofh.thermaldynamics.duct.attachments.servo.ServoFluid;
import cofh.thermaldynamics.duct.fluid.DuctUnitFluid;
import cofh.thermaldynamics.duct.fluid.FluidTankGrid;
import cofh.thermaldynamics.duct.fluid.GridFluid;
import cofh.thermaldynamics.duct.tiles.TileGrid;
import cofh.thermaldynamics.render.RenderDuct;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import java.util.*;

public class RequesterFluid extends RetrieverFluid implements IRequester<DuctUnitFluid, FluidStack>, IProcessLoader {

	public static final ResourceLocation ID = new ResourceLocation(ThermalLogistics.MODID, "requester_fluid");

	// Client-only
	private final List<Requests<DuctUnitFluid, FluidStack>> requests = new LinkedList<>();

	private final List<IRequest<DuctUnitFluid, FluidStack>> leftovers = new LinkedList<>();
	private final List<ProcessFluid> processes = new LinkedList<>();

	private NBTTagList _leftovers;
	private NBTTagList _processes;

	public RequesterFluid(TileGrid tile, byte side) {
		super(tile, side);
	}

	public RequesterFluid(TileGrid tile, byte side, int type) {
		super(tile, side, type);
		filter.handleFlagByte(24); // Whitelist by default
	}

	@Override
	public void readFromNBT(NBTTagCompound tag) {
		super.readFromNBT(tag);

		_leftovers = tag.getTagList("leftovers", Constants.NBT.TAG_COMPOUND);
		_processes = tag.getTagList("Processes", Constants.NBT.TAG_COMPOUND);
		EventHandler.LOADERS.add(this);
	}

	@Override
	public void writeToNBT(NBTTagCompound tag) {
		super.writeToNBT(tag);

		NBTTagList leftovers = new NBTTagList();
		for (IRequest<DuctUnitFluid, FluidStack> stack : this.leftovers)
			leftovers.appendTag(IRequest.writeNbt(stack, getDelegate()));

		NBTTagList processes = new NBTTagList();
		for (IProcess process : this.processes)
			processes.appendTag(process.save());

		tag.setTag("leftovers", leftovers);
		tag.setTag("Processes", processes);
	}

	@Override
	public Object getGuiClient(InventoryPlayer inventory) {
		return new GuiRequester<>(inventory, this, getClientDelegate(), requests, this::sendRequestsPacket);
	}

	@Override
	public BlockPos getBase() {
		return baseTile.getPos();
	}

	@Override
	public List<Requests<DuctUnitFluid, FluidStack>> getRequests() {
		List<Requests<DuctUnitFluid, FluidStack>> list = new LinkedList<>();
		for (ProcessFluid process : processes)
			if (!getDelegate().isNull(process.getOutput()))
				list.add(new Requests<>(process, Collections.singletonList(process)));
		return list;
	}

	@Override
	public DelegateFluid getDelegate() {
		return DelegateFluid.INSTANCE;
	}

	@Override
	public DelegateClientFluid getClientDelegate() {
		return DelegateClientFluid.INSTANCE;
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
	public void loadProcesses() {
		if (_leftovers != null) {
			for (int i = 0; i < _leftovers.tagCount(); i++)
				this.leftovers.add(new Request<>(getDelegate(), _leftovers.getCompoundTagAt(i)));
			_leftovers = null;
		}
		if (_processes != null) {
			for (int i = 0; i < _processes.tagCount(); i++) {
				ProcessFluid process = new ProcessFluid(baseTile.world(), _processes.getCompoundTagAt(i));
				process.setDestination(this);
				processes.add(process);
			}
			_processes = null;
		}
	}

	@Override
	public void tick(int pass) {
		GridFluid grid = fluidDuct.getGrid();
		if (pass != 1 || grid == null || !isPowered || !isValidInput)
			return;

		if (processes.size() > 0) {
			if (!isPowered || !isValidInput) {
				processes.forEach(IProcess::remove);
				processes.clear();
			} else {
				processes.removeIf(IProcess::isRemoved);
			}
			baseTile.markChunkDirty();
		}

		FluidTankGrid tank = grid.myTank;
		int maxInput = (int) Math.ceil(tank.fluidThroughput * ServoFluid.throttle[type]);
		int c = maxInput;

		if (tank.getFluid() != null)
			if (!fluidPassesFiltering(tank.getFluid()))
				return;
		Set<CrafterFluid> crafters = new HashSet<>();
		for (Iterator<DuctUnitFluid> iterator = grid.nodeSet.iterator(); iterator.hasNext() && maxInput > 0; ) {
			DuctUnitFluid fluidDuct = iterator.next();
			for (int k = 0; k < 6 && maxInput > 0; k++) {
				int side = (k + fluidDuct.internalSideCounter) % 6;
				DuctUnitFluid.Cache cache = fluidDuct.tileCache[side];
				if (cache == null || (!fluidDuct.isOutput(side) && !fluidDuct.isInput(side)))
					continue;

				Attachment attachment = fluidDuct.parent.getAttachment(side);
				if (attachment != null)
					if (attachment instanceof CrafterFluid) {
						crafters.add((CrafterFluid) attachment);
						continue;
					} else if (!attachment.canSend())
						continue;

				IFluidHandler handler = cache.getHandler(side ^ 1);
				if (handler == null)
					continue;

				FluidStack drainFluid = handler.drain(maxInput, false);
				if (drainFluid != null) {
					int input = tank.fill(drainFluid, false);
					if (fluidPassesFiltering(drainFluid)) {
						maxInput -= tank.fill(handler.drain(input, true), true);
						if (grid.toDistribute > 0 && tank.getFluid() != null) {
							GridFluid otherGrid = fluidDuct.getGrid();
							if (otherGrid != null)
								this.fluidDuct.transfer(side, Math.min(otherGrid.myTank.getFluid().amount, otherGrid.toDistribute), false, otherGrid.myTank.getFluid(), true);
						}
					}
				}
			}
		}
		if (maxInput == c) {
			// No fluid found, let's see if any process has some leftovers
			for (CrafterFluid crafter : crafters) {
				for (FluidStack output : crafter.outputs) {
					if (output == null || !fluidPassesFiltering(output))
						continue;

					int amount = getFluidHandler().fill(FluidUtils.copy(output, Integer.MAX_VALUE), false);
					for (ProcessFluid process : processes) {
						FluidStack compare = process.getOutput();
						if (FluidHelper.isFluidEqual(compare, output))
							amount -= compare.amount;
					}
					for (IRequest<DuctUnitFluid, FluidStack> request : leftovers)
						for (FluidStack stack : request.getStacks())
							if (FluidHelper.isFluidEqual(stack, output))
								amount -= stack.amount;

					if (amount <= 0)
						continue;

					// Alright, let's do this!
					output = crafter.registerLeftover(FluidUtils.copy(output, amount), this, false);
					baseTile.markChunkDirty();

					this.leftovers.add(new Request<>(crafter, output.copy()));
					return;
				}
			}

			for (CrafterFluid crafter : crafters) {
				a:
				for (FluidStack fluid : crafter.outputs) {
					if (fluid == null || !fluidPassesFiltering(fluid))
						continue;

					int amount = getFluidHandler().fill(FluidUtils.copy(fluid, Integer.MAX_VALUE), false);
					for (ProcessFluid process : processes) {
						FluidStack compare = process.getOutput();
						if (FluidHelper.isFluidEqual(compare, fluid)) {
							if (process.getCrafter().equals(new RequesterReference<>(crafter)) && process.isStuck())
								continue a;
							amount -= compare.amount;
						}
					}
					for (IRequest<DuctUnitFluid, FluidStack> request : leftovers)
						for (FluidStack stack : request.getStacks())
							if (FluidHelper.isFluidEqual(stack, fluid))
								amount -= stack.amount;

					if (amount <= 0)
						continue;

					// Alright, let's do this!
					int sum = (int) Math.ceil((double) amount / fluid.amount);
					processes.add(new ProcessFluid(this, crafter, FluidUtils.copy(fluid, amount), sum));
					baseTile.markChunkDirty();
					return;
				}
			}
		}
	}

	public void sendRequestsPacket() {
		PacketHandler.sendToServer(getNewPacket().addByte(0));
	}

	@Override
	public void handleInfoPacketType(byte a, PacketBase payload, boolean isServer, EntityPlayer player) {
		if (a != 0) {
			super.handleInfoPacketType(a, payload, isServer, player);
		} else if (isServer) {
			// Send requests
			PacketBase packet = getNewPacket().addByte(0);

			List<Requests<DuctUnitFluid, FluidStack>> requests = getRequests();
			packet.addInt(requests.size());
			for (Requests<DuctUnitFluid, FluidStack> request : requests)
				request.writePacket(getDelegate(), packet);

			PacketHandler.sendTo(packet, player);
		} else {
			requests.clear();
			int size = payload.getInt();
			for (int i = 0; i < size; i++)
				requests.add(new Requests<>(getDelegate(), payload));
		}
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
	public DuctUnitFluid getDuct() {
		return fluidDuct;
	}

	@Override
	public byte getSide() {
		return 0;
	}

	@Override
	public int getType() {
		return 0;
	}

	@Override
	public boolean isInvalid() {
		return baseTile.isInvalid() || baseTile.getAttachment(side) != this;
	}

	@Override
	public boolean isTick() {
		return true;
	}

	@Override
	public void removeLeftover(IRequester<DuctUnitFluid, FluidStack> requester, FluidStack leftover) {
		Iterator<IRequest<DuctUnitFluid, FluidStack>> iterator = leftovers.iterator();
		while (iterator.hasNext()) {
			IRequest<DuctUnitFluid, FluidStack> next = iterator.next();
			if (next.getStart() == requester) {
				Iterator<FluidStack> it = next.getStacks().iterator();
				while (it.hasNext()) {
					FluidStack stack = it.next();
					if (FluidHelper.isFluidEqual(stack, leftover)) {
						stack.amount -= leftover.amount;
						if (stack.amount <= 0) {
							it.remove();
							if (next.getStacks().isEmpty())
								iterator.remove();
						}
						return;
					}
				}
				return;
			}
		}
	}

}
