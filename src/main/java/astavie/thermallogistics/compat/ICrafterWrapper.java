package astavie.thermallogistics.compat;

import java.util.List;

public interface ICrafterWrapper {

	<T> List<T> getInputs(Class<T> type);

	<T> List<T> getOutputs(Class<T> type);

}
