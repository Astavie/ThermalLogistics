package astavie.thermallogistics.process;

import astavie.thermallogistics.util.RequesterReference;
import astavie.thermallogistics.util.type.Type;

import java.util.HashSet;
import java.util.Set;

public class Proposal<I> {

	public final RequesterReference<I> me;
	public final Type<I> type;
	public final long amount;
	public final boolean missing;

	public final Set<Proposal<I>> children = new HashSet<>();
	public final Set<Proposal<?>> linked = new HashSet<>();

	public Proposal(RequesterReference<I> me, Type<I> type, long amount, boolean missing) {
		this.me = me;
		this.type = type;
		this.amount = amount;
		this.missing = missing;
	}

	public Proposal(RequesterReference<I> me, Type<I> type, long amount) {
		this(me, type, amount, false);
	}

}
