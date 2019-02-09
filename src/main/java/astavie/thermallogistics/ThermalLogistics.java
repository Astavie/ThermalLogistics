package astavie.thermallogistics;

import astavie.thermallogistics.attachment.CrafterItem;
import astavie.thermallogistics.attachment.ICrafter;
import astavie.thermallogistics.attachment.IRequester;
import astavie.thermallogistics.attachment.RequesterItem;
import astavie.thermallogistics.item.ItemCrafter;
import astavie.thermallogistics.item.ItemRequester;
import astavie.thermallogistics.util.RequesterReference;
import cofh.core.util.RayTracer;
import cofh.core.util.core.IInitializer;
import cofh.thermaldynamics.duct.Attachment;
import cofh.thermaldynamics.duct.AttachmentRegistry;
import cofh.thermaldynamics.duct.tiles.TileGrid;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;

import java.util.Objects;

@Mod(modid = ThermalLogistics.MOD_ID, name = ThermalLogistics.MOD_NAME, dependencies = "required-after:thermaldynamics;")
public class ThermalLogistics {

	public static final String MOD_ID = "thermallogistics";
	public static final String MOD_NAME = "Thermal Logistics";
	public static final String MOD_VERSION = Objects.requireNonNull(Loader.instance().activeModContainer()).getVersion();

	@Mod.Instance(MOD_ID)
	public static ThermalLogistics INSTANCE;

	public Configuration config;
	public int refreshDelay;

	@Mod.EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		AttachmentRegistry.registerAttachment(RequesterItem.ID, RequesterItem::new);
		AttachmentRegistry.registerAttachment(CrafterItem.ID, CrafterItem::new);

		config = new Configuration(event.getSuggestedConfigurationFile());
		refreshDelay = config.getInt("Refresh Delay", "General", 10, 1, 100, "The amount of ticks delay between sync packets from the server when looking at a GUI.");
	}

	@Mod.EventHandler
	public void init(FMLInitializationEvent event) {

	}

	@Mod.EventHandler
	public void postInit(FMLPostInitializationEvent event) {

	}

	@GameRegistry.ObjectHolder(MOD_ID)
	public static class Blocks {

	}

	@GameRegistry.ObjectHolder(MOD_ID)
	public static class Items {

		public static final ItemRequester requester = null;
		public static final ItemCrafter crafter = null;

	}

	@Mod.EventBusSubscriber
	public static class RegistryHandler {

		@SubscribeEvent
		public static void registerItems(RegistryEvent.Register<Item> event) {
			register(event.getRegistry(), new ItemRequester("requester"));
			register(event.getRegistry(), new ItemCrafter("crafter"));
			event.getRegistry().register(new Item() {
				{
					setTranslationKey("linkstick");
					setRegistryName("linkstick");
				}

				@Override
				public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
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

					ItemStack item = player.getHeldItem(hand);
					if (item.hasTagCompound()) {
						IRequester<?> other = RequesterReference.readNBT(item.getTagCompound()).getAttachment();
						if (other instanceof ICrafter && other != crafter)
							crafter.link((ICrafter<?>) other, true);
						else
							player.sendMessage(new TextComponentString("Nope"));
						item.setTagCompound(null);
					} else {
						item.setTagCompound(RequesterReference.writeNBT(crafter.getReference()));
					}

					return EnumActionResult.SUCCESS;
				}
			});
		}

		@SubscribeEvent
		public static void registerBlocks(RegistryEvent.Register<Block> event) {

		}

		@SubscribeEvent
		public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {
			Items.requester.initialize();
			Items.crafter.initialize();
		}

		@SubscribeEvent
		public static void registerModels(ModelRegistryEvent event) {
			Items.requester.registerModels();
			Items.crafter.registerModels();
		}

		private static <V extends IForgeRegistryEntry<V>> void register(IForgeRegistry<V> registry, IInitializer i) {
			i.preInit();

			//noinspection unchecked
			registry.register((V) i);
		}

	}

}
