package astavie.thermallogistics.attachment;

import net.minecraft.entity.player.EntityPlayer;

public interface IAttachmentCrafter<I> extends ICrafterContainer<I> {

	Class<I> getItemClass();

	void split(int split);

	void sync(EntityPlayer player);

	void markDirty();

}
