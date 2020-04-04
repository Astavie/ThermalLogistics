package astavie.thermallogistics.util.collection;

import astavie.thermallogistics.util.type.Type;
import cofh.core.network.PacketBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import org.apache.commons.lang3.tuple.Pair;

public class EmptyList<I> extends StackList<I> {

	@Override
	public Type<I> getType(I stack) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getAmount(I stack) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Type<I> readType(PacketBase packet) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Type<I> readType(NBTTagCompound tag) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void add(Type<I> type, long amount) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void addAll(StackList<I> list) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void addCraftable(Type<I> type) {
		throw new UnsupportedOperationException();
	}

	@Override
	public long remove(Type<I> type, long count) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Pair<Type<I>, Long> remove(int index) {
		throw new UnsupportedOperationException();
	}

	@Override
	public NBTTagList writeNbt() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void writePacket(PacketBase packet) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void readNbt(NBTTagList list) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void readPacket(PacketBase packet) {
		throw new UnsupportedOperationException();
	}

	@Override
	public long amount(I stack) {
		return 0;
	}

	@Override
	public boolean craftable(I stack) {
		return false;
	}

	@Override
	public StackList<I> copy() {
		return this;
	}

}
