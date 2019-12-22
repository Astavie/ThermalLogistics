package astavie.thermallogistics.attachment;

import java.util.List;

public interface IRequesterContainer<I> {

	List<IRequester<I>> getRequesters();

}
