package astavie.thermallogistics.attachment;

import astavie.thermallogistics.ThermalLogistics;
import astavie.thermallogistics.client.TLTextures;
import astavie.thermallogistics.client.gui.GuiCrafter;
import astavie.thermallogistics.compat.ICrafterWrapper;
import astavie.thermallogistics.container.ContainerCrafter;
import astavie.thermallogistics.process.Process;
import astavie.thermallogistics.process.ProcessItem;
import astavie.thermallogistics.process.Request;
import astavie.thermallogistics.process.RequestItem;
import astavie.thermallogistics.util.RequesterReference;
import astavie.thermallogistics.util.StackHandler;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.vec.Translation;
import codechicken.lib.vec.Vector3;
import codechicken.lib.vec.uv.IconTransformation;
import cofh.core.network.PacketBase;
import cofh.core.network.PacketHandler;
import cofh.core.network.PacketTileInfo;
import cofh.core.util.helpers.BlockHelper;
import cofh.core.util.helpers.ItemHelper;
import cofh.core.util.helpers.ServerHelper;
import cofh.thermaldynamics.ThermalDynamics;
import cofh.thermaldynamics.duct.attachments.servo.ServoItem;
import cofh.thermaldynamics.duct.item.DuctUnitItem;
import cofh.thermaldynamics.duct.item.GridItem;
import cofh.thermaldynamics.duct.item.StackMap;
import cofh.thermaldynamics.duct.item.TravelingItem;
import cofh.thermaldynamics.duct.tiles.DuctUnit;
import cofh.thermaldynamics.duct.tiles.TileGrid;
import cofh.thermaldynamics.gui.GuiHandler;
import cofh.thermaldynamics.multiblock.IGridTileRoute;
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
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.oredict.OreDictionary;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CrafterItem extends ServoItem implements ICrafter<ItemStack> {

	public static final ResourceLocation ID = new ResourceLocation(ThermalLogistics.MOD_ID, "crafter_item");

	public static final int[] SIZE = {1, 2, 3, 4, 6};
	public static final int[][] SPLITS = {{1}, {2, 1}, {3, 1}, {4, 2, 1}, {6, 3, 2, 1}};

	private final List<Recipe<ItemStack>> recipes = NonNullList.create();

	private final List<RequesterReference<?>> linked = NonNullList.create();

	private final ProcessItem process = new ProcessItem(this);
	private final RequestItem sent = new RequestItem(null);

	public CrafterItem(TileGrid tile, byte side, int type) {
		super(tile, side, type);

		Recipe<ItemStack> recipe = new Recipe<>(new RequestItem(null));
		recipe.inputs.addAll(Collections.nCopies(SIZE[type] * 2, ItemStack.EMPTY));
		recipe.outputs.addAll(Collections.nCopies(SIZE[type], ItemStack.EMPTY));

		recipes.add(recipe);
	}

	public CrafterItem(TileGrid tile, byte side) {
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
	public void tick(int pass) {
		if (pass == 0 && itemDuct.tileCache[side] != null && !(itemDuct.tileCache[side] instanceof CacheWrapper))
			itemDuct.tileCache[side] = new CacheWrapper(itemDuct.tileCache[side].tile, this);
		super.tick(pass);
	}

	@Override
	public ItemStack insertItem(ItemStack item, boolean simulate) {
		int max = Math.min(getMaxSend(), item.getCount());
		int send = 0;

		a:
		for (int i = 0; i < recipes.size(); i++) {
			Recipe<ItemStack> recipe = recipes.get(i);
			for (Iterator<Request<ItemStack>> iterator = recipe.requests.iterator(); iterator.hasNext(); ) {
				Request<ItemStack> request = iterator.next();
				int count = Math.min(request.getCount(item), max - send);
				if (count == 0)
					continue;

				IRequester<ItemStack> attachment = request.attachment.getAttachment();
				if (attachment == null)
					continue;

				Route route = itemDuct.getRoute((IGridTileRoute) attachment.getDuct());
				if (route == null)
					continue;

				if (!simulate) {
					ItemStack removed = ItemHelper.cloneStack(item, count);

					route = route.copy();
					route.pathDirections.add(attachment.getSide());

					itemDuct.insertNewItem(new TravelingItem(removed, itemDuct, route, (byte) (side ^ 1), attachment.getSpeed()));

					onFinishCrafting(recipe, i, iterator, request, removed);
					attachment.claim(this, removed);
				}

				send += count;
				if (send == max)
					break a;
			}
		}

		return ItemHelper.cloneStack(item, item.getCount() - send);
	}

	@Override
	public void handleItemSending() {
		// Check linked
		checkLinked();

		// Check requests
		boolean changed = false;

		for (Recipe<ItemStack> recipe : recipes)
			changed |= ProcessItem.checkRequests(this, recipe.requests, IRequester::getInputFrom);

		if (changed) {
			Set<ItemStack> set = new HashSet<>();

			a:
			for (ItemStack stack : process.getStacks()) {
				for (ItemStack compare : set) {
					if (itemsIdentical(stack, compare)) {
						compare.grow(stack.getCount());
						continue a;
					}
				}
				set.add(stack.copy());
			}

			Map<ItemStack, Integer> map = set.stream().collect(Collectors.toMap(Function.identity(), item -> Math.max(item.getCount() - required(item, true), 0)));
			map.entrySet().removeIf(e -> e.getValue() == 0);

			for (Iterator<Request<ItemStack>> iterator = process.requests.iterator(); iterator.hasNext() && !map.isEmpty(); ) {
				Request<ItemStack> request = iterator.next();

				for (Iterator<ItemStack> iterator1 = request.stacks.iterator(); iterator1.hasNext() && !map.isEmpty(); ) {
					ItemStack stack = iterator1.next();

					for (Iterator<Map.Entry<ItemStack, Integer>> iterator2 = map.entrySet().iterator(); iterator2.hasNext(); ) {
						Map.Entry<ItemStack, Integer> entry = iterator2.next();
						if (!itemsIdentical(entry.getKey(), stack))
							continue;

						int shrink = Math.min(stack.getCount(), entry.getValue());
						stack.shrink(shrink);
						entry.setValue(entry.getValue() - shrink);

						if (stack.isEmpty())
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
	public void onNeighborChange() {
		boolean wasPowered = isPowered;
		super.onNeighborChange();
		if (wasPowered && !isPowered) {
			process.requests.clear();
			sent.stacks.clear();

			for (Recipe<ItemStack> recipe : recipes) {
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

			for (Recipe<ItemStack> recipe : recipes) {
				recipe.requests.clear();
				recipe.leftovers.stacks.clear();
			}
		}
	}

	@Override
	public void writeToNBT(NBTTagCompound tag) {
		super.writeToNBT(tag);

		NBTTagList recipes = new NBTTagList();
		for (Recipe<ItemStack> recipe : this.recipes) {
			NBTTagList inputs = new NBTTagList();
			for (ItemStack stack : recipe.inputs)
				inputs.appendTag(stack.writeToNBT(new NBTTagCompound()));

			NBTTagList outputs = new NBTTagList();
			for (ItemStack stack : recipe.outputs)
				outputs.appendTag(stack.writeToNBT(new NBTTagCompound()));

			NBTTagList requests = new NBTTagList();
			for (Request<ItemStack> request : recipe.requests)
				requests.appendTag(RequestItem.writeNBT(request));

			NBTTagList leftovers = new NBTTagList();
			for (ItemStack stack : recipe.leftovers.stacks)
				leftovers.appendTag(stack.writeToNBT(new NBTTagCompound()));

			NBTTagCompound nbt = new NBTTagCompound();
			nbt.setTag("inputs", inputs);
			nbt.setTag("outputs", outputs);
			nbt.setTag("requests", requests);
			nbt.setTag("leftovers", leftovers);
			recipes.appendTag(nbt);
		}

		NBTTagList sent = new NBTTagList();
		for (ItemStack stack : this.sent.stacks)
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
			Recipe<ItemStack> recipe = new Recipe<>(new RequestItem(null));
			recipe.inputs.addAll(Collections.nCopies(SIZE[type] * 2, ItemStack.EMPTY));
			recipe.outputs.addAll(Collections.nCopies(SIZE[type], ItemStack.EMPTY));

			NBTTagList inputs = tag.getTagList("Inputs", Constants.NBT.TAG_COMPOUND);
			for (int i = 0; i < inputs.tagCount(); i++) {
				NBTTagCompound compound = inputs.getCompoundTagAt(i);
				recipe.inputs.set(compound.getInteger("Slot"), new ItemStack(compound));
			}

			NBTTagList outputs = tag.getTagList("Outputs", Constants.NBT.TAG_COMPOUND);
			for (int i = 0; i < outputs.tagCount(); i++) {
				NBTTagCompound compound = outputs.getCompoundTagAt(i);
				recipe.outputs.set(compound.getInteger("Slot"), new ItemStack(compound));
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

				Recipe<ItemStack> recipe = new Recipe<>(new RequestItem(null));

				NBTTagList inputs = nbt.getTagList("inputs", Constants.NBT.TAG_COMPOUND);
				for (int j = 0; j < inputs.tagCount(); j++)
					recipe.inputs.add(new ItemStack(inputs.getCompoundTagAt(j)));

				NBTTagList outputs = nbt.getTagList("outputs", Constants.NBT.TAG_COMPOUND);
				for (int j = 0; j < outputs.tagCount(); j++)
					recipe.outputs.add(new ItemStack(outputs.getCompoundTagAt(j)));

				NBTTagList requests = nbt.getTagList("requests", Constants.NBT.TAG_COMPOUND);
				for (int j = 0; j < requests.tagCount(); j++)
					recipe.requests.add(RequestItem.readNBT(requests.getCompoundTagAt(j)));

				NBTTagList leftovers = nbt.getTagList("leftovers", Constants.NBT.TAG_COMPOUND);
				for (int j = 0; j < leftovers.tagCount(); j++)
					recipe.leftovers.stacks.add(new ItemStack(leftovers.getCompoundTagAt(j)));

				this.recipes.add(recipe);
			}

			process.readNbt(tag.getTagList("process", Constants.NBT.TAG_COMPOUND));

			NBTTagList sent = tag.getTagList("sent", Constants.NBT.TAG_COMPOUND);
			for (int i = 0; i < sent.tagCount(); i++)
				this.sent.stacks.add(new ItemStack(sent.getCompoundTagAt(i)));

			NBTTagList linked = tag.getTagList("linked", Constants.NBT.TAG_COMPOUND);
			for (int i = 0; i < linked.tagCount(); i++)
				this.linked.add(RequesterReference.readNBT(linked.getCompoundTagAt(i)));
		}
	}

	@Override
	public void writePortableData(EntityPlayer player, NBTTagCompound tag) {
		super.writePortableData(player, tag);

		NBTTagList recipes = new NBTTagList();
		for (Recipe<ItemStack> recipe : this.recipes) {
			NBTTagList inputs = new NBTTagList();
			for (ItemStack stack : recipe.inputs)
				inputs.appendTag(stack.writeToNBT(new NBTTagCompound()));

			NBTTagList outputs = new NBTTagList();
			for (ItemStack stack : recipe.outputs)
				outputs.appendTag(stack.writeToNBT(new NBTTagCompound()));

			NBTTagCompound nbt = new NBTTagCompound();
			nbt.setTag("inputs", inputs);
			nbt.setTag("outputs", outputs);
			recipes.appendTag(nbt);
		}

		tag.setString("DisplayType", new ItemStack(ThermalLogistics.Items.crafter).getTranslationKey() + ".name");

		tag.setInteger("recipesType", type);
		tag.setString("recipesClass", "ItemStack");
		tag.setTag("recipes", recipes);
	}

	@Override
	public void readPortableData(EntityPlayer player, NBTTagCompound tag) {
		super.readPortableData(player, tag);

		if (tag.getInteger("recipesType") == type && tag.getString("recipesClass").equals("ItemStack")) {
			recipes.clear();
			sent.stacks.clear();

			NBTTagList recipes = tag.getTagList("recipes", Constants.NBT.TAG_COMPOUND);
			for (int i = 0; i < recipes.tagCount(); i++) {
				NBTTagCompound nbt = recipes.getCompoundTagAt(i);

				Recipe<ItemStack> recipe = new Recipe<>(new RequestItem(null));

				NBTTagList inputs = nbt.getTagList("inputs", Constants.NBT.TAG_COMPOUND);
				for (int j = 0; j < inputs.tagCount(); j++)
					recipe.inputs.add(new ItemStack(inputs.getCompoundTagAt(j)));

				NBTTagList outputs = nbt.getTagList("outputs", Constants.NBT.TAG_COMPOUND);
				for (int j = 0; j < outputs.tagCount(); j++)
					recipe.outputs.add(new ItemStack(outputs.getCompoundTagAt(j)));

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
					ItemStack stack = payload.getItemStack();

					if (recipe < recipes.size()) {
						Recipe<ItemStack> r = recipes.get(recipe);
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

							Recipe<ItemStack> recipe = new Recipe<>(new RequestItem(null));
							recipe.inputs.addAll(Collections.nCopies(SIZE[type] * 2, ItemStack.EMPTY));
							recipe.outputs.addAll(Collections.nCopies(SIZE[type], ItemStack.EMPTY));

							wrapper.populateCast(tile, (byte) (side ^ 1), recipe, ItemStack.class);

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
						Recipe<ItemStack> recipe = new Recipe<>(new RequestItem(null));

						int inputs = payload.getInt();
						for (int j = 0; j < inputs; j++)
							recipe.inputs.add(payload.getItemStack());

						int outputs = payload.getInt();
						for (int j = 0; j < outputs; j++)
							recipe.outputs.add(payload.getItemStack());

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
		ItemStack[] inputs = new ItemStack[SIZE[type] * 2];
		ItemStack[] outputs = new ItemStack[SIZE[type]];

		int recipeSize = SIZE[type] / recipes.size();

		for (int i = 0; i < recipes.size(); i++) {
			Recipe<ItemStack> recipe = recipes.get(i);

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
			Recipe<ItemStack> recipe = new Recipe<>(new RequestItem(null));

			for (int j = 0; j < split; j++) {
				recipe.inputs.add(inputs[(i * split + j) * 2]);
				recipe.inputs.add(inputs[(i * split + j) * 2 + 1]);

				recipe.outputs.add(outputs[i * split + j]);
			}

			this.recipes.add(recipe);
		}
	}

	@Override
	public Class<ItemStack> getItemClass() {
		return ItemStack.class;
	}

	private PacketTileInfo getGuiPacket() {
		PacketTileInfo packet = getNewPacket(NETWORK_ID.GUI);
		packet.addByte(0);

		packet.addInt(recipes.size());
		for (Recipe<ItemStack> recipe : recipes) {
			packet.addInt(recipe.inputs.size());
			for (ItemStack input : recipe.inputs)
				packet.addItemStack(input);

			packet.addInt(recipe.outputs.size());
			for (ItemStack output : recipe.outputs)
				packet.addItemStack(output);
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
	public void sync(EntityPlayer player) {
		PacketTileInfo packet = getNewPacket(NETWORK_ID.GUI);
		packet.addByte(1);

		writeSyncPacket(packet);
		PacketHandler.sendTo(packet, player);
	}

	@Override
	public int getIndex() {
		return 0;
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
	public List<ItemStack> getOutputs() {
		List<ItemStack> outputs = NonNullList.create();
		for (Recipe<ItemStack> recipe : recipes)
			outputs.addAll(getOutputs(recipe));
		return outputs;
	}

	private List<ItemStack> getOutputs(Recipe<ItemStack> recipe) {
		RequestItem request = new RequestItem(null);
		for (ItemStack item : recipe.outputs)
			if (!item.isEmpty())
				request.addStack(item);
		return request.stacks;
	}

	@Override
	public List<Recipe<ItemStack>> getRecipes() {
		return recipes;
	}

	@Override
	public Set<RequesterReference<ItemStack>> getBlacklist() {
		Set<RequesterReference<ItemStack>> list = new HashSet<>();
		list.add(getReference());

		for (Request<ItemStack> request : process.requests)
			list.addAll(request.blacklist);

		return list;
	}

	@Override
	public boolean request(IRequester<ItemStack> requester, ItemStack stack) {
		for (Recipe<ItemStack> recipe : recipes) {
			ItemStack output = ItemStack.EMPTY;

			for (ItemStack out : recipe.outputs) {
				if (ItemHelper.itemsIdentical(out, stack)) {
					if (output.isEmpty())
						output = out;
					else
						output.grow(out.getCount());
				}
			}

			if (output.isEmpty())
				continue;

			// Add request
			markDirty();

			for (Request<ItemStack> request : recipe.requests) {
				if (request.attachment.references(requester)) {
					request.addStack(stack);
					return true;
				}
			}

			recipe.requests.add(new RequestItem(requester.getReference(), stack));
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
	public List<ItemStack> getInputFrom(IRequester<ItemStack> requester) {
		return process.getStacks(requester);
	}

	@Override
	public List<ItemStack> getOutputTo(IRequester<ItemStack> requester) {
		List<ItemStack> stacks = NonNullList.create();
		for (Recipe<ItemStack> recipe : recipes)
			stacks.addAll(Process.getStacks(recipe.requests, requester));
		return stacks;
	}

	@Override
	public boolean isEnabled() {
		return isPowered;
	}

	private boolean itemsIdentical(ItemStack a, ItemStack b) {
		if (!filter.getFlag(4) && a.getItem().getRegistryName().getNamespace().equals(b.getItem().getRegistryName().getNamespace()))
			return true; // Same mod
		if (!filter.getFlag(3) && !Collections.disjoint(Ints.asList(OreDictionary.getOreIDs(a)), Ints.asList(OreDictionary.getOreIDs(b))))
			return true; // Same oredict

		// Same item
		return a.getItem() == b.getItem() && (filter.getFlag(1) || a.getMetadata() == b.getMetadata()) && (filter.getFlag(2) || ItemStack.areItemStackTagsEqual(a, b));
	}

	@Override
	public int amountRequired(ItemStack stack) {
		int amount = required(stack, true);

		for (ItemStack item : process.getStacks())
			if (itemsIdentical(item, stack))
				amount -= item.getCount();

		return Math.max(amount, 0);
	}

	@Override
	public float getThrottle() {
		return 0;
	}

	private int required(ItemStack stack, boolean ducts) {
		int amount = 0;
		for (int i = 0; i < recipes.size(); i++) {
			Recipe<ItemStack> recipe = recipes.get(i);

			// Get amount required per recipe
			int inputAmount = 0;
			for (ItemStack input : recipe.inputs)
				if (!input.isEmpty() && itemsIdentical(input, stack))
					inputAmount += input.getCount();

			if (inputAmount == 0)
				continue;

			// Get amount of recipes needed
			int recipes = getRequiredRecipes(i);
			for (RequesterReference<?> reference : linked)
				recipes = Math.max(recipes, ((ICrafter<?>) reference.getAttachment()).getRequiredRecipes(i));

			amount += inputAmount * recipes;
		}

		for (ItemStack item : this.sent.stacks)
			if (itemsIdentical(item, stack))
				amount -= item.getCount();

		if (ducts) {
			StackMap map = itemDuct.getGrid().travelingItems.get(itemDuct.pos().offset(EnumFacing.byIndex(side)));
			if (map != null)
				for (ItemStack item : map.getItems())
					if (itemsIdentical(item, stack))
						amount -= item.getCount();
		}

		return Math.max(amount, 0);
	}

	@Override
	public int getRequiredRecipes(int index) {
		if (index >= recipes.size())
			return 0;

		Recipe<ItemStack> recipe = recipes.get(index);

		int recipes = 0;
		for (ItemStack output : getOutputs(recipe)) {
			int count = 0;
			for (Request<ItemStack> request : recipe.requests) {
				for (ItemStack item : request.stacks) {
					if (ItemHelper.itemsIdentical(output, item)) {
						count += item.getCount();
						break;
					}
				}
			}
			count -= recipe.leftovers.getCount(output);

			if (count > 0)
				recipes = Math.max(recipes, (count - 1) / output.getCount() + 1);
		}
		return recipes;
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

	private void onFinishCrafting(Recipe<ItemStack> recipe, int i, Iterator<Request<ItemStack>> iterator, Request<ItemStack> request, ItemStack stack) {
		ItemStack output = ItemStack.EMPTY;
		for (ItemStack out : recipe.outputs) {
			if (ItemHelper.itemsIdentical(out, stack)) {
				if (output.isEmpty())
					output = out;
				else
					output.grow(out.getCount());
			}
		}

		request.decreaseStack(stack);
		if (request.stacks.isEmpty())
			iterator.remove();

		// Check leftovers
		int count = stack.getCount();

		for (Iterator<ItemStack> iterator1 = recipe.leftovers.stacks.iterator(); iterator1.hasNext(); ) {
			ItemStack leftovers = iterator1.next();
			if (ItemHelper.itemsIdentical(leftovers, stack)) {
				int amount = Math.min(leftovers.getCount(), stack.getCount());
				leftovers.shrink(amount);
				count -= amount;

				if (leftovers.isEmpty())
					iterator1.remove();

				break;
			}
		}

		int recipes = (count - 1) / output.getCount() + 1;
		if (count > 0 && recipes > 0) {
			int leftover = count % output.getCount() > 0 ? output.getCount() - (count % output.getCount()) : 0;

			// Remove sent
			for (ItemStack in : recipe.inputs)
				if (!in.isEmpty())
					sent.decreaseStack(ItemHelper.cloneStack(in, in.getCount() * recipes));

			// Add leftovers
			for (ItemStack out : getOutputs(recipe)) {
				int amount = ItemHelper.itemsIdentical(out, stack) ? leftover : out.getCount() * recipes;
				if (amount > 0)
					recipe.leftovers.addStack(ItemHelper.cloneStack(out, amount));
			}

			checkLinked();
			for (RequesterReference<?> reference : linked)
				reference.getAttachment().onFinishCrafting(i, recipes);
		}

		markDirty();
	}

	@Override
	public void onFinishCrafting(IRequester<ItemStack> requester, ItemStack stack) {
		for (int i = 0; i < recipes.size(); i++) {
			Recipe<ItemStack> recipe = recipes.get(i);
			if (recipe.requests.isEmpty())
				continue;

			for (Iterator<Request<ItemStack>> iterator = recipe.requests.iterator(); iterator.hasNext(); ) {
				Request<ItemStack> request = iterator.next();
				if (!request.attachment.references(requester))
					continue;

				onFinishCrafting(recipe, i, iterator, request, stack);
				return;
			}
		}
	}

	@Override
	public void onFinishCrafting(int index, int recipes) {
		if (index >= this.recipes.size())
			return;

		Recipe<ItemStack> recipe = this.recipes.get(index);

		// Remove sent
		for (ItemStack in : recipe.inputs)
			if (!in.isEmpty())
				sent.decreaseStack(ItemHelper.cloneStack(in, in.getCount() * recipes));

		// Add leftovers
		for (ItemStack out : recipe.outputs)
			if (!out.isEmpty())
				recipe.leftovers.addStack(ItemHelper.cloneStack(out, out.getCount() * recipes));

		markDirty();
	}

	@Override
	public void markDirty() {
		baseTile.markChunkDirty();
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

	private static class CacheWrapper extends DuctUnitItem.Cache {

		private final CrafterItem crafter;

		private CacheWrapper(@Nonnull TileEntity tile, @Nonnull CrafterItem attachment) {
			super(tile, attachment);
			this.crafter = attachment;
		}

		@Override
		public IItemHandler getItemHandler(int face) {
			return new Inventory(crafter, super.getItemHandler(EnumFacing.byIndex(face)));
		}

		@Override
		public IItemHandler getItemHandler(EnumFacing face) {
			return new Inventory(crafter, super.getItemHandler(face));
		}

	}

	private static class Inventory implements IItemHandler {

		private final CrafterItem crafter;
		private final IItemHandler inv;

		private Inventory(CrafterItem crafter, IItemHandler inv) {
			this.crafter = crafter;
			this.inv = inv;
		}

		@Override
		public int getSlots() {
			return inv.getSlots();
		}

		@Nonnull
		@Override
		public ItemStack getStackInSlot(int slot) {
			return inv.getStackInSlot(slot);
		}

		@Nonnull
		@Override
		public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
			int required = Math.min(stack.getCount(), crafter.required(stack, false));
			if (required == 0)
				return stack;

			int remaining = stack.getCount() - required;

			ItemStack remainder = inv.insertItem(slot, ItemHelper.cloneStack(stack, required), simulate);

			if (!simulate) {
				crafter.sent.addStack(ItemHelper.cloneStack(stack, required - remainder.getCount()));
				crafter.markDirty();
			}

			return ItemHelper.cloneStack(stack, remainder.getCount() + remaining);
		}

		@Nonnull
		@Override
		public ItemStack extractItem(int slot, int amount, boolean simulate) {
			return inv.extractItem(slot, amount, simulate);
		}

		@Override
		public int getSlotLimit(int slot) {
			return inv.getSlotLimit(slot);
		}

	}

}
