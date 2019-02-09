package astavie.thermallogistics.item;

import astavie.thermallogistics.ThermalLogistics;
import astavie.thermallogistics.attachment.ICrafter;
import astavie.thermallogistics.attachment.IRequester;
import astavie.thermallogistics.util.RequesterReference;
import cofh.api.item.IMultiModeItem;
import cofh.core.item.ItemCore;
import cofh.core.key.KeyBindingItemMultiMode;
import cofh.core.render.IModelRegister;
import cofh.core.util.RayTracer;
import cofh.core.util.core.IInitializer;
import cofh.core.util.helpers.ChatHelper;
import cofh.core.util.helpers.RecipeHelper;
import cofh.core.util.helpers.StringHelper;
import cofh.thermaldynamics.duct.Attachment;
import cofh.thermaldynamics.duct.item.DuctUnitItem;
import cofh.thermaldynamics.duct.item.StackMap;
import cofh.thermaldynamics.duct.tiles.DuctToken;
import cofh.thermaldynamics.duct.tiles.TileGrid;
import cofh.thermalfoundation.item.ItemMaterial;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.client.model.ModelLoader;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class ItemManager extends ItemCore implements IMultiModeItem, IInitializer, IModelRegister {

	public ItemManager(String name) {
		this.name = name;
		setTranslationKey("logistics." + name);
		setRegistryName(name);
		setMaxStackSize(1);
	}

	@Override
	public void onModeChange(EntityPlayer player, ItemStack stack) {
		stack.removeSubCompound("Link");
		player.world.playSound(null, player.getPosition(), SoundEvents.BLOCK_LEVER_CLICK, SoundCategory.PLAYERS, 0.6F, 0.4F + 0.2F * getMode(stack));
		ChatHelper.sendIndexedChatMessageToPlayer(player, new TextComponentTranslation("info.logistics.manager.c." + getMode(stack)));
	}

	@Override
	public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flagIn) {
		if (StringHelper.displayShiftForDetail && !StringHelper.isShiftKeyDown())
			tooltip.add(StringHelper.shiftForDetails());
		if (!StringHelper.isShiftKeyDown())
			return;

		tooltip.add(StringHelper.getInfoText("info.logistics.manager.a.2"));
		tooltip.add(StringHelper.getNoticeText("info.logistics.manager.a." + getMode(stack)));
		tooltip.add(StringHelper.localizeFormat("info.logistics.manager.b." + getMode(stack), StringHelper.getKeyName(KeyBindingItemMultiMode.INSTANCE.getKey())));
	}

	@Nonnull
	@Override
	public EnumActionResult onItemUseFirst(EntityPlayer player, World world, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ, EnumHand hand) {
		ItemStack item = player.getHeldItem(hand);

		if (getMode(item) == 0) {
			TileEntity tile = world.getTileEntity(pos);
			if (tile instanceof TileGrid) {
				TileGrid grid = (TileGrid) tile;
				byte face = -1;

				RayTraceResult target = RayTracer.retraceBlock(world, player, pos);
				if (target != null) {
					int s = -1;
					int subHit = target.subHit;

					if (subHit < 6)
						s = subHit;
					else if (subHit >= 14 && subHit < 20)
						s = subHit - 14;

					if (s != -1)
						face = (byte) s;
				}

				if (face == -1)
					return EnumActionResult.PASS;

				DuctUnitItem duct = grid.getDuct(DuctToken.ITEMS);
				if (duct == null)
					return EnumActionResult.PASS;

				if (world.isRemote)
					return EnumActionResult.SUCCESS;

				StackMap map = duct.getGrid().travelingItems.get(duct.pos().offset(EnumFacing.VALUES[face]));
				if (map == null || map.isEmpty()) {
					ChatHelper.sendIndexedChatMessageToPlayer(player, new TextComponentTranslation("info.logistics.manager.e.2"));
				} else {
					List<ITextComponent> list = new ArrayList<>();
					list.add(new TextComponentTranslation("info.logistics.manager.e.0"));
					for (ItemStack stack : map.getItems())
						list.add(new TextComponentTranslation("info.logistics.manager.e.1", stack.getCount(), stack.getTextComponent()));
					ChatHelper.sendIndexedChatMessagesToPlayer(player, list);
				}

				return EnumActionResult.SUCCESS;
			}
		} else {
			ICrafter<?> crafter = null;

			TileEntity tile = world.getTileEntity(pos);
			if (tile instanceof TileGrid) {
				RayTraceResult raytrace = RayTracer.retraceBlock(world, player, pos);
				if (raytrace != null && raytrace.subHit >= 14 && raytrace.subHit < 20) {
					Attachment attachment = ((TileGrid) tile).getAttachment(raytrace.subHit - 14);
					if (attachment instanceof ICrafter)
						crafter = (ICrafter<?>) attachment;
				}
			} else if (tile instanceof ICrafter)
				crafter = (ICrafter<?>) tile;

			if (crafter == null)
				return EnumActionResult.PASS;

			if (world.isRemote)
				return EnumActionResult.SUCCESS;

			if (!item.hasTagCompound())
				item.setTagCompound(new NBTTagCompound());

			if (item.getSubCompound("Link") != null) {
				IRequester<?> other = RequesterReference.readNBT(item.getSubCompound("Link")).getAttachment();
				if (other instanceof ICrafter && other != crafter) {
					if (crafter.hasLinked((ICrafter<?>) other)) {
						ChatHelper.sendIndexedChatMessageToPlayer(player, new TextComponentTranslation("info.logistics.manager.d.2"));
					} else {
						crafter.link((ICrafter<?>) other, true);
						ChatHelper.sendIndexedChatMessageToPlayer(player, new TextComponentTranslation("info.logistics.manager.d.3"));
					}
				} else {
					if (other == crafter)
						ChatHelper.sendIndexedChatMessageToPlayer(player, new TextComponentTranslation("info.logistics.manager.d.4"));
					else
						ChatHelper.sendIndexedChatMessageToPlayer(player, new TextComponentTranslation("info.logistics.manager.d.1"));
				}
				item.getTagCompound().removeTag("Link");
			} else {
				item.getTagCompound().setTag("Link", RequesterReference.writeNBT(crafter.getReference()));
				ChatHelper.sendIndexedChatMessageToPlayer(player, new TextComponentTranslation("info.logistics.manager.d.0"));
			}

			return EnumActionResult.SUCCESS;
		}
		return EnumActionResult.PASS;
	}

	@Override
	public boolean preInit() {
		return true;
	}

	@Override
	public boolean initialize() {
		RecipeHelper.addShapedRecipe(new ItemStack(this), "iRi", "iCi", "iSi", 'i', "nuggetIron", 'R', Blocks.REDSTONE_TORCH, 'C', ItemMaterial.partToolCasing, 'S', "dustSulfur");
		return true;
	}

	@Override
	public void registerModels() {
		ModelResourceLocation location = new ModelResourceLocation(ThermalLogistics.MOD_ID + ":" + name);
		ModelLoader.setCustomModelResourceLocation(this, 0, location);
	}

}