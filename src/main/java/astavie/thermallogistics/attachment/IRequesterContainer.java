package astavie.thermallogistics.attachment;

import java.util.List;

public interface IRequesterContainer<I> {

	List<? extends IRequester<I>> getRequesters(); // TODO: Requesters of different types?

}
