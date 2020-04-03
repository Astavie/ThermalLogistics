package astavie.thermallogistics.attachment;

import astavie.thermallogistics.util.RequesterReference;
import astavie.thermallogistics.util.collection.StackList;
import astavie.thermallogistics.util.type.Type;
import cofh.thermaldynamics.duct.attachments.servo.ServoBase;
import cofh.thermaldynamics.duct.tiles.DuctUnit;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class Recipe<I> implements ICrafter<I> {

	public final List<RequesterReference<?>> linked = NonNullList.create();

	public boolean enabled = true;
	public int index;

	public List<I> inputs = NonNullList.create();
	public List<I> outputs = NonNullList.create();
	public Map<RequesterReference<I>, StackList<I>> requestOutput = new HashMap<>();
	public StackList<I> leftovers;
	public Map<RequesterReference<I>, StackList<I>> requestInput = new HashMap<>();

	// Requests
	private ServoBase parent;
	private DuctUnit<?, ?, ?> duct;
	private Supplier<StackList<I>> supplier;

	public Recipe(ServoBase parent, DuctUnit<?, ?, ?> duct, Supplier<StackList<I>> supplier, int index) {
		this.parent = parent;
		this.duct = duct;
		this.supplier = supplier;
		this.leftovers = supplier.get();
		this.index = index;
	}

	public void onDisable() {
		requestInput.clear();
		leftovers.clear();
	}

	@Override
	public Collection<RequesterReference<?>> getLinked() {
		return linked;
	}

	@Override
	public boolean isLinked(RequesterReference<?> reference) {
		return linked.contains(reference);
	}

	@Override
	public void link(ICrafter<?> crafter) {
		// TODO: Check linked
		RequesterReference<?> reference = crafter.createReference();

		if (!isLinked(reference)) {
			for (RequesterReference<?> link : linked) {
				ICrafter<?> oldie = (ICrafter<?>) link.get();
				oldie.link(reference);
				crafter.link(link);
			}

			linked.add(reference);
			crafter.link(createReference());

			markDirty();
		}
	}

	private void markDirty() {
		parent.baseTile.markChunkDirty();
	}

	@Override
	public void unlink(ICrafter<?> crafter) {
		// TODO: Check linked
		RequesterReference<?> reference = crafter.createReference();

		for (RequesterReference<?> link : linked) {
			ICrafter<?> oldie = (ICrafter<?>) link.get();
			oldie.unlink(reference);
			crafter.unlink(link);
		}

		linked.remove(reference);
		crafter.unlink(createReference());

		markDirty();
	}

	@Override
	public void link(RequesterReference<?> reference) {
		if (!isLinked(reference)) {
			linked.add(reference);
			markDirty();
		}
	}

	@Override
	public void unlink(RequesterReference<?> reference) {
		linked.remove(reference);
		markDirty();
	}

	@Override
	public boolean isEnabled() {
		return enabled && parent.isPowered;
	}

	@Override
	public Collection<I> getOutputs() {
		return outputs;
	}

	@Override
	public boolean request(IRequester<I> requester, Type<I> type, long amount) {
		return false; // TODO
	}

	@Override
	public void cancel(IRequester<I> requester, Type<I> type, long amount) {
		// TODO
	}

	@Override
	public long reserved(IRequester<I> requester, Type<I> type) {
		return requestOutput.get(requester.createReference()).amount(type);
	}

	@Override
	public void finish(IRequester<I> requester, Type<I> type, long amount) {
		requestOutput.get(requester.createReference()).remove(type, amount);
		markDirty();
	}

	@Override
	public boolean referencedBy(RequesterReference<?> reference) {
		return reference.dim == parent.baseTile.world().provider.getDimension() && reference.pos.equals(parent.baseTile.getPos()) && reference.side == parent.side && reference.index == index;
	}

	@Override
	public RequesterReference<I> createReference() {
		return new RequesterReference<>(parent.baseTile.world().provider.getDimension(), parent.baseTile.getPos(), parent.side, index);
	}

	@Override
	public ItemStack getIcon() {
		return parent.getPickBlock();
	}

	@Override
	public ItemStack getTileIcon() {
		// Sorry cpw, but I need some reflection
		TileEntity tile = ReflectionHelper.getPrivateValue(ServoBase.class, parent, "myTile");
		return tile == null ? ItemStack.EMPTY : tile.getBlockType().getItem(tile.getWorld(), tile.getPos(), tile.getWorld().getBlockState(tile.getPos()));
	}

	@Override
	public BlockPos getDestination() {
		return parent.baseTile.getPos().offset(EnumFacing.byIndex(parent.side));
	}

	@Override
	public DuctUnit<?, ?, ?> getDuct() {
		return duct;
	}

	@Override
	public byte getSide() {
		return (byte) (parent.side ^ 1);
	}

	@Override
	public StackList<I> getRequestedStacks() {
		return requestInput.getOrDefault(null, supplier.get());
	}

	@Override
	public void onFail(Type<I> type, long amount) {
		// TODO
	}

}
