package astavie.thermallogistics.block;

import astavie.thermallogistics.ThermalLogistics;
import astavie.thermallogistics.tile.TileTerminal;
import cofh.api.tileentity.IInventoryRetainer;
import cofh.core.block.BlockCoreTile;
import cofh.core.block.TileCore;
import cofh.core.block.TileNameable;
import cofh.core.render.IModelRegister;
import cofh.core.util.CoreUtils;
import cofh.core.util.RayTracer;
import cofh.core.util.helpers.*;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fml.common.eventhandler.Event;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;

public abstract class BlockTerminal extends BlockCoreTile implements IModelRegister {

	public static final PropertyEnum<Direction> DIRECTION = PropertyEnum.create("direction", Direction.class, Direction.values());

	public final boolean active;
	public boolean keepInventory = false;

	public BlockTerminal(String name, String type, boolean active) {
		super(Material.IRON, "logistics");

		setTranslationKey(name + "." + type);
		this.name = name + "_" + type;
		if (active) {
			this.name += "_active";
		}
		this.active = active;

		setRegistryName(this.name);

		if (!active) {
			setCreativeTab(ThermalLogistics.INSTANCE.tab);
		}

		setHardness(15.0F);
		setResistance(25.0F);

		this.setDefaultState(this.blockState.getBaseState().withProperty(DIRECTION, Direction.NORTH));
	}

	@Nonnull
	@Override
	public IBlockState getStateForPlacement(@Nonnull World world, @Nonnull BlockPos pos, @Nonnull EnumFacing facing, float hitX, float hitY, float hitZ, int meta, @Nonnull EntityLivingBase placer, EnumHand hand) {
		return getDefaultState().withProperty(DIRECTION, Direction.getDirection(pos, placer));
	}

