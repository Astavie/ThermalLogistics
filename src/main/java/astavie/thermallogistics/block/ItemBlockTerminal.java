package astavie.thermallogistics.block;

import astavie.thermallogistics.ThermalLogistics;
import astavie.thermallogistics.tile.TileTerminal;
import cofh.core.item.ItemCore;
import cofh.core.render.IModelRegister;
import cofh.core.util.core.IInitializer;
import cofh.core.util.helpers.RecipeHelper;
import cofh.thermaldynamics.duct.TDDucts;
import cofh.thermaldynamics.duct.tiles.DuctToken;
import cofh.thermaldynamics.duct.tiles.TileGrid;
import cofh.thermalexpansion.item.ItemFrame;
import cofh.thermalfoundation.item.ItemMaterial;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

public abstract class ItemBlockTerminal extends ItemCore implements IModelRegister, IInitializer {

	private final DuctToken<?, ?, ?> token;
	private final Block block;

	public ItemBlockTerminal(String type, DuctToken<?, ?, ?> token, Block block) {
		super("logistics");
		this.token = token;
		this.block = block;

		name = "terminal_" + type;
		setTranslationKey("logistics.terminal." + type);
		setCreativeTab(ThermalLogistics.tab);
	}

	@Override
	public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
		ItemStack itemstack = player.getHeldItem(hand);

		TileEntity tile = world.getTileEntity(pos);
		if (tile instanceof TileGrid) {
			TileGrid grid = (TileGrid) tile;
			if (grid.getDuct(token) == null)
				return EnumActionResult.FAIL;
		}

		pos = pos.offset(facing);

		if (!itemstack.isEmpty() && player.canPlayerEdit(pos, facing, itemstack) && world.mayPlace(block, pos, false, facing, null)) {
			IBlockState newState = block.getStateForPlacement(world, pos, facing, hitX, hitY, hitZ, itemstack.getItemDamage(), player, hand);

			if (world.setBlockState(pos, newState, 11)) {
				IBlockState state = world.getBlockState(pos);
				if (state.getBlock() == block) {
					((TileTerminal) world.getTileEntity(pos)).setDuct(facing.getOpposite());
					ItemBlock.setTileEntityNBT(world, player, pos, itemstack);
					block.onBlockPlacedBy(world, pos, state, player, itemstack);

					if (player instanceof EntityPlayerMP)
						CriteriaTriggers.PLACED_BLOCK.trigger((EntityPlayerMP) player, pos, itemstack);
				}

				newState = world.getBlockState(pos);
				SoundType soundtype = newState.getBlock().getSoundType(newState, world, pos, player);
				world.playSound(player, pos, soundtype.getPlaceSound(), SoundCategory.BLOCKS, (soundtype.getVolume() + 1.0F) / 2.0F, soundtype.getPitch() * 0.8F);
				itemstack.shrink(1);
			}

			return EnumActionResult.SUCCESS;
		}
		return EnumActionResult.FAIL;
	}

	@Override
	public boolean preInit() {
		ThermalLogistics.proxy.addModelRegister(this);
		ForgeRegistries.ITEMS.register(setRegistryName(name));
		return true;
	}

	@Override
	public void registerModels() {
		ModelLoader.setCustomModelResourceLocation(this, 0, new ModelResourceLocation(ThermalLogistics.MODID + ":" + name));
	}

	public static class Item extends ItemBlockTerminal {

		public Item() {
			super("item", DuctToken.ITEMS, ThermalLogistics.terminalItem);
		}

		@Override
		public boolean initialize() {
			RecipeHelper.addShapedRecipe(new ItemStack(this),
					" X ",
					"YCY",
					"IPI",
					'C', ItemFrame.frameMachine,
					'I', "gearCopper",
					'P', ItemMaterial.powerCoilGold,
					'X', ThermalLogistics.manager,
					'Y', TDDucts.itemBasic.itemStack
			);
			return true;
		}

	}

}
