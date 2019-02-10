package astavie.thermallogistics.util;

import astavie.thermallogistics.client.gui.GuiCrafter;
import astavie.thermallogistics.client.gui.element.ElementSlotFluid;
import astavie.thermallogistics.client.gui.element.ElementSlotItem;
import cofh.core.gui.GuiContainerCore;
import cofh.core.gui.element.ElementBase;
import cofh.core.network.PacketBase;
import cofh.core.util.helpers.StringHelper;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Collections;
import java.util.List;

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
			gui.drawItemStack((ItemStack) item, x, y, true, !count ? "" : null);
		} else if (item instanceof FluidStack) {
			gui.drawFluid(x, y, (FluidStack) item, 16, 16);

			if (count) {
				GlStateManager.disableLighting();
				GlStateManager.disableDepth();
				GlStateManager.disableBlend();

				GlStateManager.pushMatrix();

				GlStateManager.scale(0.5, 0.5, 0.5);
				String amount = StringHelper.formatNumber(((FluidStack) item).amount);
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
			return new ElementSlotItem(gui, x, y, (GuiCrafter.Slot<ItemStack>) slot, (GuiCrafter.Slot<ItemStack>) slot);
		else if (c == FluidStack.class)
			return new ElementSlotFluid(gui, x, y, (GuiCrafter.Slot<FluidStack>) slot, (GuiCrafter.Slot<FluidStack>) slot, true);
		else throw new IllegalArgumentException("Unknown item type " + c.getName());
	}

}
