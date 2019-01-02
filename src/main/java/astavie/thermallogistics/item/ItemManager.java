package astavie.thermallogistics.item;

import astavie.thermallogistics.ThermalLogistics;
import astavie.thermallogistics.attachment.Crafter;
import astavie.thermallogistics.util.reference.CrafterReference;
import cofh.core.item.ItemMultiRF;
import cofh.core.key.KeyBindingItemMultiMode;
import cofh.core.util.RayTracer;
import cofh.core.util.core.IInitializer;
import cofh.core.util.helpers.ChatHelper;
import cofh.core.util.helpers.StringHelper;
import cofh.thermaldynamics.duct.Attachment;
import cofh.thermaldynamics.duct.tiles.TileGrid;
import cofh.thermalfoundation.item.ItemMaterial;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.List;

import static cofh.core.util.helpers.RecipeHelper.addShapedRecipe;

public class ItemManager extends ItemMultiRF implements IInitializer {

	public static final int[] CAPACITY = {1, 3, 6, 10, 15};
	public static final int[] XFER = {1, 4, 9, 16, 25};
	public static final int[] NETWORKS = {0, 1, 3, 9, 27};

	public static final int ENERGY_PER_USE = 200;

	public static ItemStack managerBasic, managerHardened, managerReinforced, managerSignalum, managerResonant, managerCreative;

	public ItemManager() {
		super("logistics");
		setTranslationKey("manager");
		setCreativeTab(ThermalLogistics.tab);
		setMaxStackSize(1);
	}

	@Override
	protected int getCapacity(ItemStack stack) {
		return ItemMultiRF.CAPACITY_MIN * CAPACITY[stack.getItemDamage() % 5];
	}

	@Override
	protected int getReceive(ItemStack stack) {
		return ItemMultiRF.XFER_MIN * XFER[stack.getItemDamage() % 5];
	}

	@Override
	public void onModeChange(EntityPlayer player, ItemStack stack) {
		stack.removeSubCompound("Link");
		player.world.playSound(null, player.getPosition(), SoundEvents.BLOCK_LEVER_CLICK, SoundCategory.PLAYERS, 0.6F, 0.4F + 0.2F * getMode(stack));
		ChatHelper.sendIndexedChatMessageToPlayer(player, new TextComponentTranslation("info.logistics.manager.c." + getMode(stack)));
	}

	@Override
	public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
		if (StringHelper.displayShiftForDetail && !StringHelper.isShiftKeyDown())
			tooltip.add(StringHelper.shiftForDetails());
		if (!StringHelper.isShiftKeyDown())
			return;

		tooltip.add(StringHelper.getInfoText("info.logistics.manager.a.2"));
		tooltip.add(StringHelper.getNoticeText("info.logistics.manager.a." + getMode(stack)));
		tooltip.add(StringHelper.localizeFormat("info.logistics.manager.b." + getMode(stack), StringHelper.getKeyName(KeyBindingItemMultiMode.INSTANCE.getKey())));

