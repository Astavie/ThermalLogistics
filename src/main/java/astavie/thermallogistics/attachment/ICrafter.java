package astavie.thermallogistics.attachment;

import astavie.thermallogistics.process.Request;
import astavie.thermallogistics.util.RequesterReference;
import net.minecraft.util.NonNullList;

import java.util.List;
import java.util.Set;

public interface ICrafter<I> extends IRequester<I> {

	List<I> getOutputs();

	Set<RequesterReference<I>> getBlacklist();

	boolean request(IRequester<I> requester, I stack);

	void link(ICrafter<?> crafter, boolean recursion);

	boolean hasLinked(ICrafter<?> crafter);

	int getRecipes(int index);

	class Recipe<I> {

		public final List<I> inputs = NonNullList.create();
		public final List<I> outputs = NonNullList.create();

		public final List<Request<I>> requests = NonNullList.create();
		public final Request<I> leftovers;

		public Recipe(Request<I> leftovers) {
			this.leftovers = leftovers;
		}

	}

}
