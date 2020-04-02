package astavie.thermallogistics.compat.jei;

import astavie.thermallogistics.client.gui.GuiCrafter;
import astavie.thermallogistics.client.gui.GuiTerminalItem;
import mezz.jei.Internal;
import mezz.jei.api.*;
import mezz.jei.api.recipe.VanillaRecipeCategoryUid;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.config.KeyBindings;
import mezz.jei.input.IClickedIngredient;
import mezz.jei.input.InputHandler;
import mezz.jei.input.MouseHelper;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@JEIPlugin
public class CompatJEI implements IModPlugin {

	private static Method method = ReflectionHelper.findMethod(InputHandler.class, "getIngredientUnderMouseForKey", null, int.class, int.class);

	private static IIngredientFilter filter;

	@Override
	public void register(IModRegistry registry) {
		IRecipeTransferHandlerHelper helper = registry.getJeiHelpers().recipeTransferHandlerHelper();

		CrafterHandler crafter = new CrafterHandler();
		TerminalHandler terminal = new TerminalHandler(helper);

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
				InputHandler handler = ReflectionHelper.getPrivateValue(Internal.class, null, "inputHandler");
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
