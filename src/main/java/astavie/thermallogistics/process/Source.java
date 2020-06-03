package astavie.thermallogistics.process;

import astavie.thermallogistics.util.RequesterReference;
import cofh.core.network.PacketBase;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagByte;
import net.minecraft.nbt.NBTTagCompound;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.Objects;

public class Source<I> {

	public final RequesterReference<I> crafter;
	public final byte side;

	public Source(byte side, RequesterReference<I> crafter) {
		this.crafter = crafter;
		this.side = side;
	}

	public Source(byte side) {
		this.crafter = null;
		this.side = side;
	}

	public static <I> Source<I> readPacket(PacketBase packet) {
		if (packet.getBool()) {
			return new Source<>((byte) 0, RequesterReference.readPacket(packet));
		} else {
			return new Source<>((byte) 0);
		}
	}

	public static void writePacket(PacketBase packet, Source<?> source) {
		packet.addBool(source.isCrafter());
		if (source.isCrafter()) {
			RequesterReference.writePacket(packet, source.crafter);
		}
	}

	public static <I> Source<I> readNbt(NBTBase nbt) {
		if (nbt instanceof NBTTagCompound) {
			return new Source<>(((NBTTagCompound) nbt).getByte("des"), RequesterReference.readNBT((NBTTagCompound) nbt));
		} else if (nbt instanceof NBTTagByte) {
			return new Source<>(((NBTTagByte) nbt).getByte());
		} else throw new IllegalArgumentException();
	}

	public static NBTBase writeNbt(Source<?> source) {
		if (source.isCrafter()) {
			NBTTagCompound nbt = RequesterReference.writeNBT(source.crafter);
			nbt.setByte("des", source.side);
			return nbt;
		} else {
			return new NBTTagByte(source.side);
		}
	}

	public boolean isCrafter() {
		return crafter != null;
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder().append(side).append(crafter).build();
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof Source && ((Source<?>) obj).side == side && Objects.equals(((Source<?>) obj).crafter, crafter);
	}

}
