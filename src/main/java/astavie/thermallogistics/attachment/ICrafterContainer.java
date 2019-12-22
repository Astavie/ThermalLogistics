package astavie.thermallogistics.attachment;

import java.util.List;
import java.util.stream.Collectors;

public interface ICrafterContainer<I> extends IRequesterContainer<I> {

	List<ICrafter<I>> getCrafters();

	@Override
	default List<IRequester<I>> getRequesters() {
		return getCrafters().stream().map(c -> (IRequester<I>) c).collect(Collectors.toList());
	}

}
