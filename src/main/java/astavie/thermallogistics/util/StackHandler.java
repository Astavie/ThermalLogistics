package astavie.thermallogistics.util;

import astavie.thermallogistics.attachment.ICrafter;
import astavie.thermallogistics.util.collection.ItemList;
import astavie.thermallogistics.util.type.ItemType;
import cofh.core.gui.GuiContainerCore;
import cofh.core.network.PacketBase;
import cofh.core.util.helpers.ItemHelper;
import cofh.core.util.helpers.RenderHelper;
import cofh.core.util.helpers.StringHelper;
import cofh.thermaldynamics.duct.item.GridItem;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.IItemHandler;

import java.util.Collection;
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

	public static NBTTagCompound writeLargeItemStack(ItemStack stack) {
		NBTTagCompound compound = ItemHelper.cloneStack(stack, 1).writeToNBT(new NBTTagCompound());
		compound.setInteger("IntCount", stack.getCount());
		return compound;
	}

	public static ItemStack readLargeItemStack(NBTTagCompound compound) {
		ItemStack stack = new ItemStack(compound);
		if (compound.hasKey("IntCount"))
			return ItemHelper.cloneStack(stack, compound.getInteger("IntCount"));
		return stack;
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

	public static void addItems(ItemList list, GridItem grid, Collection<IItemHandler> blacklist) {
		for (IItemHandler inv : Snapshot.INSTANCE.getInventories(grid)) {
			if (blacklist.contains(inv))
				continue;
			blacklist.add(inv);

			for (int slot = 0; slot < inv.getSlots(); slot++) {
				ItemStack extract = inv.getStackInSlot(slot);
				if (extract.isEmpty())
					continue;
				list.add(extract);
			}
		}

		for (ICrafter<ItemStack> crafter : Snapshot.INSTANCE.getCrafters(grid)) {
			for (ItemStack output : crafter.getOutputs()) {
				list.addCraftable(new ItemType(output));
			}
		}
	}

}
