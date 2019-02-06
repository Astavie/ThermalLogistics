package astavie.thermallogistics.attachment;

import astavie.thermallogistics.TLTextures;
import astavie.thermallogistics.ThermalLogistics;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.vec.Translation;
import codechicken.lib.vec.Vector3;
import codechicken.lib.vec.uv.IconTransformation;
import cofh.thermaldynamics.duct.attachments.retriever.RetrieverItem;
import cofh.thermaldynamics.duct.tiles.TileGrid;
import cofh.thermaldynamics.render.RenderDuct;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.IBlockAccess;

public class RequesterItem extends RetrieverItem {

	public static final ResourceLocation ID = new ResourceLocation(ThermalLogistics.MOD_ID, "requester_item");

	public RequesterItem(TileGrid tile, byte side) {
		super(tile, side);
	}

	public RequesterItem(TileGrid tile, byte side, int type) {
		super(tile, side, type);
		filter.handleFlagByte(24); // Whitelist by default
	}

	@Override
	public String getInfo() {
		return "tab." + ThermalLogistics.MOD_ID + ".requesterItem";
	}

	@Override
	public ResourceLocation getId() {
		return ID;
	}

	@Override
	public ItemStack getPickBlock() {
		return new ItemStack(ThermalLogistics.Items.requester, 1, type);
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
		RenderDuct.modelConnection[isPowered ? 1 : 2][side].render(ccRenderState, trans, new IconTransformation(TLTextures.REQUESTER[stuffed ? 1 : 0][type]));
		return true;
	}

	@Override
	public void writePortableData(EntityPlayer player, NBTTagCompound tag) {
		super.writePortableData(player, tag);
		tag.setString("DisplayType", new ItemStack(ThermalLogistics.Items.requester).getTranslationKey() + ".name");
	}

}
