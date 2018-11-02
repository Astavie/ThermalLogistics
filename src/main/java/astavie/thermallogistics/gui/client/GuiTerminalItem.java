package astavie.thermallogistics.gui.client;

import astavie.thermallogistics.gui.container.ContainerTerminal;
import astavie.thermallogistics.tile.TileTerminalItem;
import cofh.core.network.PacketBase;
import cofh.core.network.PacketHandler;
import cofh.core.network.PacketTileInfo;
import cofh.core.util.helpers.ItemHelper;
import cofh.core.util.helpers.RenderHelper;
import cofh.core.util.helpers.StringHelper;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import org.apache.commons.lang3.tuple.Triple;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GuiTerminalItem extends GuiTerminal {

	private final NonNullList<Triple<ItemStack, Long, Boolean>> filter = NonNullList.create();

	private ItemStack selected;
	private int ticks = 0;

	public GuiTerminalItem(ContainerTerminal container) {
		super(container);
		this.xSize = 194;
		this.ySize = 250;
	}

	@Override
	protected void request() {
		PacketBase payload = PacketTileInfo.newPacket(terminal.tile).addByte(1);
		try {
			payload.addShort(Item.getIdFromItem(selected.getItem()));
			payload.addInt(Integer.parseInt(amount.getText()));
			payload.addShort(ItemHelper.getItemDamage(selected));
			payload.writeNBT(selected.getTagCompound());
		} catch (IOException e) {
			e.printStackTrace();
		}
		PacketHandler.sendToServer(payload);
	}

	@Override
	public void initGui() {
		super.initGui();
		((TileTerminalItem) terminal.tile).terminal = NonNullList.create();
		PacketHandler.sendToServer(PacketTileInfo.newPacket(terminal.tile).addByte(0));
	}

	@Override
	public void updateScreen() {
		super.updateScreen();
		NonNullList<Triple<ItemStack, Long, Boolean>> stacks = ((TileTerminalItem) terminal.tile).terminal;

		boolean enabled = selected != null;
		amount.setVisible(amount.isVisible() && enabled);
		button.setEnabled(false);

		if (enabled && !amount.getText().isEmpty()) {
			for (Triple<ItemStack, Long, Boolean> stack : stacks) {
				if (ItemHelper.itemsIdentical(selected, stack.getLeft())) {
					long parse = Long.parseLong(amount.getText());
					button.setEnabled(parse <= Integer.MAX_VALUE && (stack.getRight() || stack.getMiddle() >= parse));
					break;
				}
			}
		}

		ticks++;
		if (ticks >= 10) { // Update terminal every half second
			PacketHandler.sendToServer(PacketTileInfo.newPacket(terminal.tile).addByte(0));
			ticks = 0;
		}

		filter.clear();
		for (Triple<ItemStack, Long, Boolean> stack : stacks) {
			if (search.getText().isEmpty() || stack.getLeft().getDisplayName().toLowerCase().contains(search.getText().toLowerCase())) {
				filter.add(stack);
			} else for (String string : getItemToolTip(stack.getLeft())) {
				if (string.toLowerCase().contains(search.getText().toLowerCase())) {
					filter.add(stack);
					break;
				}
			}
		}

		filter.sort((i1, i2) -> {
			// First compare stack size
			int count = -Long.compare(i1.getMiddle(), i2.getMiddle());
			if (count != 0)
				return count;

			// Then compare item id
			int id = Integer.compare(Item.getIdFromItem(i1.getLeft().getItem()), Item.getIdFromItem(i2.getLeft().getItem()));
			if (id != 0)
				return id;

			// Then compare item damage
			int damage = Integer.compare(i1.getLeft().getItemDamage(), i2.getLeft().getItemDamage());
			if (damage != 0)
				return damage;

			// Then compare sub items
			NonNullList<ItemStack> list = NonNullList.create();
			i1.getLeft().getItem().getSubItems(CreativeTabs.SEARCH, list);
			for (ItemStack stack : list) {
				if (ItemHelper.itemsIdentical(i1.getLeft(), stack))
					return -1;
				if (ItemHelper.itemsIdentical(i2.getLeft(), stack))
					return 1;
			}

			// Then compare nbt data
			if (i1.getLeft().getTagCompound() == null && i2.getLeft().getTagCompound() != null)
				return -1;
			if (i1.getLeft().getTagCompound() != null && i2.getLeft().getTagCompound() == null)
				return 1;

			// Then we give up
			return 0;
		});

		slider.setLimits(0, Math.max((filter.size() - 1) / 9 - 2, 0));
		slider.setEnabled(filter.size() > 27);
	}

	@Override
	protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
		super.drawGuiContainerForegroundLayer(mouseX, mouseY);
		mouseX -= guiLeft;
		mouseY -= guiTop;

		ItemStack select = null;

		RenderHelper.enableGUIStandardItemLighting();
		if (selected != null && button.isVisible()) {
			drawItemStack(selected, 26, 74, true, "");
			if (mouseX >= 25 && mouseX < 43 && mouseY >= 73 && mouseY < 91)
				select = selected;
		}

		int i = slider.getValue() * 9;

		a:
		for (int y = 0; y < 3; y++) {
			for (int x = 0; x < 9; x++) {
				int slot = i + x + y * 9;
				if (slot >= filter.size())
					break a;

				int posX = 8 + x * 18;
				int posY = 18 + y * 18;

				Triple<ItemStack, Long, Boolean> triple = filter.get(slot);

				if (ItemHelper.itemsIdentical(triple.getLeft(), selected)) {
					GlStateManager.disableLighting();
					GlStateManager.disableDepth();
					drawGradientRect(posX, posY, posX + 16, posY + 16, 0xFFC5C5C5, 0xFFC5C5C5);
					GlStateManager.enableLighting();
					GlStateManager.enableDepth();
				}

				GlStateManager.enableDepth();
				itemRender.renderItemAndEffectIntoGUI(triple.getLeft(), posX, posY);
				itemRender.renderItemOverlayIntoGUI(fontRenderer, triple.getLeft(), posX, posY, "");

				GlStateManager.disableLighting();
				GlStateManager.disableDepth();
				GlStateManager.disableBlend();

				GlStateManager.pushMatrix();
				GlStateManager.scale(0.5, 0.5, 0.5);

				String amount = triple.getMiddle() == 0L ? "Craft" : StringHelper.getScaledNumber(triple.getMiddle());
				getFontRenderer().drawStringWithShadow(amount, (posX + 16) * 2 - getFontRenderer().getStringWidth(amount), (posY + 12) * 2, 0xFFFFFF);
				GlStateManager.popMatrix();

				GlStateManager.enableLighting();
				GlStateManager.enableDepth();

				if (mouseX >= posX - 1 && mouseX < posX + 17 && mouseY >= posY - 1 && mouseY < posY + 17)
					select = triple.getLeft();
			}
		}

		if (select != null) {
			List<String> text = getItemToolTip(select);

			List<String> wrapped = new ArrayList<>();
			for (String textLine : text) {
				List<String> line = fontRenderer.listFormattedStringToWidth(textLine, width - guiLeft - mouseX - 16);
				wrapped.addAll(line);
			}

			drawTooltipHoveringText(wrapped, mouseX, mouseY, fontRenderer);
		}
	}

	@Override
	protected void mouseClicked(int mX, int mY, int mouseButton) throws IOException {
		int mouseX = mX - guiLeft - 7;
		int mouseY = mY - guiTop - 17;

		if (mouseX >= 0 && mouseX < 9 * 18 && mouseY >= 0 && mouseY < 3 * 18) {
			if (!mc.player.inventory.getItemStack().isEmpty()) {
				PacketHandler.sendToServer(PacketTileInfo.newPacket(terminal.tile).addByte(2));
				mc.player.inventory.setItemStack(ItemStack.EMPTY);
			} else {
				int posX = mouseX / 18;
				int posY = mouseY / 18;

				int slot = slider.getValue() * 9 + posX + posY * 9;
				if (slot < filter.size()) {
					selected = filter.get(slot).getLeft();
					if (button.isVisible()) {
						amount.setText(Long.toString(filter.get(slot).getRight() ? selected.getMaxStackSize() : Math.min(filter.get(slot).getMiddle(), selected.getMaxStackSize())));
						amount.setFocused(true);
					}
				} else selected = null;
			}
			return;
		}

		super.mouseClicked(mX, mY, mouseButton);
	}

	@Override
	protected void keyTyped(char characterTyped, int keyPressed) throws IOException {
		if (keyPressed == 28 && amount.isFocused())
			button.onMousePressed(0, 0, 0);
		else super.keyTyped(characterTyped, keyPressed);
	}

}
