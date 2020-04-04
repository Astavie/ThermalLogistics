package astavie.thermallogistics.util;

import astavie.thermallogistics.attachment.ICrafter;
import astavie.thermallogistics.attachment.ICrafterContainer;
import astavie.thermallogistics.attachment.IRequester;
import astavie.thermallogistics.attachment.IRequesterContainer;
import astavie.thermallogistics.client.gui.GuiCrafter;
import astavie.thermallogistics.client.gui.element.ElementSlotFluid;
import astavie.thermallogistics.client.gui.element.ElementSlotItem;
import astavie.thermallogistics.util.collection.StackList;
import astavie.thermallogistics.util.type.Type;
import cofh.core.gui.GuiContainerCore;
import cofh.core.gui.element.ElementBase;
import cofh.core.network.PacketBase;
import cofh.core.util.helpers.RenderHelper;
import cofh.core.util.helpers.StringHelper;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class StackHandler {

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
			NBTTagCompound tag = new NBTTagCompound();
			tag.setTag("reference", RequesterReference.writeNBT(entry.getKey()));
			tag.setTag("stacks", entry.getValue().writeNbt());
		}

		return list;
	}

	public static <I> Map<RequesterReference<I>, StackList<I>> readRequestMap(NBTTagList list, Supplier<StackList<I>> supplier) {
		Map<RequesterReference<I>, StackList<I>> request = new HashMap<>();

		for (int i = 0; i < list.tagCount(); i++) {
			NBTTagCompound tag = list.getCompoundTagAt(i);

			StackList<I> stackList = supplier.get();
			stackList.readNbt(tag.getTagList("stacks", Constants.NBT.TAG_COMPOUND));

			request.put(RequesterReference.readNBT(tag.getCompoundTag("reference")), stackList);
		}

		return request;
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
