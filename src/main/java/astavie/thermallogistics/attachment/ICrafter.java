package astavie.thermallogistics.attachment;

import astavie.thermallogistics.process.Request;
import astavie.thermallogistics.util.RequesterReference;
import cofh.core.network.PacketTileInfo;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.NonNullList;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public interface ICrafter<I> extends IRequester<I> {

	List<I> getOutputs();

	Set<RequesterReference<I>> getBlacklist();

	boolean request(IRequester<I> requester, I stack);

	void link(ICrafter<?> crafter, boolean recursion);

	boolean hasLinked(ICrafter<?> crafter);

	int getRecipes(int index);

	void sync(EntityPlayer player);

	List<RequesterReference<?>> getLinked();

	PacketTileInfo getNewPacket(byte type);

	List<Recipe<I>> getRecipes();

	void split(int split);

	Class<I> getItemClass();

	class Recipe<I> {

		public final List<I> inputs = new LinkedList<>();
		public final List<I> outputs = new LinkedList<>();

		public final List<Request<I>> requests = NonNullList.create();
		public final Request<I> leftovers;

		public Recipe(Request<I> leftovers) {
			this.leftovers = leftovers;
		}

	}

}