		if (isCreative(stack)) {
			tooltip.add(StringHelper.localize("info.logistics.networks") + ": 0");
			tooltip.add(StringHelper.localize("info.cofh.charge") + ": 1.21G RF");
		} else {
			if (stack.getItemDamage() % 5 > 0)
				tooltip.add(StringHelper.localize("info.logistics.networks") + ": 0 / " + NETWORKS[stack.getItemDamage() % 5]);
			tooltip.add(StringHelper.localize("info.cofh.charge") + ": " + StringHelper.getScaledNumber(getEnergyStored(stack)) + " / " + StringHelper.getScaledNumber(getMaxEnergyStored(stack)) + " RF");
		}
	}

	@Override
	public EnumActionResult onItemUse(EntityPlayer player, World worldIn, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
		return EnumActionResult.PASS;
	}

	@Override
	public EnumActionResult onItemUseFirst(EntityPlayer player, World world, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ, EnumHand hand) {
		ItemStack stack = player.getHeldItem(hand);
		Attachment attachment = getAttachment(player, world, pos);
		switch (getMode(stack)) {
			case 0:
				if (attachment != null) {
					// TODO: Monitor network
				}
				break;
			case 1:
				if (attachment != null && attachment instanceof Crafter) {
					if (!world.isRemote) {
						if (getEnergyStored(stack) >= ENERGY_PER_USE || player.capabilities.isCreativeMode || stack.getItemDamage() == CREATIVE) {
							Crafter<?, ?, ?> crafter = (Crafter<?, ?, ?>) attachment;
							ActionResult<Crafter<?, ?, ?>> result = getCrafter(world, stack);
							if (result.getType() == EnumActionResult.PASS || (result.getType() == EnumActionResult.SUCCESS && result.getResult() == crafter)) {
								player.sendMessage(new TextComponentTranslation("info.logistics.manager.d.0"));
								saveCrafter(crafter, stack);
							} else {
								stack.removeSubCompound("Link");
								if (result.getType() == EnumActionResult.FAIL) {
									player.world.playSound(null, player.getPosition(), SoundEvents.BLOCK_STONE_STEP, SoundCategory.PLAYERS, 0.8F, 0.5F);
									player.sendMessage(new TextComponentTranslation("info.logistics.manager.d.1"));
								} else {
									if (result.getResult().linked.contains(new CrafterReference<>(crafter))) {
										player.world.playSound(null, player.getPosition(), SoundEvents.BLOCK_STONE_STEP, SoundCategory.PLAYERS, 0.8F, 0.5F);
										player.sendMessage(new TextComponentTranslation("info.logistics.manager.d.2"));
									} else {
										if (!player.capabilities.isCreativeMode && stack.getItemDamage() != CREATIVE)
											extractEnergy(stack, ENERGY_PER_USE, false);

										player.world.playSound(null, player.getPosition(), SoundEvents.ENTITY_ARROW_HIT_PLAYER, SoundCategory.PLAYERS, 0.4F, 0.45F);
										player.sendMessage(new TextComponentTranslation("info.logistics.manager.d.3"));

										crafter.linked.addAll(result.getResult().linked);
										result.getResult().linked.forEach(c -> c.getCrafter().linked = crafter.linked);

										crafter.linked.forEach(c -> c.getCrafter().baseTile.markChunkDirty());
									}
								}
							}
						} else {
							player.world.playSound(null, player.getPosition(), SoundEvents.BLOCK_STONE_STEP, SoundCategory.PLAYERS, 0.8F, 0.5F);
						}
					}
					return EnumActionResult.SUCCESS;
				}
				break;
		}
		return EnumActionResult.PASS;
	}

	private Attachment getAttachment(EntityPlayer player, World world, BlockPos pos) {
		TileEntity tile = world.getTileEntity(pos);
		if (tile != null && tile instanceof TileGrid) {
			TileGrid grid = (TileGrid) tile;
			RayTraceResult target = RayTracer.retraceBlock(world, player, pos);
			if (target != null && target.subHit >= 14 && target.subHit < 20) {
				Attachment attachment = grid.getAttachment(target.subHit - 14);
				if (attachment != null)
					return attachment;
			}
		}
		return null;
	}

	private ActionResult<Crafter<?, ?, ?>> getCrafter(World world, ItemStack stack) {
		NBTTagCompound tag = stack.getSubCompound("Link");
		if (tag != null && world.provider.getDimension() == tag.getInteger("dim")) {
			Crafter<?, ?, ?> crafter = Crafter.readCrafter(world, tag);
			if (crafter == null)
				return ActionResult.newResult(EnumActionResult.FAIL, null);
			else
				return ActionResult.newResult(EnumActionResult.SUCCESS, crafter);
		}
		return ActionResult.newResult(EnumActionResult.PASS, null);
	}

	private void saveCrafter(Crafter crafter, ItemStack stack) {
		stack.removeSubCompound("Link");
		NBTTagCompound tag = stack.getOrCreateSubCompound("Link");
		tag.setInteger("dim", crafter.baseTile.world().provider.getDimension());
		Crafter.writeCrafter(crafter, tag);
	}

	@Override
	public boolean preInit() {
		ForgeRegistries.ITEMS.register(setRegistryName("manager"));
		ThermalLogistics.proxy.addModelRegister(this);

		managerBasic = addItem(0, "basic", EnumRarity.COMMON);
		managerHardened = addItem(1, "hardened", EnumRarity.COMMON);
		managerReinforced = addItem(2, "reinforced", EnumRarity.UNCOMMON);
		managerSignalum = addItem(3, "signalum", EnumRarity.UNCOMMON);
		managerResonant = addItem(4, "resonant", EnumRarity.RARE);
		managerCreative = addItem(CREATIVE, "creative", EnumRarity.EPIC);
		return true;
	}

	@Override
	public boolean initialize() {
		addShapedRecipe(managerBasic, "iRi", "iCi", "iSi", 'i', "nuggetLead", 'R', Blocks.REDSTONE_TORCH, 'C', ItemMaterial.partToolCasing, 'S', "dustSulfur");
		addShapedRecipe(managerHardened, " i ", "IMI", "i i", 'i', "nuggetLead", 'I', "ingotInvar", 'M', managerBasic);
		addShapedRecipe(managerReinforced, " i ", "IMI", "i i", 'i', "nuggetInvar", 'I', "ingotElectrum", 'M', managerHardened);
		addShapedRecipe(managerSignalum, " i ", "IMI", "i i", 'i', "nuggetElectrum", 'I', "ingotSignalum", 'M', managerReinforced);
		addShapedRecipe(managerResonant, " i ", "IMI", "i i", 'i', "nuggetSignalum", 'I', "ingotEnderium", 'M', managerSignalum);
		return true;
	}

}
