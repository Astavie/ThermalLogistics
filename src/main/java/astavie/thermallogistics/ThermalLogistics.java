package astavie.thermallogistics;

import astavie.thermallogistics.attachment.CrafterItem;
import astavie.thermallogistics.attachment.RequesterItem;
import astavie.thermallogistics.item.ItemCrafter;
import astavie.thermallogistics.item.ItemRequester;
import cofh.core.util.core.IInitializer;
import cofh.thermaldynamics.duct.AttachmentRegistry;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.crafting.IRecipe;
import net.minecraftforge.client.event.ModelRegistryEvent;
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

	@Mod.EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		AttachmentRegistry.registerAttachment(RequesterItem.ID, RequesterItem::new);
		AttachmentRegistry.registerAttachment(CrafterItem.ID, CrafterItem::new);
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
