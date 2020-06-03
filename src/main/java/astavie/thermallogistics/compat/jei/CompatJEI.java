package astavie.thermallogistics.compat.jei;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import astavie.thermallogistics.client.gui.GuiCrafter;
import astavie.thermallogistics.client.gui.GuiTerminalItem;
import mezz.jei.Internal;
import mezz.jei.api.IIngredientFilter;
import mezz.jei.api.IJeiRuntime;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.JEIPlugin;
import mezz.jei.api.recipe.VanillaRecipeCategoryUid;
import mezz.jei.config.KeyBindings;
import mezz.jei.input.IClickedIngredient;
import mezz.jei.input.InputHandler;
import mezz.jei.input.MouseHelper;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

@JEIPlugin
public class CompatJEI implements IModPlugin {

	private static Method method = ObfuscationReflectionHelper.findMethod(InputHandler.class, "getIngredientUnderMouseForKey", null, int.class, int.class);

	private static IIngredientFilter filter;

	@Override
	public void register(IModRegistry registry) {
		CrafterHandler crafter = new CrafterHandler();
		TerminalHandler terminal = new TerminalHandler();

		registry.getRecipeTransferRegistry().addRecipeTransferHandler(terminal, VanillaRecipeCategoryUid.CRAFTING);
		registry.getRecipeTransferRegistry().addUniversalRecipeTransferHandler(crafter);

		registry.addAdvancedGuiHandlers(terminal, crafter);

		registry.addGhostIngredientHandler(GuiTerminalItem.class, terminal);
		registry.addGhostIngredientHandler(GuiCrafter.class, crafter);
	}

	public static void synchronize(String search) {
		filter.setFilterText(search);
	}

	public static boolean checkKey(int key) {
		boolean showRecipe = KeyBindings.showRecipe.isActiveAndMatches(key);
		boolean showUses = KeyBindings.showUses.isActiveAndMatches(key);
		boolean bookmark = KeyBindings.bookmark.isActiveAndMatches(key);

		if (showRecipe || showUses || bookmark) {
			try {
				InputHandler handler = ObfuscationReflectionHelper.getPrivateValue(Internal.class, null, "inputHandler");
				IClickedIngredient<?> clicked = (IClickedIngredient<?>) method.invoke(handler, MouseHelper.getX(), MouseHelper.getY());
				return clicked != null;
			} catch (IllegalAccessException | InvocationTargetException e) {
				e.printStackTrace();
			}
		}

		return false;
	}

	@Override
	public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
		filter = jeiRuntime.getIngredientFilter();
	}

}
