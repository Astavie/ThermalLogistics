package astavie.thermallogistics.util;

import cofh.core.util.helpers.ItemHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;

import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class Shared<T> implements Consumer<T>, Supplier<T>, Predicate<T> {

	private T t;

	public Shared() {
	}

	public Shared(T t) {
		this.t = t;
	}

	@Override
	public void accept(T t) {
		this.t = t;
	}

	@Override
	public T get() {
		return t;
	}

	@Override
	public boolean test(T t) {
		return t.equals(this.t);
	}

	public static class Item extends Shared<ItemStack> {

		public Item() {
		}

		public Item(ItemStack stack) {
			super(stack);
		}

		@Override
		public boolean test(ItemStack stack) {
			return (get().isEmpty() && stack.isEmpty()) || ItemHelper.itemsIdentical(get(), stack);
		}

		public Ingredient asIngredient() {
			return Ingredient.fromStacks(get());
		}

		public ItemStack getDisplayStack() {
			return get();
		}

	}

}
