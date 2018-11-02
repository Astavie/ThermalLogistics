package astavie.thermallogistics.proxy;

import astavie.thermallogistics.ThermalLogistics;
import astavie.thermallogistics.attachment.Crafter;
import astavie.thermallogistics.attachment.CrafterFluid;
import astavie.thermallogistics.attachment.CrafterItem;
import astavie.thermallogistics.gui.client.GuiCrafter;
import astavie.thermallogistics.gui.client.slots.CrafterFluidSlots;
import astavie.thermallogistics.gui.client.slots.CrafterItemSlots;
import astavie.thermallogistics.gui.client.slots.ICrafterSlots;
import astavie.thermallogistics.gui.client.summary.CrafterFluidSummary;
import astavie.thermallogistics.gui.client.summary.CrafterItemSummary;
import astavie.thermallogistics.gui.client.summary.ICrafterSummary;
import cofh.core.gui.GuiContainerCore;
import cofh.core.render.IModelRegister;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@SideOnly(Side.CLIENT)
public class ProxyClient extends Proxy {

	private static final Map<Class, ICrafterSlots> SLOT_REGISTRY = new HashMap<>();
	private static final Map<Class, ICrafterSummary> SUMMARY_REGISTRY = new HashMap<>();

	public static final TextureAtlasSprite[][] REQUESTER = new TextureAtlasSprite[2][];
	public static final TextureAtlasSprite[][] CRAFTER = new TextureAtlasSprite[2][];

	public static TextureAtlasSprite ICON_LINK;
	public static TextureAtlasSprite ICON_ARROW_RIGHT;
	public static TextureAtlasSprite ICON_FLUID;

	private final Set<IModelRegister> models = new HashSet<>();

	@Override
	public void preInit() {
		MinecraftForge.EVENT_BUS.register(this);
		models.forEach(IModelRegister::registerModels);

		SLOT_REGISTRY.put(CrafterItem.class, new CrafterItemSlots());
		SLOT_REGISTRY.put(CrafterFluid.class, new CrafterFluidSlots());

		SUMMARY_REGISTRY.put(CrafterItem.class, new CrafterItemSummary());
		SUMMARY_REGISTRY.put(CrafterFluid.class, new CrafterFluidSummary());
	}

	public static void addSlots(Crafter crafter, GuiCrafter gui) {
		//noinspection unchecked,SuspiciousMethodCalls
		SLOT_REGISTRY.get(crafter.getClass()).addSlots(crafter, gui);
	}

	public static void renderSummary(Crafter crafter, GuiContainerCore gui, int x, int y, int text) {
		//noinspection unchecked,SuspiciousMethodCalls
		SUMMARY_REGISTRY.get(crafter.getClass()).renderSummary(crafter, gui, x, y, text);
	}

	@SubscribeEvent
	public void handleTextureStitchEventPre(TextureStitchEvent.Pre event) {
		registerAttachment(event.getMap(), REQUESTER, "requester");
		registerAttachment(event.getMap(), CRAFTER, "crafter");

		ICON_LINK = event.getMap().registerSprite(new ResourceLocation(ThermalLogistics.MODID, "items/manager_1"));
		ICON_ARROW_RIGHT = event.getMap().registerSprite(new ResourceLocation(ThermalLogistics.MODID, "gui/icons/arrow_right"));
		ICON_FLUID = event.getMap().registerSprite(new ResourceLocation("items/bucket_water"));
	}

	private void registerAttachment(TextureMap map, TextureAtlasSprite[][] array, String name) {
		name = "blocks/attachment/" + name + "/" + name + "_base_";

		array[0] = new TextureAtlasSprite[5];
		array[0][0] = map.registerSprite(new ResourceLocation(ThermalLogistics.MODID, name + "0_0"));
		array[0][1] = map.registerSprite(new ResourceLocation(ThermalLogistics.MODID, name + "0_1"));
		array[0][2] = map.registerSprite(new ResourceLocation(ThermalLogistics.MODID, name + "0_2"));
		array[0][3] = map.registerSprite(new ResourceLocation(ThermalLogistics.MODID, name + "0_3"));
		array[0][4] = map.registerSprite(new ResourceLocation(ThermalLogistics.MODID, name + "0_4"));

		array[1] = new TextureAtlasSprite[5];
		array[1][0] = map.registerSprite(new ResourceLocation(ThermalLogistics.MODID, name + "1_0"));
		array[1][1] = map.registerSprite(new ResourceLocation(ThermalLogistics.MODID, name + "1_1"));
		array[1][2] = map.registerSprite(new ResourceLocation(ThermalLogistics.MODID, name + "1_2"));
		array[1][3] = map.registerSprite(new ResourceLocation(ThermalLogistics.MODID, name + "1_3"));
		array[1][4] = map.registerSprite(new ResourceLocation(ThermalLogistics.MODID, name + "1_4"));
	}

	@Override
	public void addModelRegister(IModelRegister model) {
		models.add(model);
	}

}
