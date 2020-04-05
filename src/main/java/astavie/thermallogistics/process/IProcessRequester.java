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

	void addRequest(Request<I> request);

	/**
	 * @return If the requester should automatically requests more items
	 */
	boolean hasWants();

	/**
	 * @return How much the requester wants of the item type
	 */
	long amountRequired(Type<I> type);

}
