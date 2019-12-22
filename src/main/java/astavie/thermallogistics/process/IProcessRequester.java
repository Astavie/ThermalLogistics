package astavie.thermallogistics.process;

import astavie.thermallogistics.attachment.ICrafter;
import astavie.thermallogistics.attachment.IRequester;
import astavie.thermallogistics.util.RequesterReference;
import astavie.thermallogistics.util.collection.StackList;
import astavie.thermallogistics.util.type.Type;
import cofh.thermaldynamics.duct.tiles.DuctUnit;
import cofh.thermaldynamics.util.ListWrapper;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Map;

public interface IProcessRequester<I> extends IRequester<I> {

	/**
	 * Requests of this requester. Should be a copy of the real map.
	 */
	Map<RequesterReference<I>, StackList<I>> getRequests();

	/**
	 * @return A list of sources sorted by distance
	 */
	ListWrapper<Pair<DuctUnit, Byte>> getSources();

	/**
	 * A crafter has sent this stack toward this requester
	 */
	void onCrafterSend(ICrafter<I> crafter, Type<I> type, long amount);

	boolean hasWants();

	long amountRequired(Type<I> type);

	void addRequest(Request<I> request);

}
