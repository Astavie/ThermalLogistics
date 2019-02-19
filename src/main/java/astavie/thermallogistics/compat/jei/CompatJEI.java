package astavie.thermallogistics.compat.jei;

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

		registry.getRecipeTransferRegistry().addRecipeTransferHandler(new TerminalHandler(helper), VanillaRecipeCategoryUid.CRAFTING);
		registry.getRecipeTransferRegistry().addUniversalRecipeTransferHandler(new CrafterHandler());
	}

}
