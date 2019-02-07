package astavie.thermallogistics.attachment;

import astavie.thermallogistics.ThermalLogistics;
import astavie.thermallogistics.client.TLTextures;
import astavie.thermallogistics.process.Process;
import astavie.thermallogistics.process.ProcessItem;
import astavie.thermallogistics.process.Request;
import astavie.thermallogistics.process.RequestItem;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.vec.Translation;
import codechicken.lib.vec.Vector3;
import codechicken.lib.vec.uv.IconTransformation;
import cofh.core.util.helpers.ItemHelper;
import cofh.thermaldynamics.duct.attachments.servo.ServoItem;
import cofh.thermaldynamics.duct.item.DuctUnitItem;
import cofh.thermaldynamics.duct.item.GridItem;
import cofh.thermaldynamics.duct.tiles.TileGrid;
import cofh.thermaldynamics.multiblock.IGridTileRoute;
import cofh.thermaldynamics.multiblock.Route;
import cofh.thermaldynamics.render.RenderDuct;
import cofh.thermaldynamics.util.ListWrapper;
import com.google.common.collect.Iterables;
import com.google.common.primitives.Ints;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.oredict.OreDictionary;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class CrafterItem extends ServoItem implements ICrafter<ItemStack> {

	public static final ResourceLocation ID = new ResourceLocation(ThermalLogistics.MOD_ID, "crafter_item");

	private final List<Recipe<ItemStack>> recipes = NonNullList.create();

	private final ProcessItem process = new ProcessItem(this);
	private final RequestItem sent = new RequestItem(null);

	public CrafterItem(TileGrid tile, byte side, int type) {
		super(tile, side, type);
		recipes.add(new Recipe<ItemStack>() {{
			inputs.add(new ItemStack(Blocks.PLANKS, 4));
			outputs.add(new ItemStack(Blocks.CRAFTING_TABLE, 1));
		}});
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
		filter.setFlag(1, true);

		// Check requests
		for (Recipe<ItemStack> recipe : recipes)
			ProcessItem.checkRequests(this, recipe.requests, IRequester::getInputFrom);

		// Handle input
		process.tick();
	}

	@Override
	public void writePortableData(EntityPlayer player, NBTTagCompound tag) {
		super.writePortableData(player, tag);
		tag.setString("DisplayType", new ItemStack(ThermalLogistics.Items.crafter).getTranslationKey() + ".name");
	}

	@Override
	public List<ItemStack> getOutputs() {
		List<ItemStack> outputs = NonNullList.create();
		for (Recipe<ItemStack> recipe : recipes)
			outputs.addAll(recipe.outputs);
		return outputs;
	}

	@Override
	public boolean request(IRequester<ItemStack> requester, ItemStack stack) {
		for (Recipe<ItemStack> recipe : recipes) {
			if (recipe.outputs.stream().noneMatch(output -> ItemHelper.itemsIdentical(output, stack)))
				continue;

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
				int count = 0;
				for (Request<ItemStack> request : recipe.requests) {
					for (ItemStack item : request.stacks) {
						if (ItemHelper.itemsIdentical(output, item)) {
							count += item.getCount();
							break;
						}
					}
				}

				recipes = Math.max(recipes, (count - 1) / output.getCount() + 1);
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

				int count = (stack.getCount() - 1) / output.getCount() + 1;
				for (ItemStack in : recipe.inputs)
					sent.decreaseStack(ItemHelper.cloneStack(in, in.getCount() * count));

				return;
			}
		}
	}

	@Override
	public void onExtract(ItemStack stack) {
		sent.addStack(stack);
	}

}
