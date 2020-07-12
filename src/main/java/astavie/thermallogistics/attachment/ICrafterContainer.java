package astavie.thermallogistics.attachment;

import java.util.List;

public interface ICrafterContainer<I> extends IRequesterContainer<I> {

	List<? extends ICrafter<I>> getCrafters();

	@Override
	default List<? extends IRequester<I>> getRequesters() {
		return getCrafters();
	}

}
