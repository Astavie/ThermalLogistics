package astavie.thermallogistics.util.type;

public interface Type<I> {

	I getAsStack();

	I withAmount(int amount);

	String getDisplayName();

}
