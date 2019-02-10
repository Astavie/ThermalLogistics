package astavie.thermallogistics.attachment;

import java.util.List;

public interface IRequesterContainer {

	List<IRequester<?>> getRequesters();

	IRequester<?> getRequester(int index);

}
