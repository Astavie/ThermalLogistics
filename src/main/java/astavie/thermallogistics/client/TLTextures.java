package astavie.thermallogistics.client;

import astavie.thermallogistics.ThermalLogistics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(value = Side.CLIENT, modid = ThermalLogistics.MOD_ID)
public class TLTextures {

	public static final TextureAtlasSprite[][] REQUESTER = new TextureAtlasSprite[2][];
	public static final TextureAtlasSprite[][] CRAFTER = new TextureAtlasSprite[2][];
	public static final TextureAtlasSprite[][] DISTRIBUTOR = new TextureAtlasSprite[2][];

	public static TextureAtlasSprite ICON_LINK;
	public static TextureAtlasSprite ICON_ARROW_RIGHT;
	public static TextureAtlasSprite ICON_FLUID;
	public static TextureAtlasSprite ICON_CRAFTING;

	@SubscribeEvent
	public static void onTextureStitch(TextureStitchEvent.Pre event) {
		registerAttachment(event.getMap(), REQUESTER, "requester");
		registerAttachment(event.getMap(), CRAFTER, "crafter");
		registerAttachment(event.getMap(), DISTRIBUTOR, "distributor");

		ICON_LINK = event.getMap().registerSprite(new ResourceLocation(ThermalLogistics.MOD_ID, "items/manager"));
		ICON_ARROW_RIGHT = event.getMap().registerSprite(new ResourceLocation(ThermalLogistics.MOD_ID, "gui/icons/arrow_right"));
		ICON_FLUID = event.getMap().registerSprite(new ResourceLocation("items/bucket_water"));
		ICON_CRAFTING = event.getMap().registerSprite(new ResourceLocation(ThermalLogistics.MOD_ID, "gui/icons/crafting"));
	}

	private static void registerAttachment(TextureMap map, TextureAtlasSprite[][] array, String name) {
		name = "blocks/attachment/" + name + "/" + name + "_base_";

		array[0] = new TextureAtlasSprite[5];
		array[0][0] = map.registerSprite(new ResourceLocation(ThermalLogistics.MOD_ID, name + "0_0"));
		array[0][1] = map.registerSprite(new ResourceLocation(ThermalLogistics.MOD_ID, name + "0_1"));
		array[0][2] = map.registerSprite(new ResourceLocation(ThermalLogistics.MOD_ID, name + "0_2"));
		array[0][3] = map.registerSprite(new ResourceLocation(ThermalLogistics.MOD_ID, name + "0_3"));
		array[0][4] = map.registerSprite(new ResourceLocation(ThermalLogistics.MOD_ID, name + "0_4"));

		array[1] = new TextureAtlasSprite[5];
		array[1][0] = map.registerSprite(new ResourceLocation(ThermalLogistics.MOD_ID, name + "1_0"));
		array[1][1] = map.registerSprite(new ResourceLocation(ThermalLogistics.MOD_ID, name + "1_1"));
		array[1][2] = map.registerSprite(new ResourceLocation(ThermalLogistics.MOD_ID, name + "1_2"));
		array[1][3] = map.registerSprite(new ResourceLocation(ThermalLogistics.MOD_ID, name + "1_3"));
		array[1][4] = map.registerSprite(new ResourceLocation(ThermalLogistics.MOD_ID, name + "1_4"));
	}

}
