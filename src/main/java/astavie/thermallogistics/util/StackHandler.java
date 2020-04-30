package astavie.thermallogistics.util;

import astavie.thermallogistics.attachment.ICrafter;
import astavie.thermallogistics.attachment.ICrafterContainer;
import astavie.thermallogistics.attachment.IRequester;
import astavie.thermallogistics.attachment.IRequesterContainer;
import astavie.thermallogistics.client.gui.GuiCrafter;
import astavie.thermallogistics.client.gui.element.ElementSlotFluid;
import astavie.thermallogistics.client.gui.element.ElementSlotItem;
import astavie.thermallogistics.process.IProcessRequester;
import astavie.thermallogistics.util.collection.StackList;
import astavie.thermallogistics.util.type.Type;
import cofh.core.gui.GuiContainerCore;
import cofh.core.gui.element.ElementBase;
import cofh.core.network.PacketBase;
import cofh.core.util.helpers.InventoryHelper;
import cofh.core.util.helpers.ItemHelper;
import cofh.core.util.helpers.RenderHelper;
import cofh.core.util.helpers.StringHelper;
import cofh.thermaldynamics.duct.item.DuctUnitItem;
import cofh.thermaldynamics.duct.item.SimulatedInv;
import cofh.thermaldynamics.duct.item.StackMap;
import gnu.trove.iterator.TObjectIntIterator;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.IItemHandler;

