package astavie.thermallogistics.attachment;

import astavie.thermallogistics.ThermalLogistics;
import astavie.thermallogistics.client.TLTextures;
import astavie.thermallogistics.client.gui.GuiCrafter;
import astavie.thermallogistics.container.ContainerCrafter;
import astavie.thermallogistics.util.RequesterReference;
import astavie.thermallogistics.util.StackHandler;
import astavie.thermallogistics.util.collection.ItemList;
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
import cofh.thermaldynamics.duct.attachments.filter.IFilterItems;
import cofh.thermaldynamics.duct.attachments.servo.ServoItem;
import cofh.thermaldynamics.duct.tiles.TileGrid;
import cofh.thermaldynamics.gui.GuiHandler;
import cofh.thermaldynamics.render.RenderDuct;
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

import java.util.Collections;
import java.util.List;

public class CrafterItem extends ServoItem implements IAttachmentCrafter<ItemStack> {

	public static final ResourceLocation ID = new ResourceLocation(ThermalLogistics.MOD_ID, "crafter_item");

	public static final int[] SIZE = {1, 2, 3, 4, 6};
	public static final int[][] SPLITS = {{1}, {2, 1}, {3, 1}, {4, 2, 1}, {6, 3, 2, 1}};
	private static final IFilterItems emptyFilter = new IFilterItems() {
		@Override
		public boolean matchesFilter(ItemStack item) {
			return false;
		}

		@Override
		public boolean shouldIncRouteItems() {
			return true;
		}

		@Override
		public int getMaxStock() {
			return 0;
		}
	};
	private final List<Recipe<ItemStack>> recipes = NonNullList.create();

	public CrafterItem(TileGrid tile, byte side, int type) {
		super(tile, side, type);

		Recipe<ItemStack> recipe = newRecipe();
		recipe.inputs.addAll(Collections.nCopies(SIZE[type] * 2, ItemStack.EMPTY));
		recipe.outputs.addAll(Collections.nCopies(SIZE[type], ItemStack.EMPTY));

		recipes.add(recipe);
	}

	public CrafterItem(TileGrid tile, byte side) {
		super(tile, side);
	}

	private Recipe<ItemStack> newRecipe() {
		return new Recipe<>(this, itemDuct, ItemList::new);
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
		/*if (pass == 0) {
			if (itemDuct.tileCache[side] != null && !(itemDuct.tileCache[side] instanceof CacheWrapper)) {
				itemDuct.tileCache[side] = new CacheWrapper(itemDuct.tileCache[side].tile, this);
			}
		}*/
		super.tick(pass);
	}

	@Override
	public ItemStack insertItem(ItemStack item, boolean simulate) {
		return super.insertItem(item, simulate);
	}

	@Override
	public IFilterItems getItemFilter() {
		return emptyFilter; // No items should be routed to the crafter unless specifically asked
	}

	@Override
	public void handleItemSending() {
		super.handleItemSending();
	}

	@Override
	public void onNeighborChange() {
		boolean wasPowered = isPowered;
		super.onNeighborChange();
		if (wasPowered && !isPowered)
			for (Recipe<ItemStack> recipe : recipes)
				recipe.onDisable();
	}

	@Override
	public void checkSignal() {
		boolean wasPowered = isPowered;
		super.checkSignal();
		if (wasPowered && !isPowered)
			for (Recipe<ItemStack> recipe : recipes)
				recipe.onDisable();
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

			NBTTagCompound nbt = new NBTTagCompound();
			nbt.setTag("inputs", inputs);
			nbt.setTag("outputs", outputs);
			nbt.setTag("requestInput", StackHandler.writeRequestMap(recipe.requestInput));
			nbt.setTag("requestOutput", StackHandler.writeRequestMap(recipe.requestOutput));
			nbt.setTag("leftovers", recipe.leftovers.writeNbt());
			recipes.appendTag(nbt);
		}

