package astavie.thermallogistics.attachment;

import astavie.thermallogistics.ThermalLogistics;
import astavie.thermallogistics.client.TLTextures;
import astavie.thermallogistics.client.gui.GuiCrafter;
import astavie.thermallogistics.compat.ICrafterWrapper;
import astavie.thermallogistics.container.ContainerCrafter;
import astavie.thermallogistics.process.Process;
import astavie.thermallogistics.process.ProcessFluid;
import astavie.thermallogistics.process.Request;
import astavie.thermallogistics.process.RequestFluid;
import astavie.thermallogistics.util.RequesterReference;
import astavie.thermallogistics.util.StackHandler;
import codechicken.lib.fluid.FluidUtils;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.vec.Translation;
import codechicken.lib.vec.Vector3;
import codechicken.lib.vec.uv.IconTransformation;
import cofh.core.network.PacketBase;
import cofh.core.network.PacketHandler;
import cofh.core.network.PacketTileInfo;
import cofh.core.util.helpers.BlockHelper;
import cofh.core.util.helpers.FluidHelper;
import cofh.core.util.helpers.ServerHelper;
import cofh.thermaldynamics.ThermalDynamics;
import cofh.thermaldynamics.duct.attachments.filter.IFilterFluid;
import cofh.thermaldynamics.duct.attachments.servo.ServoFluid;
import cofh.thermaldynamics.duct.attachments.servo.ServoItem;
import cofh.thermaldynamics.duct.fluid.DuctUnitFluid;
import cofh.thermaldynamics.duct.item.DuctUnitItem;
import cofh.thermaldynamics.duct.item.GridItem;
import cofh.thermaldynamics.duct.tiles.DuctUnit;
import cofh.thermaldynamics.duct.tiles.TileGrid;
import cofh.thermaldynamics.gui.GuiHandler;
import cofh.thermaldynamics.multiblock.Route;
import cofh.thermaldynamics.render.RenderDuct;
import cofh.thermaldynamics.util.ListWrapper;
import com.google.common.primitives.Ints;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CrafterFluid extends ServoFluid implements ICrafter<FluidStack> {

	public static final ResourceLocation ID = new ResourceLocation(ThermalLogistics.MOD_ID, "crafter_fluid");

	public static final int[] SIZE = {1, 2, 3, 4, 6};
	public static final int[][] SPLITS = {{1}, {2, 1}, {3, 1}, {4, 2, 1}, {6, 3, 2, 1}};

	private final List<Recipe<FluidStack>> recipes = NonNullList.create();

	private final List<RequesterReference<?>> linked = NonNullList.create();

	private final ProcessFluid process = new ProcessFluid(this);
	private final RequestFluid sent = new RequestFluid(null);

	private final IFilterFluid filter = new Filter(this);

	public CrafterFluid(TileGrid tile, byte side, int type) {
		super(tile, side, type);

		Recipe<FluidStack> recipe = new Recipe<>(new RequestFluid(null));
		recipe.inputs.addAll(Collections.nCopies(SIZE[type] * 2, null));
		recipe.outputs.addAll(Collections.nCopies(SIZE[type], null));

		recipes.add(recipe);
	}

	public CrafterFluid(TileGrid tile, byte side) {
		super(tile, side);
	}

	@Override
	public boolean canSend() {
		return false;
	}

	@Override
	public boolean allowDuctConnection() {
		return true;
	}

	@Override
	public String getInfo() {
		return "tab." + ThermalLogistics.MOD_ID + ".crafterFluid";
	}

	@Override
	public ResourceLocation getId() {
		return ID;
	}

	@Override
	public ItemStack getPickBlock() {
		return new ItemStack(ThermalLogistics.Items.crafter, 1, type);
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
		RenderDuct.modelConnection[isPowered ? 1 : 2][side].render(ccRenderState, trans, new IconTransformation(TLTextures.CRAFTER[stuffed ? 1 : 0][type]));
		return true;
	}

	private void checkLinked() {
		for (Iterator<RequesterReference<?>> iterator = linked.iterator(); iterator.hasNext(); ) {
			IRequester<?> requester = iterator.next().getAttachment();
			if (!(requester instanceof ICrafter)) {
				iterator.remove();
				markDirty();
			} else {
				ICrafter<?> crafter = (ICrafter<?>) requester;
				if (!crafter.hasLinked(this)) {
					iterator.remove();
					markDirty();
				}
			}
		}
	}

	@Override
	public void claim(ICrafter<FluidStack> crafter, FluidStack stack) {
		for (Iterator<Request<FluidStack>> iterator = process.requests.iterator(); iterator.hasNext(); ) {
			Request<FluidStack> request = iterator.next();
			if (request.attachment.references(crafter)) {
				request.decreaseStack(stack);
				if (request.stacks.isEmpty())
					iterator.remove();
				return;
			}
		}
	}

	@Override
	public void tick(int pass) {
		if (pass == 0 && fluidDuct.tileCache[side] != null && !(fluidDuct.tileCache[side] instanceof CrafterFluid.CacheWrapper))
			fluidDuct.tileCache[side] = new CrafterFluid.CacheWrapper(fluidDuct.tileCache[side].tile, this);

		if (pass != 1 || fluidDuct.getGrid() == null || !isPowered || !isValidInput)
			return;

		// Check linked
		checkLinked();

		// Check requests
		boolean changed = false;

		for (Recipe<FluidStack> recipe : recipes)
			changed |= ProcessFluid.checkRequests(this, recipe.requests, IRequester::getInputFrom);

		if (changed) {
			Set<FluidStack> set = new HashSet<>();

			a:
			for (FluidStack stack : process.getStacks()) {
				for (FluidStack compare : set) {
					if (itemsIdentical(stack, compare)) {
						compare.amount += stack.amount;
						continue a;
					}
				}
				set.add(stack.copy());
			}

			Map<FluidStack, Integer> map = set.stream().collect(Collectors.toMap(Function.identity(), item -> Math.max(item.amount - required(item), 0)));
			map.entrySet().removeIf(e -> e.getValue() == 0);

			for (Iterator<Request<FluidStack>> iterator = process.requests.iterator(); iterator.hasNext() && !map.isEmpty(); ) {
				Request<FluidStack> request = iterator.next();

				for (Iterator<FluidStack> iterator1 = request.stacks.iterator(); iterator1.hasNext() && !map.isEmpty(); ) {
					FluidStack stack = iterator1.next();

					for (Iterator<Map.Entry<FluidStack, Integer>> iterator2 = map.entrySet().iterator(); iterator2.hasNext(); ) {
						Map.Entry<FluidStack, Integer> entry = iterator2.next();
						if (!itemsIdentical(entry.getKey(), stack))
							continue;

						int shrink = Math.min(stack.amount, entry.getValue());
						stack.amount -= shrink;
						entry.setValue(entry.getValue() - shrink);

						if (stack.amount <= 0)
							iterator1.remove();
						if (entry.getValue() == 0)
							iterator2.remove();

						break;
					}
				}

				if (request.stacks.isEmpty())
					iterator.remove();
			}
		}

		// Handle input
		process.tick();
	}

	@Override
	public IFilterFluid getFluidFilter() {
		return filter;
	}

	@Override
	public void onNeighborChange() {
		boolean wasPowered = isPowered;
		super.onNeighborChange();
		if (wasPowered && !isPowered) {
			process.requests.clear();
			sent.stacks.clear();

			for (Recipe<FluidStack> recipe : recipes) {
				recipe.requests.clear();
				recipe.leftovers.stacks.clear();
			}
		}
	}

	@Override
	public void checkSignal() {
		boolean wasPowered = isPowered;
		super.checkSignal();
		if (wasPowered && !isPowered) {
			process.requests.clear();
			sent.stacks.clear();

			for (Recipe<FluidStack> recipe : recipes) {
				recipe.requests.clear();
				recipe.leftovers.stacks.clear();
			}
		}
	}

	@Override
	public void writeToNBT(NBTTagCompound tag) {
		super.writeToNBT(tag);

		NBTTagList recipes = new NBTTagList();
		for (Recipe<FluidStack> recipe : this.recipes) {
			NBTTagList inputs = new NBTTagList();
			for (FluidStack stack : recipe.inputs)
				inputs.appendTag(stack == null ? new NBTTagCompound() : stack.writeToNBT(new NBTTagCompound()));

			NBTTagList outputs = new NBTTagList();
			for (FluidStack stack : recipe.outputs)
				outputs.appendTag(stack == null ? new NBTTagCompound() : stack.writeToNBT(new NBTTagCompound()));

			NBTTagList requests = new NBTTagList();
			for (Request<FluidStack> request : recipe.requests)
				requests.appendTag(RequestFluid.writeNBT(request));

			NBTTagList leftovers = new NBTTagList();
			for (FluidStack stack : recipe.leftovers.stacks)
				leftovers.appendTag(stack.writeToNBT(new NBTTagCompound()));

			NBTTagCompound nbt = new NBTTagCompound();
			nbt.setTag("inputs", inputs);
			nbt.setTag("outputs", outputs);
			nbt.setTag("requests", requests);
			nbt.setTag("leftovers", leftovers);
			recipes.appendTag(nbt);
		}

		NBTTagList sent = new NBTTagList();
		for (FluidStack stack : this.sent.stacks)
			sent.appendTag(stack.writeToNBT(new NBTTagCompound()));

		NBTTagList linked = new NBTTagList();
		for (RequesterReference<?> reference : this.linked)
			linked.appendTag(RequesterReference.writeNBT(reference));

		tag.setTag("recipes", recipes);
		tag.setTag("process", process.writeNbt());
		tag.setTag("sent", sent);
		tag.setTag("linked", linked);
	}

	@Override
	public void readFromNBT(NBTTagCompound tag) {
		super.readFromNBT(tag);

		recipes.clear();
		sent.stacks.clear();

		if (tag.hasKey("Inputs") || tag.hasKey("Outputs") || tag.hasKey("Linked")) {
			// Legacy nbt format
			Recipe<FluidStack> recipe = new Recipe<>(new RequestFluid(null));
			recipe.inputs.addAll(Collections.nCopies(SIZE[type] * 2, null));
			recipe.outputs.addAll(Collections.nCopies(SIZE[type], null));

			NBTTagList inputs = tag.getTagList("Inputs", Constants.NBT.TAG_COMPOUND);
			for (int i = 0; i < inputs.tagCount(); i++) {
				NBTTagCompound compound = inputs.getCompoundTagAt(i);
				recipe.inputs.set(compound.getInteger("Slot"), FluidStack.loadFluidStackFromNBT(compound));
			}

			NBTTagList outputs = tag.getTagList("Outputs", Constants.NBT.TAG_COMPOUND);
			for (int i = 0; i < outputs.tagCount(); i++) {
				NBTTagCompound compound = outputs.getCompoundTagAt(i);
				recipe.outputs.set(compound.getInteger("Slot"), FluidStack.loadFluidStackFromNBT(compound));
			}

			recipes.add(recipe);

			NBTTagList linked = tag.getTagList("Linked", Constants.NBT.TAG_COMPOUND);
			for (int i = 0; i < linked.tagCount(); i++) {
				NBTTagCompound compound = linked.getCompoundTagAt(i);
				this.linked.add(RequesterReference.readNBT(compound));
			}
		} else {
			NBTTagList recipes = tag.getTagList("recipes", Constants.NBT.TAG_COMPOUND);
			for (int i = 0; i < recipes.tagCount(); i++) {
				NBTTagCompound nbt = recipes.getCompoundTagAt(i);

				Recipe<FluidStack> recipe = new Recipe<>(new RequestFluid(null));

				NBTTagList inputs = nbt.getTagList("inputs", Constants.NBT.TAG_COMPOUND);
				for (int j = 0; j < inputs.tagCount(); j++)
					recipe.inputs.add(FluidStack.loadFluidStackFromNBT(inputs.getCompoundTagAt(j)));

				NBTTagList outputs = nbt.getTagList("outputs", Constants.NBT.TAG_COMPOUND);
				for (int j = 0; j < outputs.tagCount(); j++)
					recipe.outputs.add(FluidStack.loadFluidStackFromNBT(outputs.getCompoundTagAt(j)));

				NBTTagList requests = nbt.getTagList("requests", Constants.NBT.TAG_COMPOUND);
				for (int j = 0; j < requests.tagCount(); j++)
					recipe.requests.add(RequestFluid.readNBT(requests.getCompoundTagAt(j)));

				NBTTagList leftovers = nbt.getTagList("leftovers", Constants.NBT.TAG_COMPOUND);
				for (int j = 0; j < leftovers.tagCount(); j++)
					recipe.leftovers.stacks.add(FluidStack.loadFluidStackFromNBT(leftovers.getCompoundTagAt(j)));

				this.recipes.add(recipe);
			}

			process.readNbt(tag.getTagList("process", Constants.NBT.TAG_COMPOUND));

			NBTTagList sent = tag.getTagList("sent", Constants.NBT.TAG_COMPOUND);
			for (int i = 0; i < sent.tagCount(); i++)
				this.sent.stacks.add(FluidStack.loadFluidStackFromNBT(sent.getCompoundTagAt(i)));

			NBTTagList linked = tag.getTagList("linked", Constants.NBT.TAG_COMPOUND);
			for (int i = 0; i < linked.tagCount(); i++)
				this.linked.add(RequesterReference.readNBT(linked.getCompoundTagAt(i)));
		}
	}

	@Override
	public void writePortableData(EntityPlayer player, NBTTagCompound tag) {
		super.writePortableData(player, tag);

		NBTTagList recipes = new NBTTagList();
		for (Recipe<FluidStack> recipe : this.recipes) {
			NBTTagList inputs = new NBTTagList();
			for (FluidStack stack : recipe.inputs)
				inputs.appendTag(stack == null ? new NBTTagCompound() : stack.writeToNBT(new NBTTagCompound()));

			NBTTagList outputs = new NBTTagList();
			for (FluidStack stack : recipe.outputs)
				outputs.appendTag(stack == null ? new NBTTagCompound() : stack.writeToNBT(new NBTTagCompound()));

			NBTTagCompound nbt = new NBTTagCompound();
			nbt.setTag("inputs", inputs);
			nbt.setTag("outputs", outputs);
			recipes.appendTag(nbt);
		}

		tag.setString("DisplayType", new ItemStack(ThermalLogistics.Items.crafter).getTranslationKey() + ".name");

		tag.setInteger("recipesType", type);
		tag.setString("recipesClass", "FluidStack");
		tag.setTag("recipes", recipes);
	}

	@Override
	public void readPortableData(EntityPlayer player, NBTTagCompound tag) {
		super.readPortableData(player, tag);

		if (tag.getInteger("recipesType") == type && tag.getString("recipesClass").equals("FluidStack")) {
			recipes.clear();
			sent.stacks.clear();

			NBTTagList recipes = tag.getTagList("recipes", Constants.NBT.TAG_COMPOUND);
			for (int i = 0; i < recipes.tagCount(); i++) {
				NBTTagCompound nbt = recipes.getCompoundTagAt(i);

				Recipe<FluidStack> recipe = new Recipe<>(new RequestFluid(null));

				NBTTagList inputs = nbt.getTagList("inputs", Constants.NBT.TAG_COMPOUND);
				for (int j = 0; j < inputs.tagCount(); j++)
					recipe.inputs.add(FluidStack.loadFluidStackFromNBT(inputs.getCompoundTagAt(j)));

				NBTTagList outputs = nbt.getTagList("outputs", Constants.NBT.TAG_COMPOUND);
				for (int j = 0; j < outputs.tagCount(); j++)
					recipe.outputs.add(FluidStack.loadFluidStackFromNBT(outputs.getCompoundTagAt(j)));

				this.recipes.add(recipe);
			}

			markDirty();
		}
	}

	@Override
	public Object getGuiServer(InventoryPlayer inventory) {
		return new ContainerCrafter(inventory, this);
	}

	@Override
	public Object getGuiClient(InventoryPlayer inventory) {
		return new GuiCrafter(inventory, this);
	}

	@Override
	public void handleInfoPacketType(byte a, PacketBase payload, boolean isServer, EntityPlayer player) {
		if (a == NETWORK_ID.GUI) {
			if (isServer) {
				byte message = payload.getByte();
				if (message == 0) {
					int recipe = payload.getInt();
					boolean input = payload.getBool();
					int index = payload.getInt();
					FluidStack stack = payload.getFluidStack();

					if (recipe < recipes.size()) {
						Recipe<FluidStack> r = recipes.get(recipe);
						if (input) {
							if (index < r.inputs.size()) {
								r.inputs.set(index, stack);
								markDirty();
							}
						} else if (index < r.outputs.size()) {
							r.outputs.set(index, stack);
							markDirty();
						}
					}
				} else if (message == 1) {
					int split = payload.getInt();
					if (Ints.contains(SPLITS[type], split)) {
						split(split);
						markDirty();
					}
				} else if (message == 2) {
					int n = payload.getInt();
					if (n < linked.size())
						linked.remove(n);
				} else if (message == 3) {
					TileEntity tile = BlockHelper.getAdjacentTileEntity(baseTile, side);
					if (tile != null) {
						ICrafterWrapper<?> wrapper = ThermalLogistics.INSTANCE.getWrapper(tile.getClass());
						if (wrapper != null) {
							recipes.clear();
							sent.stacks.clear();

							Recipe<FluidStack> recipe = new Recipe<>(new RequestFluid(null));
							recipe.inputs.addAll(Collections.nCopies(SIZE[type] * 2, null));
							recipe.outputs.addAll(Collections.nCopies(SIZE[type], null));

							wrapper.populateCast(tile, (byte) (side ^ 1), recipe, FluidStack.class);

							recipes.add(recipe);
							markDirty();
						}
					}
				}

				// Send to clients
				PacketHandler.sendToAllAround(getGuiPacket(), baseTile);
			} else {
				byte message = payload.getByte();
				if (message == 0) {
					recipes.clear();
					int size = payload.getInt();
					for (int i = 0; i < size; i++) {
						Recipe<FluidStack> recipe = new Recipe<>(new RequestFluid(null));

						int inputs = payload.getInt();
						for (int j = 0; j < inputs; j++)
							recipe.inputs.add(payload.getFluidStack());

						int outputs = payload.getInt();
						for (int j = 0; j < outputs; j++)
							recipe.outputs.add(payload.getFluidStack());

						recipes.add(recipe);
					}
				}
				if (message == 0 || message == 1) {
					linked.clear();

					int links = payload.getInt();
					for (int i = 0; i < links; i++) {
						RequesterReference<?> reference = RequesterReference.readPacket(payload);

						int outputs = payload.getInt();
						for (int j = 0; j < outputs; j++)
							reference.outputs.add(StackHandler.readPacket(payload));

						linked.add(reference);
					}
				}
			}
		} else super.handleInfoPacketType(a, payload, isServer, player);
	}

	@Override
	public void split(int split) {
		FluidStack[] inputs = new FluidStack[SIZE[type] * 2];
		FluidStack[] outputs = new FluidStack[SIZE[type]];

		int recipeSize = SIZE[type] / recipes.size();

		for (int i = 0; i < recipes.size(); i++) {
			Recipe<FluidStack> recipe = recipes.get(i);

			for (int j = 0; j < recipeSize; j++) {
				inputs[(i * recipeSize + j) * 2] = recipe.inputs.get(j * 2);
				inputs[(i * recipeSize + j) * 2 + 1] = recipe.inputs.get(j * 2 + 1);

				outputs[i * recipeSize + j] = recipe.outputs.get(j);
			}
		}

		recipes.clear();
		sent.stacks.clear();

		int recipes = SIZE[type] / split;
		for (int i = 0; i < recipes; i++) {
			Recipe<FluidStack> recipe = new Recipe<>(new RequestFluid(null));

			for (int j = 0; j < split; j++) {
				recipe.inputs.add(inputs[(i * split + j) * 2]);
				recipe.inputs.add(inputs[(i * split + j) * 2 + 1]);

				recipe.outputs.add(outputs[i * split + j]);
			}

			this.recipes.add(recipe);
		}
	}

	@Override
	public Class<FluidStack> getItemClass() {
		return FluidStack.class;
	}

	private PacketTileInfo getGuiPacket() {
		PacketTileInfo packet = getNewPacket(NETWORK_ID.GUI);
		packet.addByte(0);

		packet.addInt(recipes.size());
		for (Recipe<FluidStack> recipe : recipes) {
			packet.addInt(recipe.inputs.size());
			for (FluidStack input : recipe.inputs)
				packet.addFluidStack(input);

			packet.addInt(recipe.outputs.size());
			for (FluidStack output : recipe.outputs)
				packet.addFluidStack(output);
		}

		writeSyncPacket(packet);
		return packet;
	}

	private void writeSyncPacket(PacketTileInfo packet) {
		checkLinked();

		packet.addInt(linked.size());
		for (RequesterReference<?> reference : linked) {
			RequesterReference.writePacket(packet, reference);

			ICrafter<?> crafter = (ICrafter<?>) reference.getAttachment();

			List<?> outputs = crafter.getOutputs();
			packet.addInt(outputs.size());
			for (Object object : outputs)
				StackHandler.writePacket(packet, object, crafter.getItemClass(), true);
		}
	}

	@Override
	public int getIndex() {
		return 0;
	}

	@Override
	public void sync(EntityPlayer player) {
		PacketTileInfo packet = getNewPacket(NETWORK_ID.GUI);
		packet.addByte(1);

		writeSyncPacket(packet);
		PacketHandler.sendTo(packet, player);
	}

	@Override
	public List<RequesterReference<?>> getLinked() {
		return linked;
	}

	@Override
	public boolean openGui(EntityPlayer player) {
		if (ServerHelper.isServerWorld(baseTile.world())) {
			PacketHandler.sendTo(getGuiPacket(), player);
			player.openGui(ThermalDynamics.instance, GuiHandler.TILE_ATTACHMENT_ID + side, baseTile.getWorld(), baseTile.x(), baseTile.y(), baseTile.z());
		}
		return true;
	}

	@Override
	public List<FluidStack> getOutputs() {
		List<FluidStack> outputs = NonNullList.create();
		for (Recipe<FluidStack> recipe : recipes)
			outputs.addAll(getOutputs(recipe));
		return outputs;
	}

	private List<FluidStack> getOutputs(Recipe<FluidStack> recipe) {
		RequestFluid request = new RequestFluid(null);
		for (FluidStack fluid : recipe.outputs)
			if (fluid != null)
				request.addStack(fluid);
		return request.stacks;
	}

	@Override
	public List<Recipe<FluidStack>> getRecipes() {
		return recipes;
	}

	@Override
	public Set<RequesterReference<FluidStack>> getBlacklist() {
		Set<RequesterReference<FluidStack>> list = new HashSet<>();
		list.add(getReference());

		for (Request<FluidStack> request : process.requests)
			list.addAll(request.blacklist);

		return list;
	}

	@Override
	public boolean request(IRequester<FluidStack> requester, FluidStack stack) {
		for (Recipe<FluidStack> recipe : recipes) {
			FluidStack output = null;

			for (FluidStack out : recipe.outputs) {
				if (FluidHelper.isFluidEqual(out, stack)) {
					if (output == null)
						output = out;
					else
						output.amount += out.amount;
				}
			}

			if (output == null)
				continue;

			// Add request
			markDirty();

			for (Request<FluidStack> request : recipe.requests) {
				if (request.attachment.references(requester)) {
					request.addStack(stack);
					return true;
				}
			}

			recipe.requests.add(new RequestFluid(requester.getReference(), stack));
			return true;
		}
		return false;
	}

	@Override
	public void link(ICrafter<?> crafter, boolean recursion) {
		if (!linked.contains(crafter.getReference())) {
			for (Iterator<RequesterReference<?>> iterator = linked.iterator(); iterator.hasNext(); ) {
				IRequester<?> requester = iterator.next().getAttachment();
				if (!(requester instanceof ICrafter)) {
					iterator.remove();
				} else {
					ICrafter<?> other = (ICrafter<?>) requester;
					if (!other.hasLinked(this))
						iterator.remove();
					else if (recursion)
						other.link(crafter, false);
				}
			}

			linked.add(crafter.getReference());
			crafter.link(this, false);

			markDirty();
		}
	}

	@Override
	public boolean hasLinked(ICrafter<?> crafter) {
		return linked.contains(crafter.getReference());
	}

	@Override
	public List<FluidStack> getInputFrom(IRequester<FluidStack> requester) {
		return process.getStacks(requester);
	}

	@Override
	public List<FluidStack> getOutputTo(IRequester<FluidStack> requester) {
		List<FluidStack> stacks = NonNullList.create();
		for (Recipe<FluidStack> recipe : recipes)
			stacks.addAll(Process.getStacks(recipe.requests, requester));
		return stacks;
	}

	@Override
	public boolean isEnabled() {
		return isPowered;
	}

	@Override
	public int amountRequired(FluidStack stack) {
		int amount = required(stack);

		for (FluidStack item : process.getStacks())
			if (itemsIdentical(item, stack))
				amount -= item.amount;

		return Math.max(amount, 0);
	}

	private int required(FluidStack stack) {
		int amount = 0;
		for (int i = 0; i < recipes.size(); i++) {
			Recipe<FluidStack> recipe = recipes.get(i);

			// Get amount required per recipe
			int inputAmount = 0;
			for (FluidStack input : recipe.inputs)
				if (input != null && itemsIdentical(input, stack))
					inputAmount += input.amount;

			if (inputAmount == 0)
				continue;

			// Get amount of recipes needed
			int recipes = getRequiredRecipes(i);
			for (RequesterReference<?> reference : linked)
				recipes = Math.max(recipes, ((ICrafter<?>) reference.getAttachment()).getRequiredRecipes(i));

			amount += inputAmount * recipes;
		}

		for (FluidStack item : this.sent.stacks)
			if (itemsIdentical(item, stack))
				amount -= item.amount;

		return Math.max(amount, 0);
	}

	@Override
	public int getMaxSend() {
		return 0;
	}

	private boolean itemsIdentical(FluidStack a, FluidStack b) {
		return a.getFluid() == b.getFluid() && (super.filter.getFlag(2) || FluidStack.areFluidStackTagsEqual(a, b));
	}

	@Override
	public float getThrottle() {
		return throttle[type];
	}

	@Override
	public int getRequiredRecipes(int index) {
		if (index >= recipes.size())
			return 0;

		Recipe<FluidStack> recipe = recipes.get(index);

		int recipes = 0;
		for (FluidStack output : getOutputs(recipe)) {
			int count = 0;
			for (Request<FluidStack> request : recipe.requests) {
				for (FluidStack item : request.stacks) {
					if (FluidHelper.isFluidEqual(output, item)) {
						count += item.amount;
						break;
					}
				}
			}
			count -= recipe.leftovers.getCount(output);

			if (count > 0)
				recipes = Math.max(recipes, (count - 1) / output.amount + 1);
		}
		return recipes;
	}

	@Override
	public DuctUnit getDuct() {
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
		for (int i = 0; i < recipes.size(); i++) {
			Recipe<FluidStack> recipe = recipes.get(i);
			if (recipe.requests.isEmpty())
				continue;

			FluidStack output = null;
			for (FluidStack out : recipe.outputs) {
				if (FluidHelper.isFluidEqual(out, stack)) {
					if (output == null)
						output = out;
					else
						output.amount += out.amount;
				}
			}

			if (output == null)
				continue;

			for (Iterator<Request<FluidStack>> iterator = recipe.requests.iterator(); iterator.hasNext(); ) {
				Request<FluidStack> request = iterator.next();
				if (!request.attachment.references(requester))
					continue;

				request.decreaseStack(stack);
				if (request.stacks.isEmpty())
					iterator.remove();

				// Check leftovers
				int count = stack.amount;

				for (Iterator<FluidStack> iterator1 = recipe.leftovers.stacks.iterator(); iterator1.hasNext(); ) {
					FluidStack leftovers = iterator1.next();
					if (FluidHelper.isFluidEqual(leftovers, stack)) {
						int amount = Math.min(leftovers.amount, stack.amount);
						leftovers.amount -= amount;
						count -= amount;

						if (leftovers.amount <= 0)
							iterator1.remove();

						break;
					}
				}

				int recipes = (count - 1) / output.amount + 1;
				if (count > 0 && recipes > 0) {
					int leftover = count % output.amount > 0 ? output.amount - (count % output.amount) : 0;

					// Remove sent
					for (FluidStack in : recipe.inputs)
						if (in != null)
							sent.decreaseStack(FluidUtils.copy(in, in.amount * recipes));

					// Add leftovers
					for (FluidStack out : getOutputs(recipe)) {
						int amount = FluidHelper.isFluidEqual(out, stack) ? leftover : out.amount * recipes;
						if (amount > 0)
							recipe.leftovers.addStack(FluidUtils.copy(out, amount));
					}

					checkLinked();
					for (RequesterReference<?> reference : linked)
						reference.getAttachment().onFinishCrafting(i, recipes);
				}

				markDirty();
				return;
			}
		}
	}

	@Override
	public void onFinishCrafting(int index, int recipes) {
		if (index >= this.recipes.size())
			return;

		Recipe<FluidStack> recipe = this.recipes.get(index);

		// Remove sent
		for (FluidStack in : recipe.inputs)
			if (in != null)
				sent.decreaseStack(FluidUtils.copy(in, in.amount * recipes));

		// Add leftovers
		for (FluidStack out : recipe.outputs)
			if (out != null)
				recipe.leftovers.addStack(FluidUtils.copy(out, out.amount * recipes));

		markDirty();
	}

	@Override
	public void markDirty() {
		baseTile.markChunkDirty();
	}

	@Override
	public int tickDelay() {
		return ServoItem.tickDelays[type];
	}

	private static class Filter implements IFilterFluid {

		private final CrafterFluid crafter;

		private Filter(CrafterFluid crafter) {
			this.crafter = crafter;
		}

		@Override
		public boolean allowFluid(FluidStack fluid) {
			return crafter.recipes.stream().anyMatch(recipe -> recipe.inputs.stream().anyMatch(input -> FluidHelper.isFluidEqual(input, fluid)));
		}

	}

	private static class CacheWrapper extends DuctUnitFluid.Cache {

		private final CrafterFluid crafter;

		private CacheWrapper(TileEntity tile, @Nonnull CrafterFluid crafter) {
			super(tile, crafter.filter);
			this.crafter = crafter;
		}

		@Override
		public IFluidHandler getHandler(int side) {
			return new Tank(crafter, super.getHandler(side));
		}

	}

	private static class Tank implements IFluidHandler {

		private final CrafterFluid crafter;
		private final IFluidHandler handler;

		private Tank(CrafterFluid crafter, IFluidHandler handler) {
			this.crafter = crafter;
			this.handler = handler;
		}

		@Override
		public IFluidTankProperties[] getTankProperties() {
			return handler.getTankProperties();
		}

		@Override
		public int fill(FluidStack resource, boolean doFill) {
			int required = Math.min(resource.amount, crafter.required(resource));
			if (required == 0)
				return 0;

			int fill = handler.fill(FluidUtils.copy(resource, required), doFill);
			if (doFill) {
				crafter.sent.addStack(FluidUtils.copy(resource, fill));
				crafter.markDirty();
			}

			return fill;
		}

		@Nullable
		@Override
		public FluidStack drain(FluidStack resource, boolean doDrain) {
			return handler.drain(resource, doDrain);
		}

		@Nullable
		@Override
		public FluidStack drain(int maxDrain, boolean doDrain) {
			return handler.drain(maxDrain, doDrain);
		}

	}

}
