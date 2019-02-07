package astavie.thermallogistics.attachment;

import astavie.thermallogistics.process.Request;
import net.minecraft.util.NonNullList;

import java.util.List;

public interface ICrafter<I> extends IRequester<I> {

	List<I> getOutputs();

	boolean request(IRequester<I> requester, I stack);

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