		tag.setTag("recipes", recipes);
	}

	@Override
	public void readFromNBT(NBTTagCompound tag) {
		super.readFromNBT(tag);

		recipes.clear();

		if (tag.hasKey("Inputs") || tag.hasKey("Outputs") || tag.hasKey("Linked")) {
			// Version 0.1-x nbt format

			Recipe<ItemStack> recipe = newRecipe();
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
				recipe.linked.add(RequesterReference.readNBT(compound));
			}
		} else if (tag.hasKey("sent") || tag.hasKey("process") || tag.hasKey("linked")) {
			// Version 0.2-x nbt format

			NBTTagList recipes = tag.getTagList("recipes", Constants.NBT.TAG_COMPOUND);
			for (int i = 0; i < recipes.tagCount(); i++) {
				NBTTagCompound nbt = recipes.getCompoundTagAt(i);

				Recipe<ItemStack> recipe = newRecipe();

				NBTTagList inputs = nbt.getTagList("inputs", Constants.NBT.TAG_COMPOUND);
				for (int j = 0; j < inputs.tagCount(); j++)
					recipe.inputs.add(new ItemStack(inputs.getCompoundTagAt(j)));

				NBTTagList outputs = nbt.getTagList("outputs", Constants.NBT.TAG_COMPOUND);
				for (int j = 0; j < outputs.tagCount(); j++)
					recipe.outputs.add(new ItemStack(outputs.getCompoundTagAt(j)));

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
			NBTTagList recipes = tag.getTagList("recipes", Constants.NBT.TAG_COMPOUND);
			for (int i = 0; i < recipes.tagCount(); i++) {
				NBTTagCompound nbt = recipes.getCompoundTagAt(i);

				Recipe<ItemStack> recipe = newRecipe();

				NBTTagList inputs = nbt.getTagList("inputs", Constants.NBT.TAG_COMPOUND);
				for (int j = 0; j < inputs.tagCount(); j++)
					recipe.inputs.add(new ItemStack(inputs.getCompoundTagAt(j)));

				NBTTagList outputs = nbt.getTagList("outputs", Constants.NBT.TAG_COMPOUND);
				for (int j = 0; j < outputs.tagCount(); j++)
					recipe.outputs.add(new ItemStack(outputs.getCompoundTagAt(j)));

				recipe.requestInput = StackHandler.readRequestMap(nbt.getTagList("requestInput", Constants.NBT.TAG_COMPOUND), ItemList::new);
				recipe.requestOutput = StackHandler.readRequestMap(nbt.getTagList("requestOutput", Constants.NBT.TAG_COMPOUND), ItemList::new);
				recipe.leftovers.readNbt(nbt.getTagList("leftovers", Constants.NBT.TAG_COMPOUND));
				this.recipes.add(recipe);
			}
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

			NBTTagList recipes = tag.getTagList("recipes", Constants.NBT.TAG_COMPOUND);
			for (int i = 0; i < recipes.tagCount(); i++) {
				NBTTagCompound nbt = recipes.getCompoundTagAt(i);

				Recipe<ItemStack> recipe = newRecipe();

				NBTTagList inputs = nbt.getTagList("inputs", Constants.NBT.TAG_COMPOUND);
				for (int j = 0; j < inputs.tagCount(); j++)
					recipe.inputs.add(new ItemStack(inputs.getCompoundTagAt(j)));

				NBTTagList outputs = nbt.getTagList("outputs", Constants.NBT.TAG_COMPOUND);
				for (int j = 0; j < outputs.tagCount(); j++)
					recipe.outputs.add(new ItemStack(outputs.getCompoundTagAt(j)));

				this.recipes.add(recipe);
			}

			baseTile.markChunkDirty();
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
					// TODO: Remove linked
				} else if (message == 3) {
					TileEntity tile = BlockHelper.getAdjacentTileEntity(baseTile, side);
					if (tile != null) {
						// TODO: Wrapper
						/*
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
						*/
					}
				} else if (message == 4) {
					int recipe = payload.getInt();
					if (recipe < recipes.size()) {
						Recipe<ItemStack> r = recipes.get(recipe);
						for (int i = 0; i < r.inputs.size(); i++)
							r.inputs.set(i, payload.getItemStack());
						for (int i = 0; i < r.outputs.size(); i++)
							r.outputs.set(i, payload.getItemStack());
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
						Recipe<ItemStack> recipe = newRecipe();

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
					// TODO: Read linked
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
		// TODO Send linked
	}

	@Override
	public Class<ItemStack> getItemClass() {
		return ItemStack.class;
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

		int recipes = SIZE[type] / split;
		for (int i = 0; i < recipes; i++) {
			Recipe<ItemStack> recipe = newRecipe();

			for (int j = 0; j < split; j++) {
				recipe.inputs.add(inputs[(i * split + j) * 2]);
				recipe.inputs.add(inputs[(i * split + j) * 2 + 1]);

				recipe.outputs.add(outputs[i * split + j]);
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
	public List<? extends ICrafter<ItemStack>> getCrafters() {
		return recipes;
	}

}
