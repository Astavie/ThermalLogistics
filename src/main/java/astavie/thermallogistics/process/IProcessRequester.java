package astavie.thermallogistics.process;

import astavie.thermallogistics.attachment.IRequester;
import astavie.thermallogistics.util.collection.StackList;
import cofh.thermaldynamics.duct.tiles.DuctUnit;
import cofh.thermaldynamics.multiblock.MultiBlockGrid;
import cofh.thermaldynamics.util.ListWrapper;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Map;
import java.util.Set;

public interface IProcessRequester<I> extends IRequester<I> {

	/**
	 * Requests of this requester. Should be a copy of the real map.
	 */
	Map<Source<I>, StackList<I>> getRequests();

	/**
	 * @return A list of sources sorted by distance
	 */
	ListWrapper<Pair<DuctUnit, Byte>> getSources(byte side);

	DuctUnit<?, ?, ?> getDuct(byte side);

	Set<MultiBlockGrid<?>> getGrids();

}
