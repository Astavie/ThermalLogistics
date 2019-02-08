package astavie.thermallogistics.attachment;

import astavie.thermallogistics.ThermalLogistics;
import astavie.thermallogistics.client.TLTextures;
import astavie.thermallogistics.client.gui.GuiCrafter;
import astavie.thermallogistics.process.Process;
import astavie.thermallogistics.process.ProcessItem;
import astavie.thermallogistics.process.Request;
import astavie.thermallogistics.process.RequestItem;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.vec.Translation;
import codechicken.lib.vec.Vector3;
import codechicken.lib.vec.uv.IconTransformation;
import cofh.core.network.PacketBase;
import cofh.core.network.PacketHandler;
import cofh.core.network.PacketTileInfo;
import cofh.core.util.helpers.ItemHelper;
import cofh.core.util.helpers.ServerHelper;
import cofh.thermaldynamics.ThermalDynamics;
import cofh.thermaldynamics.duct.attachments.servo.ServoItem;
import cofh.thermaldynamics.duct.item.DuctUnitItem;
import cofh.thermaldynamics.duct.item.GridItem;
import cofh.thermaldynamics.duct.tiles.TileGrid;
import cofh.thermaldynamics.gui.GuiHandler;
import cofh.thermaldynamics.gui.container.ContainerAttachmentBase;
import cofh.thermaldynamics.multiblock.IGridTileRoute;
import cofh.thermaldynamics.multiblock.Route;
import cofh.thermaldynamics.render.RenderDuct;
import cofh.thermaldynamics.util.ListWrapper;
import com.google.common.collect.Iterables;
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
import net.minecraftforge.oredict.OreDictionary;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class CrafterItem extends ServoItem implements ICrafter<ItemStack> {

	public static final ResourceLocation ID = new ResourceLocation(ThermalLogistics.MOD_ID, "crafter_item");

	public static final int[] SIZE = {1, 2, 3, 4, 6};
	public static final int[][] SPLITS = {{1}, {2, 1}, {3, 1}, {4, 2, 1}, {6, 3, 2, 1}};

	public final List<Recipe<ItemStack>> recipes = NonNullList.create();

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

	@Override
	public void handleItemSending() {
		// Check requests
		for (Recipe<ItemStack> recipe : recipes)
			ProcessItem.checkRequests(this, recipe.requests, IRequester::getInputFrom);

		// Handle input
		process.tick();
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

		tag.setTag("recipes", recipes);
		tag.setTag("process", process.writeNbt());
		tag.setTag("sent", sent);
	}

	@Override
	public void readFromNBT(NBTTagCompound tag) {
		super.readFromNBT(tag);

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
		tag.setTag("recipes", recipes);
	}

	@Override
	public void readPortableData(EntityPlayer player, NBTTagCompound tag) {
		super.readPortableData(player, tag);

		if (tag.getInteger("recipesType") == type) {
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
		return new ContainerAttachmentBase(inventory, this);
	}

	@Override
	public Object getGuiClient(InventoryPlayer inventory) {
		return new GuiCrafter(inventory, this);
	}

	@Override
	public void handleInfoPacketType(byte a, PacketBase payload, boolean isServer, EntityPlayer player) {
		if (a == NETWORK_ID.GUI) {
			if (isServer) {
				if (payload.getByte() == 0) {
					int recipe = payload.getInt();
					boolean input = payload.getBool();
					int index = payload.getInt();
					ItemStack stack = payload.getItemStack();

					if (recipe < recipes.size()) {
						Recipe<ItemStack> r = recipes.get(recipe);
						if (input) {
							if (index < r.inputs.size())
								r.inputs.set(index, stack);
						} else if (index < r.outputs.size())
							r.outputs.set(index, stack);
					}

					markDirty();
				} else {
					int split = payload.getInt();
					if (Ints.contains(SPLITS[type], split))
						split(split);
				}
			} else {
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
		} else super.handleInfoPacketType(a, payload, isServer, player);
	}

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

		if (ServerHelper.isServerWorld(baseTile.world()))
			markDirty();
	}

	@Override
	public boolean openGui(EntityPlayer player) {
		if (ServerHelper.isServerWorld(baseTile.world())) {
			PacketTileInfo packet = getNewPacket(NETWORK_ID.GUI);

			packet.addInt(recipes.size());
			for (Recipe<ItemStack> recipe : recipes) {
				packet.addInt(recipe.inputs.size());
				for (ItemStack input : recipe.inputs)
					packet.addItemStack(input);

				packet.addInt(recipe.outputs.size());
				for (ItemStack output : recipe.outputs)
					packet.addItemStack(output);
			}

			PacketHandler.sendTo(packet, player);
			player.openGui(ThermalDynamics.instance, GuiHandler.TILE_ATTACHMENT_ID + side, baseTile.getWorld(), baseTile.x(), baseTile.y(), baseTile.z());
		}
		return true;
	}

	@Override
	public List<ItemStack> getOutputs() {
		List<ItemStack> outputs = NonNullList.create();
		for (Recipe<ItemStack> recipe : recipes)
			outputs.addAll(recipe.outputs);
		outputs.removeIf(ItemStack::isEmpty);
		return outputs;
	}

	@Override
	public boolean request(IRequester<ItemStack> requester, ItemStack stack) {
		for (Recipe<ItemStack> recipe : recipes) {
			ItemStack output = ItemStack.EMPTY;

			for (ItemStack out : recipe.outputs) {
				if (ItemHelper.itemsIdentical(out, stack)) {
					output = out;
					break;
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
		int amount = 0;
		for (Recipe<ItemStack> recipe : recipes) {
			if (recipe.requests.isEmpty())
				continue;

			// Get amount required per recipe
			int inputAmount = 0;
			for (ItemStack input : recipe.inputs)
				if (itemsIdentical(input, stack))
					inputAmount += input.getCount();

			if (inputAmount == 0)
				continue;

			// Get amount of recipes needed
			int recipes = 0;
			for (ItemStack output : recipe.outputs) {
				if (!output.isEmpty()) {
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
			}

			amount += inputAmount * recipes;
		}

		for (ItemStack item : Iterables.concat(process.getStacks(), sent.stacks))
			if (itemsIdentical(item, stack))
				amount -= item.getCount();

		return Math.max(amount, 0);
	}

	@Override
	public IGridTileRoute getDuct() {
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
		for (Recipe<ItemStack> recipe : recipes) {
			if (recipe.requests.isEmpty())
				continue;

			ItemStack output = ItemStack.EMPTY;
			for (ItemStack out : recipe.outputs) {
				if (ItemHelper.itemsIdentical(out, stack)) {
					output = out;
					break;
				}
			}

			if (output.isEmpty())
				continue;

			for (Iterator<Request<ItemStack>> iterator = recipe.requests.iterator(); iterator.hasNext(); ) {
				Request<ItemStack> request = iterator.next();
				if (!request.attachment.references(requester))
					continue;

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

				// Remove sent
				int recipes = (count - 1) / output.getCount() + 1;
				for (ItemStack in : recipe.inputs)
					if (!in.isEmpty())
						sent.decreaseStack(ItemHelper.cloneStack(in, in.getCount() * recipes));

				// Add leftovers
				int leftover = count % output.getCount();
				for (ItemStack out : recipe.outputs) {
					if (!out.isEmpty()) {
						int amount = ItemHelper.itemsIdentical(out, stack) ? leftover : out.getCount() * recipes;
						if (amount > 0)
							recipe.leftovers.addStack(ItemHelper.cloneStack(out, amount));
					}
				}

				markDirty();
				return;
			}
		}
	}

	@Override
	public void onExtract(ItemStack stack) {
		sent.addStack(stack);
		markDirty();
	}

	@Override
	public void markDirty() {
		baseTile.markChunkDirty();
	}

}
