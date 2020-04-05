package astavie.thermallogistics.util.collection;

import astavie.thermallogistics.util.type.Type;
import cofh.core.network.PacketBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.stream.Collectors;

public abstract class StackList<S> {

	protected final Map<Type<S>, Pair<Long, Boolean>> map = new LinkedHashMap<>();

	public abstract Type<S> getType(S stack);

	public abstract int getAmount(S stack);

	public void add(S stack) {
		add(getType(stack), getAmount(stack));
	}

	public void add(Type<S> type, long amount) {
		if (!type.isNothing() && amount > 0) {
			map.compute(type, (t, p) -> Pair.of(
					(p == null ? 0L : p.getLeft()) + amount,
					p == null ? false : p.getRight()
			));
		}
	}

	public void addAll(StackList<S> list) {
		for (Map.Entry<Type<S>, Pair<Long, Boolean>> entry : list.map.entrySet()) {
			map.compute(entry.getKey(), (t, p) -> Pair.of(
					(p == null ? 0L : p.getLeft()) + entry.getValue().getLeft(),
					(p == null ? false : p.getRight()) || entry.getValue().getRight()
			));
		}
	}

	public void addCraftable(Type<S> type) {
		if (!type.isNothing()) {
			map.compute(type, (t, p) -> Pair.of(
					p == null ? 0L : p.getLeft(),
					true
			));
		}
	}

	public long remove(S stack) {
		return remove(getType(stack), getAmount(stack));
	}

	public long remove(Type<S> type, long count) {
		Pair<Long, Boolean> amount = map.get(type);
		if (amount == null)
			return count;

		if (count < amount.getLeft()) {
			map.put(type, Pair.of(amount.getLeft() - count, amount.getRight()));
			return 0;
		} else {
			if (amount.getRight())
				map.put(type, Pair.of(0L, true));
			else
				map.remove(type);

			return (int) (count - amount.getLeft());
		}
	}

	public long remove(Type<S> type, long count, boolean ignoreMod, boolean ignoreOreDict, boolean ignoreMetadata, boolean ignoreNbt) {
		for (Type<S> compare : map.keySet()) {
			if (type.isIdentical(compare, ignoreMod, ignoreOreDict, ignoreMetadata, ignoreNbt)) {
				count = remove(compare, count);
				if (count == 0) {
					return 0;
				}
			}
		}
		return count;
	}

	public Pair<Type<S>, Long> remove(int index) {
		Iterator<Map.Entry<Type<S>, Pair<Long, Boolean>>> iterator = map.entrySet().iterator();
		for (int i = 0; i < index; i++)
			iterator.next();

		Map.Entry<Type<S>, Pair<Long, Boolean>> entry = iterator.next();
		iterator.remove();

		return Pair.of(entry.getKey(), entry.getValue().getLeft());
	}

	public int size() {
		return map.size();
	}

	public boolean isEmpty() {
		return map.isEmpty();
	}

	public void clear() {
		map.clear();
	}

	public long amount(Type<S> type) {
		return map.getOrDefault(type, Pair.of(0L, false)).getLeft();
	}

	public long amount(S stack) {
		return amount(getType(stack));
	}

	public boolean craftable(Type<S> type) {
		return map.getOrDefault(type, Pair.of(0L, false)).getRight();
	}

	public boolean craftable(S stack) {
		return craftable(getType(stack));
	}

	public Set<Type<S>> types() {
		return Collections.unmodifiableSet(new HashSet<>(map.keySet()));
	}

	public List<S> stacks() {
		return map.entrySet().stream().map(e -> e.getKey().withAmount(Math.toIntExact(e.getValue().getLeft()))).collect(Collectors.toList());
	}

	protected void writeType(Type<S> type, PacketBase packet) {
		type.writePacket(packet);
	}

	public abstract Type<S> readType(PacketBase packet);

	protected NBTTagCompound writeType(Type<S> type) {
		return type.writeNbt();
	}

	public abstract Type<S> readType(NBTTagCompound tag);

	public void writePacket(PacketBase packet) {
		packet.addInt(map.size());
		for (Map.Entry<Type<S>, Pair<Long, Boolean>> entry : map.entrySet()) {
			writeType(entry.getKey(), packet);
			packet.addLong(entry.getValue().getLeft());
			packet.addBool(entry.getValue().getRight());
		}
	}

	public void readPacket(PacketBase packet) {
		map.clear();

		int size = packet.getInt();
		for (int i = 0; i < size; i++) {
			map.put(readType(packet), Pair.of(packet.getLong(), packet.getBool()));
		}
	}

	public NBTTagList writeNbt() {
		NBTTagList list = new NBTTagList();
		for (Map.Entry<Type<S>, Pair<Long, Boolean>> entry : map.entrySet()) {
			NBTTagCompound nbt = writeType(entry.getKey());
			nbt.setLong("Count", entry.getValue().getLeft());
			list.appendTag(nbt);
		}
		return list;
	}

	public void readNbt(NBTTagList list) {
		map.clear();

		for (int i = 0; i < list.tagCount(); i++) {
			NBTTagCompound tag = list.getCompoundTagAt(i);
			map.put(readType(tag), Pair.of(tag.getLong("Count"), false));
		}
	}

	public abstract StackList<S> copy();

}
