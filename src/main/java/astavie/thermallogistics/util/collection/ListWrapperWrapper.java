package astavie.thermallogistics.util.collection;

import cofh.thermaldynamics.util.ListWrapper;

import javax.annotation.Nonnull;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.function.Function;

public class ListWrapperWrapper<F, T> extends ListWrapper<T> {

	private final ListWrapper<F> from;
	private final Function<F, T> func;

	public ListWrapperWrapper(ListWrapper<F> from, Function<F, T> func) {
		this.from = from;
		this.func = func;
	}

	@Override
	public void setList(LinkedList<T> list, SortType type) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void advanceCursor() {
		from.advanceCursor();
	}

	@Override
	public T peekRR() {
		return func.apply(from.peekRR());
	}

	@Override
	public int size() {
		return from.size();
	}

	@Nonnull
	@Override
	public Iterator<T> iterator() {
		Iterator<F> it = from.iterator();

		return new Iterator<T>() {

			@Override
			public boolean hasNext() {
				return it.hasNext();
			}

			@Override
			public T next() {
				return func.apply(it.next());
			}

		};
	}

}
