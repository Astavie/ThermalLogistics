package astavie.thermallogistics.attachment;

import astavie.thermallogistics.ThermalLogistics;
import astavie.thermallogistics.client.TLTextures;
import astavie.thermallogistics.client.gui.GuiCrafter;
import astavie.thermallogistics.compat.ICrafterWrapper;
import astavie.thermallogistics.container.ContainerCrafter;
import astavie.thermallogistics.util.RequesterReference;
import astavie.thermallogistics.util.StackHandler;
import astavie.thermallogistics.util.collection.EmptyList;
import astavie.thermallogistics.util.collection.FluidList;
import astavie.thermallogistics.util.collection.StackList;
import astavie.thermallogistics.util.type.FluidType;
import astavie.thermallogistics.util.type.Type;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.vec.Translation;
import codechicken.lib.vec.Vector3;
import codechicken.lib.vec.uv.IconTransformation;
import cofh.core.network.PacketBase;
import cofh.core.network.PacketHandler;
import cofh.core.network.PacketTileInfo;
import cofh.core.util.helpers.BlockHelper;
import cofh.core.util.helpers.ServerHelper;
import cofh.thermaldynamics.ThermalDynamics;
import cofh.thermaldynamics.duct.attachments.filter.IFilterFluid;
import cofh.thermaldynamics.duct.attachments.servo.ServoFluid;
import cofh.thermaldynamics.duct.fluid.DuctUnitFluid;
import cofh.thermaldynamics.duct.tiles.TileGrid;
import cofh.thermaldynamics.gui.GuiHandler;
import cofh.thermaldynamics.render.RenderDuct;
import com.google.common.primitives.Ints;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
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

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class CrafterFluid extends ServoFluid implements IAttachmentCrafter<FluidStack> {

	public static final ResourceLocation ID = new ResourceLocation(ThermalLogistics.MOD_ID, "crafter_fluid");

	private final List<Recipe<FluidStack>> recipes = NonNullList.create();

	private final IFilterFluid filter2 = fluid -> checkCache() && matchesInput(fluid);

	private boolean processParallel = false;
	private int currentRecipe = 0;

	public CrafterFluid(TileGrid tile, byte side) {
		super(tile, side);
		
		// Disable redstone control
		rsMode = ControlMode.DISABLED;
	}

	public CrafterFluid(TileGrid tile, byte side, int type) {
		super(tile, side, type);

		Recipe<FluidStack> recipe = newRecipe(0);
		recipe.inputs.addAll(Collections.nCopies(CrafterItem.SIZE[type] * 2, null));
		recipe.outputs.addAll(Collections.nCopies(CrafterItem.SIZE[type], null));

		recipes.add(recipe);
		
		// Disable redstone control
		rsMode = ControlMode.DISABLED;
	}

	@Override
	public boolean processParallel() {
		return processParallel;
	}

	@Override
	public void processParallel(boolean parallel) {
		this.processParallel = parallel;
	}

	@Override
	public int currentRecipe() {
		return currentRecipe;
	}

	@Override
	public boolean canAlterRS() {
		return false;
	}

	private Recipe<FluidStack> newRecipe(int index) {
		return new Recipe.Fluid(this, index);
	}

	@Override
	public IFilterFluid getFluidFilter() {
		return filter2;
	}

	private boolean matchesInput(FluidStack fluid) {
		for (Recipe<FluidStack> recipe : recipes)
			for (FluidStack stack : recipe.inputs)
				if (stack != null && stack.isFluidEqual(fluid))
					return true;
		return false;
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
		return "tab." + ThermalLogistics.MOD_ID + ".crafterItem";
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

	@Override
	public void tick(int pass) {
		if (pass == 0) {
			checkCache();
		}

		if (pass != 1 || fluidDuct.getGrid() == null || !isPowered || !isValidInput) {
			return;
		}

		for (Recipe<FluidStack> recipe : recipes) {
			recipe.check();
		}

		boolean onlyCheck = false;
		for (int i = 0; i < recipes.size(); i++) {
			Recipe<FluidStack> recipe = recipes.get(i);
			if (recipe.isEnabled()) {
				boolean actuallyDoIt = !onlyCheck && (processParallel || currentRecipe == i);
				if (actuallyDoIt) {
					onlyCheck = recipe.updateMissing();
				}
				if (recipe.process.update(!actuallyDoIt)) {
					onlyCheck = true;
				}
			}
		}

		if (!processParallel) {
			int lastRecipe = currentRecipe;

			if (currentRecipe == -1) {
				currentRecipe = 0;
			}
			while (currentRecipe < recipes.size() && recipes.get(currentRecipe).isDone()) {
				currentRecipe++;
			}
			if (currentRecipe >= recipes.size()) {
				currentRecipe = 0;
			}
			while (currentRecipe < lastRecipe && recipes.get(currentRecipe).isDone()) {
				currentRecipe++;
			}
			if (recipes.get(currentRecipe).isDone()) {
				currentRecipe = -1;
			}

			if (lastRecipe != currentRecipe) {
				// Send to clients
				PacketHandler.sendToAllAround(getGuiPacket(), baseTile);
			}
		} else {
			currentRecipe = -1;
		}
	}

	private boolean checkCache() {
		// Very hacky solution, I know...
		if (fluidDuct.tileCache[side] != null && !(fluidDuct.tileCache[side] instanceof CrafterFluid.CacheWrapper)) {
			fluidDuct.tileCache[side] = new CrafterFluid.CacheWrapper(fluidDuct.tileCache[side].tile, this);
			return false;
		}
		return true;
	}

	@Override
	public Class<FluidStack> getItemClass() {
		return FluidStack.class;
	}

	@Override
	public List<Recipe<FluidStack>> getRecipes() {
		return recipes;
	}

	@Override
	public Supplier<StackList<FluidStack>> getSupplier() {
		return FluidList::new;
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

			NBTTagList linked = new NBTTagList();
			for (RequesterReference<?> link : recipe.linked)
				linked.appendTag(RequesterReference.writeNBT(link));

			NBTTagCompound nbt = new NBTTagCompound();
			nbt.setTag("inputs", inputs);
			nbt.setTag("outputs", outputs);
			nbt.setTag("linked", linked);
			nbt.setTag("requestInput", StackHandler.writeRequestMap(recipe.requestInput));
			nbt.setTag("requestOutput", StackHandler.writeRequestMap(recipe.requestOutput));
			nbt.setTag("leftovers", recipe.leftovers.writeNbt());
			nbt.setTag("missing", recipe.missing.writeNbt());
			nbt.setBoolean("disabled", !recipe.enabled);
			recipes.appendTag(nbt);
		}

		tag.setBoolean("parallel", processParallel);
		tag.setInteger("recipe", currentRecipe);
		tag.setTag("recipes", recipes);
	}

	@Override
	public void readFromNBT(NBTTagCompound tag) {
		super.readFromNBT(tag);

		recipes.clear();

		// 0.2-x enabled format
		boolean enabled = tag.hasKey("rsMode") && tag.getByte("rsMode") == 0;

		if (tag.hasKey("Inputs") || tag.hasKey("Outputs") || tag.hasKey("Linked")) {
			// Version 0.1-x nbt format

			Recipe<FluidStack> recipe = newRecipe(0);
			recipe.enabled = true;
			recipe.inputs.addAll(Collections.nCopies(CrafterItem.SIZE[type] * 2, null));
			recipe.outputs.addAll(Collections.nCopies(CrafterItem.SIZE[type], null));

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
				recipe.linked.add(RequesterReference.readNBT(compound));
			}
		} else if (tag.hasKey("sent") || tag.hasKey("process") || tag.hasKey("linked")) {
			// Version 0.2-x nbt format

			NBTTagList recipes = tag.getTagList("recipes", Constants.NBT.TAG_COMPOUND);
			for (int i = 0; i < recipes.tagCount(); i++) {
				NBTTagCompound nbt = recipes.getCompoundTagAt(i);

				Recipe<FluidStack> recipe = newRecipe(i);
				recipe.enabled = enabled;

				NBTTagList inputs = nbt.getTagList("inputs", Constants.NBT.TAG_COMPOUND);
				for (int j = 0; j < inputs.tagCount(); j++)
					recipe.inputs.add(FluidStack.loadFluidStackFromNBT(inputs.getCompoundTagAt(j)));

				NBTTagList outputs = nbt.getTagList("outputs", Constants.NBT.TAG_COMPOUND);
				for (int j = 0; j < outputs.tagCount(); j++)
					recipe.outputs.add(FluidStack.loadFluidStackFromNBT(outputs.getCompoundTagAt(j)));

				this.recipes.add(recipe);
			}

			NBTTagList linked = tag.getTagList("linked", Constants.NBT.TAG_COMPOUND);
			for (int i = 0; i < linked.tagCount(); i++) {
				NBTTagCompound compound = linked.getCompoundTagAt(i);

				for (int j = 0; j < this.recipes.size(); j++) {
					compound.setInteger("index", j);
					this.recipes.get(j).linked.add(RequesterReference.readNBT(compound));
				}
			}
		} else {
			// Version 0.3+ nbt format

			processParallel = tag.getBoolean("parallel");
			currentRecipe = tag.getInteger("recipe");

			NBTTagList recipes = tag.getTagList("recipes", Constants.NBT.TAG_COMPOUND);
			for (int i = 0; i < recipes.tagCount(); i++) {
				NBTTagCompound nbt = recipes.getCompoundTagAt(i);

				Recipe<FluidStack> recipe = newRecipe(i);

				NBTTagList inputs = nbt.getTagList("inputs", Constants.NBT.TAG_COMPOUND);
				for (int j = 0; j < inputs.tagCount(); j++)
					recipe.inputs.add(FluidStack.loadFluidStackFromNBT(inputs.getCompoundTagAt(j)));

				NBTTagList outputs = nbt.getTagList("outputs", Constants.NBT.TAG_COMPOUND);
				for (int j = 0; j < outputs.tagCount(); j++)
					recipe.outputs.add(FluidStack.loadFluidStackFromNBT(outputs.getCompoundTagAt(j)));

				NBTTagList linked = nbt.getTagList("linked", Constants.NBT.TAG_COMPOUND);
				for (int j = 0; j < linked.tagCount(); j++)
					recipe.linked.add(RequesterReference.readNBT(linked.getCompoundTagAt(j)));

				recipe.requestInput = StackHandler.readRequestMap(nbt.getTagList("requestInput", Constants.NBT.TAG_COMPOUND), FluidList::new);
				recipe.requestOutput = StackHandler.readRequestMap(nbt.getTagList("requestOutput", Constants.NBT.TAG_COMPOUND), FluidList::new);
				recipe.leftovers.readNbt(nbt.getTagList("leftovers", Constants.NBT.TAG_COMPOUND));
				recipe.missing.readNbt(nbt.getTagList("missing", Constants.NBT.TAG_COMPOUND));
				recipe.enabled = enabled || (nbt.hasKey("disabled") && !nbt.getBoolean("disabled"));
				this.recipes.add(recipe);
			}
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
			processParallel = tag.getBoolean("parallel");
			recipes.clear();

			NBTTagList recipes = tag.getTagList("recipes", Constants.NBT.TAG_COMPOUND);
			for (int i = 0; i < recipes.tagCount(); i++) {
				NBTTagCompound nbt = recipes.getCompoundTagAt(i);

				Recipe<FluidStack> recipe = newRecipe(i);

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
					if (Ints.contains(CrafterItem.SPLITS[type], split)) {
						split(split);
						markDirty();
					}
				} else if (message == 2) {
					int index1 = payload.getInt();
					int index2 = payload.getInt();

					if (index1 < recipes.size()) {
						Recipe<FluidStack> recipe = recipes.get(index1);
						if (index2 < recipe.linked.size()) {
							recipe.unlink((ICrafter<?>) recipe.linked.get(index2).get());
						}
					}
				} else if (message == 3) {
					TileEntity tile = BlockHelper.getAdjacentTileEntity(baseTile, side);
					if (tile != null) {
						ICrafterWrapper<?> wrapper = ThermalLogistics.INSTANCE.getWrapper(tile.getClass());
						if (wrapper != null) {
							List<RequesterReference<?>> linked = recipes.get(0).linked;
							recipes.clear();

							Recipe<FluidStack> recipe = newRecipe(0);
							recipe.inputs.addAll(Collections.nCopies(CrafterItem.SIZE[type] * 2, null));
							recipe.outputs.addAll(Collections.nCopies(CrafterItem.SIZE[type], null));

							wrapper.populateCast(tile, (byte) (side ^ 1), recipe, FluidStack.class);

							recipe.linked.addAll(linked);
							recipes.add(recipe);
							markDirty();
						}
					}
				} else if (message == 4) {
					int recipe = payload.getInt();
					if (recipe < recipes.size()) {
						Recipe<FluidStack> r = recipes.get(recipe);
						for (int i = 0; i < r.inputs.size(); i++)
							r.inputs.set(i, payload.getFluidStack());
						for (int i = 0; i < r.outputs.size(); i++)
							r.outputs.set(i, payload.getFluidStack());
						markDirty();
					}
				} else if (message == 5) {
					// Link!
					int index = payload.getInt();

					if (index >= 0 && index < recipes.size()) {
						ItemStack stack = player.inventory.getItemStack();

						if (stack.getItem() == ThermalLogistics.Items.manager) {
							ThermalLogistics.Items.manager.link(player, stack, recipes.get(index));
							((EntityPlayerMP) player).updateHeldItem();
							markDirty();
						}
					}
				} else if (message == 6) {
					// Enable / disable
					int index = payload.getInt();

					if (index >= 0 && index < recipes.size()) {
						Recipe<FluidStack> recipe = recipes.get(index);
						recipe.toggleEnabled();
						markDirty();
					}
				} else if (message == 7) {
					processParallel = !processParallel;
					markDirty();
				}

				// Send to clients
				PacketHandler.sendToAllAround(getGuiPacket(), baseTile);
			} else {
				byte message = payload.getByte();
				if (message == 0) {
					recipes.clear();
					int size = payload.getInt();
					for (int i = 0; i < size; i++) {
						Recipe<FluidStack> recipe = newRecipe(i);

						int inputs = payload.getInt();
						for (int j = 0; j < inputs; j++)
							recipe.inputs.add(payload.getFluidStack());

						int outputs = payload.getInt();
						for (int j = 0; j < outputs; j++)
							recipe.outputs.add(payload.getFluidStack());

						recipe.enabled = payload.getBool();

						recipes.add(recipe);
					}

					processParallel = payload.getBool();
					currentRecipe = payload.getInt();
				}
				if (message == 0 || message == 1) {
					int size = payload.getInt();
					for (int i = 0; i < size; i++) {
						if (i >= recipes.size()) {
							break;
						}

						Recipe<FluidStack> recipe = recipes.get(i);

						// Linked
						int linked = payload.getInt();

						recipe.linked.clear();

						for (int j = 0; j < linked; j++) {
							recipe.linked.add(RequesterReference.readPacket(payload));
						}

						// Input
						recipe.requestInput = StackHandler.readRequestMap(payload, FluidList::new);
						recipe.missing.readPacket(payload);

						// Output
						recipe.requestOutput = StackHandler.readRequestMap(payload, FluidList::new);
						recipe.leftovers.readPacket(payload);
					}
				}
			}
		} else super.handleInfoPacketType(a, payload, isServer, player);
	}

	@Override
	public boolean openGui(EntityPlayer player) {
		if (ServerHelper.isServerWorld(baseTile.world())) {
			PacketHandler.sendTo(getGuiPacket(), player);
			player.openGui(ThermalDynamics.instance, GuiHandler.TILE_ATTACHMENT_ID + side, baseTile.getWorld(), baseTile.x(), baseTile.y(), baseTile.z());
		}
		return true;
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

			packet.addBool(recipe.enabled);
		}

		packet.addBool(processParallel);
		packet.addInt(currentRecipe);

		writeSyncPacket(packet);
		return packet;
	}

	private void writeSyncPacket(PacketTileInfo packet) {
		checkLinked();

		packet.addInt(recipes.size());
		for (Recipe<FluidStack> recipe : recipes) {

			// Linked
			packet.addInt(recipe.linked.size());
			for (RequesterReference<?> reference : recipe.linked) {
				RequesterReference.writePacket(packet, reference);
			}

			// Input
			StackHandler.writeRequestMap(recipe.requestInput, packet);
			recipe.missing.writePacket(packet);

			// Output
			StackHandler.writeRequestMap(recipe.requestOutput, packet);
			recipe.leftovers.writePacket(packet);
		}
	}

	private void checkLinked() {
		for (Recipe<FluidStack> recipe : recipes) {
			recipe.checkLinked();
		}
	}

	@Override
	public void split(int split) {
		FluidStack[] inputs = new FluidStack[CrafterItem.SIZE[type] * 2];
		FluidStack[] outputs = new FluidStack[CrafterItem.SIZE[type]];

		int recipeSize = CrafterItem.SIZE[type] / recipes.size();

		for (int i = 0; i < recipes.size(); i++) {
			Recipe<FluidStack> recipe = recipes.get(i);

			for (int j = 0; j < recipeSize; j++) {
				inputs[(i * recipeSize + j) * 2] = recipe.inputs.get(j * 2);
				inputs[(i * recipeSize + j) * 2 + 1] = recipe.inputs.get(j * 2 + 1);

				outputs[i * recipeSize + j] = recipe.outputs.get(j);
			}
		}

		List<List<RequesterReference<?>>> lists = recipes.stream().map(r -> r.linked).collect(Collectors.toList());

		recipes.clear();

		int recipes = CrafterItem.SIZE[type] / split;
		for (int i = 0; i < recipes; i++) {
			Recipe<FluidStack> recipe = newRecipe(i);

			for (int j = 0; j < split; j++) {
				recipe.inputs.add(inputs[(i * split + j) * 2]);
				recipe.inputs.add(inputs[(i * split + j) * 2 + 1]);

				recipe.outputs.add(outputs[i * split + j]);
			}

			if (i < lists.size()) {
				recipe.linked.addAll(lists.get(i));
			}
			this.recipes.add(recipe);
		}
	}

	@Override
	public void sync(EntityPlayer player) {
		PacketTileInfo packet = getNewPacket(NETWORK_ID.GUI);
		packet.addByte(1);

		writeSyncPacket(packet);
		PacketHandler.sendTo(packet, player);
	}

	@Override
	public void markDirty() {
		baseTile.markChunkDirty();
	}

	@Override
	public List<? extends ICrafter<FluidStack>> getCrafters() {
		return recipes;
	}

	private void handleInsertedStack(Type<FluidStack> type, long amount) {
		for (Recipe<FluidStack> recipe : recipes) {
			amount = recipe.requestInput.getOrDefault(null, EmptyList.getInstance()).remove(type, amount);
			if (amount == 0) {
				return;
			}
		}
	}

	private static class CacheWrapper extends DuctUnitFluid.Cache {

		private final CrafterFluid crafter;

		private CacheWrapper(TileEntity tile, CrafterFluid attachment) {
			super(tile, attachment.getFluidFilter());
			this.crafter = attachment;
		}

		@Override
		public IFluidHandler getHandler(int side) {
			return new Tank(crafter, super.getHandler(side));
		}

	}

	private static class Tank implements IFluidHandler {

		private final CrafterFluid crafter;
		private final IFluidHandler inv;

		private Tank(CrafterFluid crafter, IFluidHandler inv) {
			this.crafter = crafter;
			this.inv = inv;
		}

		@Override
		public IFluidTankProperties[] getTankProperties() {
			return inv.getTankProperties();
		}

		@Override
		public int fill(FluidStack resource, boolean doFill) {
			if (!doFill && StackHandler.SIM) {
				return inv.fill(resource, false);
			}

			long required = 0;

			Type<FluidStack> type = new FluidType(resource);
			for (Recipe<FluidStack> recipe : crafter.recipes) {
				required += recipe.requestInput.getOrDefault(null, EmptyList.getInstance()).amount(type);
			}

			if (required == 0) {
				return 0;
			}

			long insert = Math.min(resource.amount, required);
			int filled = inv.fill(type.withAmount((int) insert), doFill);

			if (doFill && filled > 0) {
				crafter.handleInsertedStack(type, filled);
			}

			return filled;
		}

		@Nullable
		@Override
		public FluidStack drain(FluidStack resource, boolean doDrain) {
			return inv.drain(resource, doDrain);
		}

		@Nullable
		@Override
		public FluidStack drain(int maxDrain, boolean doDrain) {
			return inv.drain(maxDrain, doDrain);
		}

	}

}