	@Override
	public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase living, ItemStack stack) {
		TileTerminal<?> tile = (TileTerminal<?>) world.getTileEntity(pos);
		tile.setCustomName(ItemHelper.getNameFromItemStack(stack));
		if (stack.getTagCompound() != null)
			tile.requester.set(new ItemStack(stack.getTagCompound().getCompoundTag("requester")));
		super.onBlockPlacedBy(world, pos, state, living, stack);
	}

	@Override
	public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
		RayTraceResult traceResult = RayTracer.retrace(player);

		if (traceResult == null) {
			return false;
		}
		PlayerInteractEvent event = new PlayerInteractEvent.RightClickBlock(player, hand, pos, side, traceResult.hitVec);
		if (MinecraftForge.EVENT_BUS.post(event) || event.getResult() == Event.Result.DENY) {
			return false;
		}
		if (player.isSneaking()) {
			if (WrenchHelper.isHoldingUsableWrench(player, traceResult)) {
				if (ServerHelper.isServerWorld(world) && canDismantle(world, pos, state, player)) {
					dismantleBlock(world, pos, state, player, false);
					WrenchHelper.usedWrench(player, traceResult);
				}
				return true;
			}
		}
		TileNameable tile = (TileNameable) world.getTileEntity(pos);

		if (tile == null || tile.isInvalid())
			return false;
		if (WrenchHelper.isHoldingUsableWrench(player, traceResult)) {
			if (tile.canPlayerAccess(player)) {
				if (ServerHelper.isServerWorld(world))
					tile.onWrench(player, side);
				WrenchHelper.usedWrench(player, traceResult);
			}
			return true;
		}
		if (!tile.canPlayerAccess(player))
			return false;
		if (tile.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, null)) {
			ItemStack heldItem = player.getHeldItem(hand);
			if (FluidHelper.isFluidHandler(heldItem)) {
				FluidHelper.interactWithHandler(heldItem, tile.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, null), player, hand);
				return true;
			}
		}
		return !ServerHelper.isServerWorld(world) || tile.openGui(player);
	}

	@Nonnull
	@Override
	public IBlockState getStateFromMeta(int meta) {
		return this.getDefaultState().withProperty(DIRECTION, Direction.values()[meta]);
	}

	@Override
	public int getMetaFromState(IBlockState state) {
		return state.getValue(DIRECTION).ordinal();
	}

	@Nonnull
	@Override
	protected BlockStateContainer createBlockState() {
		return new BlockStateContainer(this, DIRECTION);
	}

	@Override
	public void breakBlock(World world, BlockPos pos, IBlockState state) {
		if (!keepInventory) {
			CoreUtils.dropItemStackIntoWorldWithVelocity(((TileTerminal<?>) world.getTileEntity(pos)).requester.get(), world, pos);
		}

		TileEntity tile = world.getTileEntity(pos);

		if (tile instanceof TileCore) {
			((TileCore) tile).blockBroken();
		}
		if (tile instanceof IInventoryRetainer && ((IInventoryRetainer) tile).retainInventory()) {
			// do nothing
		} else if (tile instanceof IInventory) {
			IInventory inv = (IInventory) tile;
			for (int i = 0; i < inv.getSizeInventory(); i++) {
				CoreUtils.dropItemStackIntoWorldWithVelocity(inv.getStackInSlot(i), world, pos);
			}
		}
	}

	protected abstract Item getItem();

	public abstract Block getActive(boolean active);

	@Override
	public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos, EntityPlayer player) {
		return new ItemStack(getItem());
	}

	@Override
	public ArrayList<ItemStack> dropDelegate(NBTTagCompound nbt, IBlockAccess world, BlockPos pos, int fortune) {
		return new ArrayList<>(Collections.singleton(new ItemStack(getItem())));
	}

	@Override
	public NBTTagCompound getItemStackTag(IBlockAccess world, BlockPos pos) {
		NBTTagCompound tag = new NBTTagCompound();
		tag.setTag("requester", ((TileTerminal<?>) world.getTileEntity(pos)).requester.get().writeToNBT(new NBTTagCompound()));
		return tag;
	}

	@Override
	public ArrayList<ItemStack> dismantleBlock(World world, BlockPos pos, IBlockState state, EntityPlayer player, boolean returnDrops) {
		NBTTagCompound nbt = getItemStackTag(world, pos);
		((TileTerminal<?>) world.getTileEntity(pos)).requester.set(ItemStack.EMPTY);
		return dismantleDelegate(nbt, world, pos, player, returnDrops, false);
	}

	@Override
	public ArrayList<ItemStack> dismantleDelegate(NBTTagCompound nbt, World world, BlockPos pos, EntityPlayer player, boolean returnDrops, boolean simulate) {
		ArrayList<ItemStack> ret = new ArrayList<>();
		if (world.getBlockState(pos).getBlock() != this)
			return ret;

		ItemStack dropBlock = new ItemStack(getItem());
		if (nbt != null && !nbt.isEmpty())
			dropBlock.setTagCompound(nbt);

		ret.add(dropBlock);

		if (!simulate) {
			world.setBlockToAir(pos);
			if (!returnDrops) {
				float f = 0.3F;
				double x2 = world.rand.nextFloat() * f + (1.0F - f) * 0.5D;
				double y2 = world.rand.nextFloat() * f + (1.0F - f) * 0.5D;
				double z2 = world.rand.nextFloat() * f + (1.0F - f) * 0.5D;

				EntityItem dropEntity = new EntityItem(world, pos.getX() + x2, pos.getY() + y2, pos.getZ() + z2, dropBlock);
				dropEntity.setPickupDelay(10);
				world.spawnEntity(dropEntity);

				if (player != null)
					CoreUtils.dismantleLog(player.getName(), this, 0, pos);
			}
		}
		return ret;
	}

	@Override
	public boolean preInit() {
		return true;
	}

	@Override
	public boolean initialize() {
		return true;
	}

	@Override
	public void registerModels() {
		ModelResourceLocation location = new ModelResourceLocation(ThermalLogistics.MOD_ID + ":" + name);
		ModelLoader.setCustomModelResourceLocation(getItem(), 0, location);
	}

	public enum Direction implements IStringSerializable {
		UP_NORTH, UP_SOUTH, UP_WEST, UP_EAST, NORTH, SOUTH, WEST, EAST, DOWN_NORTH, DOWN_SOUTH, DOWN_WEST, DOWN_EAST;

		private final String name;

		Direction() {
			this.name = toString().toLowerCase();
		}

		public static Direction getDirection(BlockPos pos, EntityLivingBase placer) {
			return getDirection(EnumFacing.getDirectionFromEntityLiving(pos, placer), placer.getHorizontalFacing().getOpposite());
		}

		public static Direction getDirection(EnumFacing vertical, EnumFacing horizontal) {
			if (vertical == EnumFacing.UP) {
				switch (horizontal) {
					case NORTH:
						return UP_NORTH;
					case SOUTH:
						return UP_SOUTH;
					case WEST:
						return UP_WEST;
					case EAST:
						return UP_EAST;
					default:
						break;
				}

			} else if (vertical == EnumFacing.DOWN) {
				switch (horizontal) {
					case NORTH:
						return DOWN_NORTH;
					case SOUTH:
						return DOWN_SOUTH;
					case WEST:
						return DOWN_WEST;
					case EAST:
						return DOWN_EAST;
					default:
						break;
				}
			} else {
				switch (horizontal) {
					case NORTH:
						return NORTH;
					case SOUTH:
						return SOUTH;
					case WEST:
						return WEST;
					case EAST:
						return EAST;
					default:
						break;
				}
			}
			return NORTH;
		}

		@Nonnull
		@Override
		public String getName() {
			return name;
		}

		public EnumFacing getFace() {
			if (ordinal() > 7)
				return EnumFacing.DOWN;
			return EnumFacing.byIndex(MathHelper.clamp(ordinal() - 2, 1, 5));
		}

	}

}