import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class StackHandler {

	public static boolean SIM = false;

	@SideOnly(Side.CLIENT)
	public static void render(GuiContainerCore gui, int x, int y, Object item, boolean count) {
		if (item instanceof ItemStack) {
			ItemStack stack = (ItemStack) item;

			FontRenderer font = null;
			if (!stack.isEmpty()) {
				font = stack.getItem().getFontRenderer(stack);
			}
			if (font == null) {
				font = gui.getFontRenderer();
			}

			RenderHelper.enableGUIStandardItemLighting();
			gui.itemRender.renderItemAndEffectIntoGUI(stack, x, y);
			gui.itemRender.renderItemOverlayIntoGUI(font, stack, x, y - (gui.draggedStack.isEmpty() ? 0 : 8), count ? null : "");
		} else if (item instanceof FluidStack) {
			FluidStack fluid = (FluidStack) item;

			if (count) {
				String amount = StringHelper.formatNumber(fluid.amount);
				render(gui, x, y, item, amount);
			} else {
				GlStateManager.disableLighting();
				gui.drawFluid(x, y, fluid, 16, 16);
			}
		} else throw new IllegalArgumentException("Unknown item type " + item.getClass().getName());
	}

	@SideOnly(Side.CLIENT)
	public static void render(GuiContainerCore gui, int x, int y, Object item, String text) {
		if (item instanceof ItemStack) {
			RenderHelper.enableGUIStandardItemLighting();
			gui.drawItemStack((ItemStack) item, x, y, true, "");
		} else if (item instanceof FluidStack) {
			GlStateManager.disableLighting();
			gui.drawFluid(x, y, (FluidStack) item, 16, 16);
		} else throw new IllegalArgumentException("Unknown item type " + item.getClass().getName());

		if (text != null) {
			GlStateManager.disableLighting();
			GlStateManager.disableDepth();
			GlStateManager.disableBlend();

			GlStateManager.pushMatrix();

			GlStateManager.scale(0.5, 0.5, 0.5);
			gui.getFontRenderer().drawStringWithShadow(text, (x + 16) * 2 - gui.getFontRenderer().getStringWidth(text), (y + 12) * 2, 0xFFFFFF);

			GlStateManager.popMatrix();

			GlStateManager.enableLighting();
			GlStateManager.enableDepth();
		}
	}

	@SideOnly(Side.CLIENT)
	public static List<String> getTooltip(GuiContainerCore gui, Object item) {
		if (item instanceof ItemStack)
			return gui.getItemToolTip((ItemStack) item);
		else if (item instanceof FluidStack)
			return Collections.singletonList(((FluidStack) item).getLocalizedName());
		else throw new IllegalArgumentException("Unknown item type " + item.getClass().getName());
	}

	public static <I> void addRequesters(Collection<IRequester<I>> collection, Object object) {
		if (object instanceof IRequester) {
			//noinspection unchecked
			collection.add((IRequester<I>) object);
		} else if (object instanceof IRequesterContainer) {
			for (IRequester<?> requester : ((IRequesterContainer<?>) object).getRequesters())
				addRequesters(collection, requester);
		}
	}

	public static <I> void addCrafters(Collection<ICrafter<I>> collection, Object object) {
		if (object instanceof ICrafter) {
			if (((ICrafter) object).isEnabled())
				//noinspection unchecked
				collection.add((ICrafter<I>) object);
		} else if (object instanceof ICrafterContainer) {
			for (ICrafter<?> crafter : ((ICrafterContainer<?>) object).getCrafters())
				addCrafters(collection, crafter);
		}
	}

	public static <I> boolean forEachCrafter(Object object, Predicate<ICrafter<I>> function) {
		if (object instanceof ICrafter) {
			if (((ICrafter) object).isEnabled())
				//noinspection unchecked
				return function.test((ICrafter<I>) object);
		} else if (object instanceof ICrafterContainer) {
			for (ICrafter<?> crafter : ((ICrafterContainer<?>) object).getCrafters())
				if (forEachCrafter(crafter, function))
					return true;
		}
		return false;
	}

	public static <I> void addCraftable(StackList<I> list, Object object) {
		if (object instanceof ICrafter) {
			if (((ICrafter) object).isEnabled())
				//noinspection unchecked
				for (Type<I> type : ((ICrafter<I>) object).getOutputs().types())
					list.addCraftable(type);
		} else if (object instanceof ICrafterContainer) {
			for (ICrafter<?> crafter : ((ICrafterContainer<?>) object).getCrafters())
				addCraftable(list, crafter);
		}
	}

	public static <I> NBTTagList writeRequestMap(Map<RequesterReference<I>, StackList<I>> map) {
		NBTTagList list = new NBTTagList();

		for (Map.Entry<RequesterReference<I>, StackList<I>> entry : map.entrySet()) {
			if (!entry.getValue().isEmpty()) {
				NBTTagCompound tag = new NBTTagCompound();
				tag.setTag("reference", RequesterReference.writeNBT(entry.getKey()));
				tag.setTag("stacks", entry.getValue().writeNbt());
				list.appendTag(tag);
			}
		}

		return list;
	}

	public static <I> Map<RequesterReference<I>, StackList<I>> readRequestMap(NBTTagList list, Supplier<StackList<I>> supplier) {
		Map<RequesterReference<I>, StackList<I>> request = new LinkedHashMap<>();

		for (int i = 0; i < list.tagCount(); i++) {
			NBTTagCompound tag = list.getCompoundTagAt(i);

			StackList<I> stackList = supplier.get();
			stackList.readNbt(tag.getTagList("stacks", Constants.NBT.TAG_COMPOUND));

			request.put(RequesterReference.readNBT(tag.getCompoundTag("reference")), stackList);
		}

		return request;
	}

	public static <I> void writeRequestMap(Map<RequesterReference<I>, StackList<I>> map, PacketBase packet) {
		packet.addInt(map.size());

		for (Map.Entry<RequesterReference<I>, StackList<I>> entry : map.entrySet()) {
			RequesterReference.writePacket(packet, entry.getKey());
			entry.getValue().writePacket(packet);
		}
	}

	public static <I> Map<RequesterReference<I>, StackList<I>> readRequestMap(PacketBase packet, Supplier<StackList<I>> supplier) {
		Map<RequesterReference<I>, StackList<I>> request = new LinkedHashMap<>();

		int size = packet.getInt();
		for (int i = 0; i < size; i++) {
			RequesterReference<I> reference = RequesterReference.readPacket(packet);

			StackList<I> list = supplier.get();
			list.readPacket(packet);

			if (!list.isEmpty()) {
				request.put(reference, list);
			}
		}

		return request;
	}

	// PARTIALLY COPIED FROM DuctUnitItem

	public static int canRouteItem(DuctUnitItem duct, ItemStack stack, byte side, IProcessRequester<ItemStack> requester) {
		if (duct.getGrid() == null) {
			return stack.getCount();
		}

		int stackSizeLeft = stack.getCount();
		ItemStack curItem;

		curItem = stack.copy();
		curItem.setCount(Math.min(duct.getMoveStackSize(side), curItem.getCount()));

		if (curItem.getCount() > 0) {
			stackSizeLeft = simTransferI(duct, side, requester, curItem.copy());
			stackSizeLeft = (stack.getCount() - curItem.getCount()) + stackSizeLeft;
		}

		return stackSizeLeft;
	}

	private static int simTransferI(DuctUnitItem duct, byte side, IProcessRequester<ItemStack> requester, ItemStack stack) {
		SIM = true;
		ItemStack itemStack = simTransfer(duct, side, requester, stack);
		SIM = false;
		return itemStack.isEmpty() ? 0 : itemStack.getCount();
	}

	private static ItemStack simTransfer(DuctUnitItem duct, byte side, IProcessRequester<ItemStack> requester, ItemStack stack) {
		EnumFacing face = EnumFacing.VALUES[side];

		if (stack.isEmpty()) {
			return ItemStack.EMPTY;
		}

		DuctUnitItem.Cache cache = duct.tileCache[side];

		if (duct.getGrid() == null || cache == null) {
			return stack;
		}

		boolean routeItems = cache.filter.shouldIncRouteItems();
		int maxStock = cache.filter.getMaxStock();

		IItemHandler itemHandler = cache.getItemHandler(side ^ 1);
		if (!routeItems) {
			return DuctUnitItem.simulateInsertItemStackIntoInventory(itemHandler, stack, side ^ 1, maxStock);
		}

		// Start own code

		StackMap travelingItems = new StackMap();
		StackMap map = duct.getGrid().travelingItems.get(duct.pos().offset(face));
		if (map != null)
			for (ItemStack s : map.getItems())
				travelingItems.addItemstack(s, side);

		/*
		for (StackList<ItemStack> list : requester.getRequests().values()) {
			for (Type<ItemStack> type : list.types()) {
				long amount = list.amount(type);
				while (amount > 0) {
					int remove = (int) Math.min(amount, type.maxSize());
					travelingItems.addItemstack(type.withAmount(remove), side);
					amount -= remove;
				}
			}
		}
		*/

		// End own code

		if (travelingItems == null || travelingItems.isEmpty()) {
			return DuctUnitItem.simulateInsertItemStackIntoInventory(itemHandler, stack, side ^ 1, maxStock);
		}
		if (travelingItems.size() == 1) {
			if (ItemHelper.itemsIdentical(stack, travelingItems.getItems().next())) {
				stack.grow(travelingItems.getItems().next().getCount());
				return DuctUnitItem.simulateInsertItemStackIntoInventory(itemHandler, stack, side ^ 1, maxStock);
			}
		} else {
			int s = 0;
			for (ItemStack travelingItem : travelingItems.getItems()) {
				if (!ItemHelper.itemsIdentical(stack, travelingItem)) {
					s = -1;
					break;
				} else {
					s += travelingItem.getCount();
				}
			}
			if (s >= 0) {
				stack.grow(s);
				return DuctUnitItem.simulateInsertItemStackIntoInventory(itemHandler, stack, side ^ 1, maxStock);
			}
		}
		SimulatedInv simulatedInv = SimulatedInv.wrapHandler(itemHandler);

		for (TObjectIntIterator<StackMap.ItemEntry> iterator = travelingItems.iterator(); iterator.hasNext(); ) {
			iterator.advance();

			StackMap.ItemEntry itemEntry = iterator.key();

			if (itemEntry.side != side && (cache.areEquivalentHandlers(itemHandler, itemEntry.side))) {
				continue;
			}
			if (!InventoryHelper.insertStackIntoInventory(simulatedInv, itemEntry.toItemStack(iterator.value()), false).isEmpty() && ItemHelper.itemsIdentical(stack, itemEntry.toItemStack(iterator.value()))) {
				return stack;
			}
		}
		return DuctUnitItem.simulateInsertItemStackIntoInventory(simulatedInv, stack, side ^ 1, maxStock);
	}

	// TODO OLD CODE, MAYBE REPLACE THEM AT SOME POINT

	public static void writePacket(PacketBase packet, Object item, Class<?> c, boolean identifier) {
		if (c == ItemStack.class) {
			if (identifier)
				packet.addByte(0);
			packet.addItemStack((ItemStack) item);
		} else if (c == FluidStack.class) {
			if (identifier)
				packet.addByte(1);
			packet.addFluidStack((FluidStack) item);
		} else throw new IllegalArgumentException("Unknown item type " + c.getName());
	}

	@SuppressWarnings("unchecked")
	@SideOnly(Side.CLIENT)
	public static ElementBase getSlot(GuiCrafter gui, int x, int y, GuiCrafter.Slot<?> slot) {
		Class<?> c = slot.getCrafter().getItemClass();
		if (c == ItemStack.class)
			return new ElementSlotItem(gui, x, y, (GuiCrafter.Slot<ItemStack>) slot, (GuiCrafter.Slot<ItemStack>) slot, true);
		else if (c == FluidStack.class)
			return new ElementSlotFluid(gui, x, y, (GuiCrafter.Slot<FluidStack>) slot, (GuiCrafter.Slot<FluidStack>) slot, true);
		else throw new IllegalArgumentException("Unknown item type " + c.getName());
	}

}
