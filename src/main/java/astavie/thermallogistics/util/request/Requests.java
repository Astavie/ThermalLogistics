package astavie.thermallogistics.util.request;

import astavie.thermallogistics.process.IProcess;
import astavie.thermallogistics.util.delegate.IDelegate;
import cofh.core.network.PacketBase;
import cofh.thermaldynamics.duct.tiles.DuctUnit;
import net.minecraft.world.World;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class Requests<T extends DuctUnit<T, ?, ?>, I> implements Comparable<Requests<T, I>> {

	private final List<IRequest<T, I>> requests;
	private final int x, y, z, side, index;
	private final long age;

	public Requests(IProcess<?, T, I> process, List<IRequest<T, I>> requests) {
		this.requests = requests;

		this.x = process.getBase().getX();
		this.y = process.getBase().getY();
		this.z = process.getBase().getZ();
		this.side = process.getSide();
		this.index = process.getIndex();
		this.age = process.getAge();
	}

	public Requests(World world, IDelegate<I> delegate, PacketBase packet) {
		this.requests = new LinkedList<>();

		this.x = packet.getInt();
		this.y = packet.getInt();
		this.z = packet.getInt();
		this.side = packet.getInt();
		this.index = packet.getInt();
		this.age = packet.getLong();

		int size = packet.getInt();
		for (int i = 0; i < size; i++)
			requests.add(new Request<>(world, delegate, packet));
	}

	public void writeCancel(PacketBase packet) {
		packet.addInt(x);
		packet.addInt(y);
		packet.addInt(z);
		packet.addInt(side);
		packet.addInt(index);
	}

	public void writePacket(IDelegate<I> delegate, PacketBase packet) {
		packet.addInt(x);
		packet.addInt(y);
		packet.addInt(z);
		packet.addInt(side);
		packet.addInt(index);
		packet.addLong(age);

		packet.addInt(requests.size());
		for (IRequest<T, I> request : requests)
			IRequest.writePacket(request, delegate, packet);
	}

	public void condense(World world, IDelegate<I> delegate) {
		Iterator<IRequest<T, I>> a = requests.iterator();
		while (a.hasNext()) {
			IRequest<T, I> fa = a.next();
			for (int i = 0; i < requests.size(); i++) {
				IRequest<T, I> fb = requests.get(i);
				if (fa == fb)
					break;

				if (fa.getStart().getBase().compareTo(fb.getStart().getBase()) == 0 && fa.getStart().getSide() == fb.getStart().getSide()) {
					requests.set(i, Request.combine(world, delegate, fb, fa));
					a.remove();
					break;
				}
			}
		}
	}

	public List<IRequest<T, I>> getRequests() {
		return requests;
	}

	@Override
	public int compareTo(Requests<T, I> o) {
		return Long.compare(age, o.age);
	}

}
