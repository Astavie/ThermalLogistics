package astavie.thermallogistics.util;

import cofh.core.gui.GuiContainerCore;
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

	public static void writePacket(PacketBase packet, Object item) {
		if (item instanceof ItemStack) {
			packet.addByte(0);
			packet.addItemStack((ItemStack) item);
		} else if (item instanceof FluidStack) {
			packet.addByte(1);
			packet.addFluidStack((FluidStack) item);
		} else throw new IllegalArgumentException("Unknown item type " + item.getClass().getName());
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
	public static void render(GuiContainerCore gui, int x, int y, Object item, boolean overlay) {
		if (item instanceof ItemStack) {
			gui.drawItemStack((ItemStack) item, x, y, overlay, null);
		} else if (item instanceof FluidStack) {
			gui.drawFluid(x, y, (FluidStack) item, 16, 16);
			if (overlay) {
				GlStateManager.pushMatrix();

				GlStateManager.scale(0.5, 0.5, 0.5);
				String amount = StringHelper.getScaledNumber(((FluidStack) item).amount);
				gui.getFontRenderer().drawStringWithShadow(amount, (x + 16) * 2 - gui.getFontRenderer().getStringWidth(amount), (y + 12) * 2, 0xFFFFFF);

				GlStateManager.popMatrix();
			}
		} else throw new IllegalArgumentException("Unknown item type " + item.getClass().getName());
	}

	@SideOnly(Side.CLIENT)
	public static List<String> getTooltip(GuiContainerCore gui, Object item) {
		if (item instanceof ItemStack)
			return gui.getItemToolTip((ItemStack) item);
		else if (item instanceof FluidStack)
			return Collections.singletonList(((FluidStack) item).getLocalizedName());
		else throw new IllegalArgumentException("Unknown item type " + item.getClass().getName());
	}

}
