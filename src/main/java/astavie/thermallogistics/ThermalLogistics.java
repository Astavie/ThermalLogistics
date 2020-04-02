package astavie.thermallogistics;

import astavie.thermallogistics.attachment.CrafterItem;
import astavie.thermallogistics.attachment.DistributorFluid;
import astavie.thermallogistics.attachment.DistributorItem;
import astavie.thermallogistics.attachment.RequesterItem;
import astavie.thermallogistics.block.BlockTerminalItem;
import astavie.thermallogistics.item.ItemCrafter;
import astavie.thermallogistics.item.ItemDistributor;
import astavie.thermallogistics.item.ItemManager;
import astavie.thermallogistics.item.ItemRequester;
import astavie.thermallogistics.tile.TileTerminalItem;
import cofh.core.gui.CreativeTabCore;
import cofh.core.util.core.IInitializer;
import cofh.thermaldynamics.duct.AttachmentRegistry;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.tileentity.TileEntity;
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

	public CreativeTabs tab = new CreativeTabCore(MOD_ID) {
		@Override
		public ItemStack createIcon() {
			return new ItemStack(Blocks.terminal_item);
		}
	};

	public Configuration config;

	public int refreshDelay;
	public int syncDelay;

	@Mod.EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		AttachmentRegistry.registerAttachment(DistributorItem.ID, DistributorItem::new);
		AttachmentRegistry.registerAttachment(DistributorFluid.ID, DistributorFluid::new);

		AttachmentRegistry.registerAttachment(RequesterItem.ID, RequesterItem::new);

		AttachmentRegistry.registerAttachment(CrafterItem.ID, CrafterItem::new);

		config = new Configuration(event.getSuggestedConfigurationFile());

		refreshDelay = config.getInt("Refresh Delay", Configuration.CATEGORY_GENERAL, 20, 1, 100, "The amount of ticks a terminal item list is cached.");
		syncDelay = config.getInt("Sync Delay", Configuration.CATEGORY_GENERAL, 20, 1, 100, "The amount of ticks delay between terminal update packets from the server.");

		config.save();
	}

	@Mod.EventHandler
	public void init(FMLInitializationEvent event) {
		TileEntity.register(Blocks.terminal_item.getRegistryName().toString(), TileTerminalItem.class);
	}

	@Mod.EventHandler
	public void postInit(FMLPostInitializationEvent event) {
	}

	@GameRegistry.ObjectHolder(MOD_ID)
	public static class Blocks {

		public static final BlockTerminalItem terminal_item = null;

	}

	@GameRegistry.ObjectHolder(MOD_ID)
	public static class Items {

		public static final ItemRequester requester = null;
		public static final ItemCrafter crafter = null;
		public static final ItemDistributor distributor = null;

		public static final ItemManager manager = null;

	}

	@Mod.EventBusSubscriber
	public static class RegistryHandler {

		@SubscribeEvent
		public static void registerBlocks(RegistryEvent.Register<Block> event) {
			register(event.getRegistry(), new BlockTerminalItem("terminal", "item"));
		}

		@SubscribeEvent
		public static void registerItems(RegistryEvent.Register<Item> event) {
			// Items
			register(event.getRegistry(), new ItemRequester("requester"));
			register(event.getRegistry(), new ItemCrafter("crafter"));
			register(event.getRegistry(), new ItemDistributor("distributor"));

			register(event.getRegistry(), new ItemManager("manager"));

			// Blocks
			registerBlock(event.getRegistry(), Blocks.terminal_item);
		}

		@SubscribeEvent
		public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {
			// Items
			Items.requester.initialize();
			Items.crafter.initialize();
			Items.distributor.initialize();

			Items.manager.initialize();

			// Blocks
			Blocks.terminal_item.initialize();
		}

		@SubscribeEvent
		public static void registerModels(ModelRegistryEvent event) {
			// Items
			Items.requester.registerModels();
			Items.crafter.registerModels();
			Items.distributor.registerModels();

			Items.manager.registerModels();

			// Blocks
			Blocks.terminal_item.registerModels();
		}

		private static <V extends IForgeRegistryEntry<V>> void register(IForgeRegistry<V> registry, IInitializer i) {
			i.preInit();

			//noinspection unchecked
			registry.register((V) i);
		}

		private static void registerBlock(IForgeRegistry<Item> registry, Block block) {
			registry.register(new ItemBlock(block).setRegistryName(block.getRegistryName()));
		}

	}

}
