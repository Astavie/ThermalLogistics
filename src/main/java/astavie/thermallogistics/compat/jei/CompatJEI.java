package astavie.thermallogistics.compat.jei;

import astavie.thermallogistics.client.gui.GuiTerminalItem;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.JEIPlugin;
import mezz.jei.api.recipe.VanillaRecipeCategoryUid;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;

@JEIPlugin
public class CompatJEI implements IModPlugin {

	@Override
	public void register(IModRegistry registry) {
		IRecipeTransferHandlerHelper helper = registry.getJeiHelpers().recipeTransferHandlerHelper();

		// TODO
		// CrafterHandler crafter = new CrafterHandler();
		TerminalHandler terminal = new TerminalHandler(helper);

		registry.getRecipeTransferRegistry().addRecipeTransferHandler(terminal, VanillaRecipeCategoryUid.CRAFTING);
		// registry.getRecipeTransferRegistry().addUniversalRecipeTransferHandler(crafter);

		registry.addAdvancedGuiHandlers(terminal/*, crafter*/);

		registry.addGhostIngredientHandler(GuiTerminalItem.class, terminal);
		// registry.addGhostIngredientHandler(GuiCrafter.class, crafter);
	}

}
