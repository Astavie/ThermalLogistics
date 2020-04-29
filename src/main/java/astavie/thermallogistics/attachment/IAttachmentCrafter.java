package astavie.thermallogistics.attachment;

import astavie.thermallogistics.util.collection.StackList;
import net.minecraft.entity.player.EntityPlayer;

import java.util.List;
import java.util.function.Supplier;

public interface IAttachmentCrafter<I> extends ICrafterContainer<I> {

	Class<I> getItemClass();

	List<Recipe<I>> getRecipes();

	Supplier<StackList<I>> getSupplier();

	void split(int split);

	void sync(EntityPlayer player);

	void markDirty();

}
