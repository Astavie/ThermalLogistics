package astavie.thermallogistics.util;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class Shared<T> implements Consumer<T>, Supplier<T> {

	private T t;

	public Shared() {
	}

	public Shared(T t) {
		this.t = t;
	}

	@Override
	public void accept(T t) {
		this.t = t;
	}

	@Override
	public T get() {
		return t;
	}

}
