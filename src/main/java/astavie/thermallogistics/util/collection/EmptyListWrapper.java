package astavie.thermallogistics.util.collection;

import cofh.thermaldynamics.util.ListWrapper;

import java.util.LinkedList;

public class EmptyListWrapper<T> extends ListWrapper<T> {

	public EmptyListWrapper() {
		setList(new LinkedList<>(), SortType.NORMAL);
	}

}
