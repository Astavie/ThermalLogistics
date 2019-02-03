package astavie.thermallogistics;

import astavie.thermallogistics.attachment.CrafterFluid;
import astavie.thermallogistics.attachment.CrafterItem;
import astavie.thermallogistics.attachment.RequesterFluid;
import astavie.thermallogistics.attachment.RequesterItem;
import astavie.thermallogistics.block.BlockTerminal;
import astavie.thermallogistics.block.BlockTerminalItem;
import astavie.thermallogistics.block.ItemBlockTerminal;
import astavie.thermallogistics.compat.FabricatorWrapper;
import astavie.thermallogistics.compat.ICrafterWrapper;
import astavie.thermallogistics.event.EventHandler;
import astavie.thermallogistics.item.ItemCrafter;
import astavie.thermallogistics.item.ItemManager;
import astavie.thermallogistics.item.ItemRequester;
import astavie.thermallogistics.network.PacketCancelProcess;
import astavie.thermallogistics.network.PacketTick;
import astavie.thermallogistics.process.IProcess;
import astavie.thermallogistics.process.ProcessFluid;
import astavie.thermallogistics.process.ProcessItem;
import astavie.thermallogistics.proxy.Proxy;
import cofh.core.gui.CreativeTabCore;
import cofh.thermaldynamics.duct.AttachmentRegistry;
import cofh.thermaldynamics.item.ItemAttachment;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

@Mod(modid = ThermalLogistics.MODID, name = "Thermal Logistics", dependencies = "required-after:thermaldynamics;")
public class ThermalLogistics {

	public static final String MODID = "thermallogistics";
	public static final String VERSION = Loader.instance().activeModContainer().getVersion();

	public static final CreativeTabs tab = new CreativeTabCore(MODID) {
		@Override
		public ItemStack createIcon() {
			return new ItemStack(terminal);
		}
	};

	// Items
	public static final ItemAttachment requester = new ItemRequester();
	public static final ItemAttachment crafter = new ItemCrafter();
	public static final ItemManager manager = new ItemManager();

	// Blocks
	public static final ItemBlockTerminal terminal = new ItemBlockTerminal();
	public static final BlockTerminal terminalItem = new BlockTerminalItem();

	private static final Map<Class<?>, Function<?, ICrafterWrapper>> crafterRegistry = new HashMap<>();
	private static final Map<ResourceLocation, BiFunction<World, NBTTagCompound, IProcess<?, ?, ?>>> processRegistry = new HashMap<>();

	public static Logger logger;

	@SidedProxy(serverSide = "astavie.thermallogistics.proxy.Proxy", clientSide = "astavie.thermallogistics.proxy.ProxyClient")
	public static Proxy proxy;

	public static <T extends TileEntity> void registerCrafter(Class<T> c, Function<T, ICrafterWrapper> f) {
		crafterRegistry.put(c, f);
	}

	public static void registerProcessType(ResourceLocation id, BiFunction<World, NBTTagCompound, IProcess<?, ?, ?>> process) {
		processRegistry.put(id, process);
	}

	@SuppressWarnings("unchecked")
	public static <T extends TileEntity> ICrafterWrapper getWrapper(T c) {
		Function<T, ICrafterWrapper> f = (Function<T, ICrafterWrapper>) crafterRegistry.get(c.getClass());
		return f == null ? null : f.apply(c);
	}

	public static IProcess readProcess(World world, NBTTagCompound tag) {
		ResourceLocation id = new ResourceLocation(tag.getString("id"));
		if (processRegistry.containsKey(id))
			return processRegistry.get(id).apply(world, tag);
		throw new IllegalStateException("Unknown process id");
	}

	@Mod.EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		logger = event.getModLog();

		requester.preInit();
		crafter.preInit();
		manager.preInit();

		terminal.preInit();
		terminalItem.preInit();

		AttachmentRegistry.registerAttachment(RequesterItem.ID, RequesterItem::new);
		AttachmentRegistry.registerAttachment(RequesterFluid.ID, RequesterFluid::new);
		AttachmentRegistry.registerAttachment(CrafterItem.ID, CrafterItem::new);
		AttachmentRegistry.registerAttachment(CrafterFluid.ID, CrafterFluid::new);

		registerProcessType(new ResourceLocation("item"), ProcessItem::new);
		registerProcessType(new ResourceLocation("fluid"), ProcessFluid::new);

		MinecraftForge.EVENT_BUS.register(new EventHandler());

		PacketCancelProcess.initialize();
		PacketTick.initialize();

		proxy.preInit();
	}

	@Mod.EventHandler
	public void init(FMLInitializationEvent event) {
		requester.initialize();
		crafter.initialize();
		manager.initialize();

		terminal.initialize();
		terminalItem.initialize();
	}

	@Mod.EventHandler
	public void postInit(FMLPostInitializationEvent event) {
		if (Loader.isModLoaded("thermalexpansion"))
			registerCrafter(FabricatorWrapper.CLASS, FabricatorWrapper::new);
	}

}
