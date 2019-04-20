package astavie.thermallogistics.util;

import astavie.thermallogistics.attachment.ICrafter;
import astavie.thermallogistics.attachment.IRequester;
import astavie.thermallogistics.client.gui.GuiCrafter;
import astavie.thermallogistics.client.gui.element.ElementSlotFluid;
import astavie.thermallogistics.client.gui.element.ElementSlotItem;
import cofh.core.gui.GuiContainerCore;
import cofh.core.gui.element.ElementBase;
import cofh.core.network.PacketBase;
import cofh.core.util.helpers.ItemHelper;
import cofh.core.util.helpers.RenderHelper;
import cofh.core.util.helpers.StringHelper;
import cofh.thermaldynamics.duct.Attachment;
import cofh.thermaldynamics.duct.item.DuctUnitItem;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.IItemHandler;
import org.apache.commons.lang3.tuple.Triple;

import javax.annotation.Nullable;
import java.util.*;

public class StackHandler {

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
	public static <I> I readPacket(PacketBase packet) {
		byte type = packet.getByte();
		if (type == 0)
			return (I) packet.getItemStack();
		else if (type == 1)
			return (I) packet.getFluidStack();
		else throw new IllegalArgumentException("Unknown item type " + type);
	}

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

			GlStateManager.disableLighting();
			gui.drawFluid(x, y, fluid, 16, 16);

			if (count) {
				GlStateManager.disableLighting();
				GlStateManager.disableDepth();
				GlStateManager.disableBlend();

				GlStateManager.pushMatrix();

				GlStateManager.scale(0.5, 0.5, 0.5);
				String amount = StringHelper.formatNumber(fluid.amount);
				gui.getFontRenderer().drawStringWithShadow(amount, (x + 16) * 2 - gui.getFontRenderer().getStringWidth(amount), (y + 12) * 2, 0xFFFFFF);

				GlStateManager.popMatrix();

				GlStateManager.enableLighting();
				GlStateManager.enableDepth();
			}
		} else throw new IllegalArgumentException("Unknown item type " + item.getClass().getName());
	}

	@SideOnly(Side.CLIENT)
	public static void render(GuiContainerCore gui, int x, int y, Object item, String text) {
		if (item instanceof ItemStack)
			gui.drawItemStack((ItemStack) item, x, y, true, "");
		else if (item instanceof FluidStack)
			gui.drawFluid(x, y, (FluidStack) item, 16, 16);
		else throw new IllegalArgumentException("Unknown item type " + item.getClass().getName());

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

	@SideOnly(Side.CLIENT)
	public static List<String> getTooltip(GuiContainerCore gui, Object item) {
		if (item instanceof ItemStack)
			return gui.getItemToolTip((ItemStack) item);
		else if (item instanceof FluidStack)
			return Collections.singletonList(((FluidStack) item).getLocalizedName());
		else throw new IllegalArgumentException("Unknown item type " + item.getClass().getName());
	}

	@SuppressWarnings("unchecked")
	@SideOnly(Side.CLIENT)
	public static ElementBase getSlot(GuiContainerCore gui, int x, int y, GuiCrafter.Slot<?> slot) {
		Class<?> c = slot.getCrafter().getItemClass();
		if (c == ItemStack.class)
			return new ElementSlotItem(gui, x, y, (GuiCrafter.Slot<ItemStack>) slot, (GuiCrafter.Slot<ItemStack>) slot, true);
		else if (c == FluidStack.class)
			return new ElementSlotFluid(gui, x, y, (GuiCrafter.Slot<FluidStack>) slot, (GuiCrafter.Slot<FluidStack>) slot, true);
		else throw new IllegalArgumentException("Unknown item type " + c.getName());
	}

	public static List<Triple<ItemStack, Long, Boolean>> getItems(IRequester<ItemStack> requester, @Nullable Set<IItemHandler> handlers) {
		List<Triple<ItemStack, Long, Boolean>> items = new LinkedList<>();

		DuctUnitItem duct = (DuctUnitItem) requester.getDuct();
		if (duct == null)
			return items;

		if (handlers == null)
			handlers = new HashSet<>();

		IItemHandler handler = requester.getCachedInv();
		if (handler != null)
			handlers.add(handler);

		for (DuctUnitItem start : duct.getGrid().nodeSet) {
			for (byte side = 0; side < 6; side++) {
				if ((!start.isInput(side) && !start.isOutput(side)) || !start.parent.getConnectionType(side).allowTransfer)
					continue;

				DuctUnitItem.Cache cache = start.tileCache[side];
				if (cache == null)
					continue;

				IItemHandler inv = cache.getItemHandler(side ^ 1);
				if (inv == null || handlers.contains(inv))
					continue;

				Attachment attachment = start.parent.getAttachment(side);
				if (attachment != null) {
					if (attachment instanceof ICrafter && ((ICrafter) attachment).isEnabled()) {
						a:
						//noinspection unchecked
						for (ItemStack out : ((ICrafter<ItemStack>) attachment).getOutputs()) {
							if (out.isEmpty())
								continue;
							for (int i = 0; i < items.size(); i++) {
								Triple<ItemStack, Long, Boolean> stack = items.get(i);
								if (!ItemHelper.itemsIdentical(out, stack.getLeft()))
									continue;
								if (!stack.getRight())
									items.set(i, Triple.of(stack.getLeft(), stack.getMiddle(), true));
								continue a;
							}
							items.add(Triple.of(ItemHelper.cloneStack(out, 1), 0L, true));
						}
					}
					if (!attachment.canSend())
						continue;
				}

				if (cache.tile != null) {
					if (cache.tile instanceof ICrafter && ((ICrafter) cache.tile).isEnabled()) {
						a:
						//noinspection unchecked
						for (ItemStack out : ((ICrafter<ItemStack>) cache.tile).getOutputs()) {
							if (out.isEmpty())
								continue;
							for (int i = 0; i < items.size(); i++) {
								Triple<ItemStack, Long, Boolean> stack = items.get(i);
								if (!ItemHelper.itemsIdentical(out, stack.getLeft()))
									continue;
								if (!stack.getRight())
									items.set(i, Triple.of(stack.getLeft(), stack.getMiddle(), true));
								continue a;
							}
							items.add(Triple.of(ItemHelper.cloneStack(out, 1), 0L, true));
						}
					}
				}

				a:
				for (int slot = 0; slot < inv.getSlots(); slot++) {
					ItemStack extract = inv.getStackInSlot(slot);
					if (extract.isEmpty())
						continue;

					for (int i = 0; i < items.size(); i++) {
						Triple<ItemStack, Long, Boolean> stack = items.get(i);
						if (!ItemHelper.itemsIdentical(extract, stack.getLeft()))
							continue;
						items.set(i, Triple.of(stack.getLeft(), stack.getMiddle() + extract.getCount(), stack.getRight()));
						continue a;
					}
					items.add(Triple.of(ItemHelper.cloneStack(extract, 1), (long) extract.getCount(), false));
				}

				handlers.add(inv);
			}
		}

		return items;
	}

}
