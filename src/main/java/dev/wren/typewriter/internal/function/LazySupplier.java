package dev.wren.typewriter.internal.function;

import cpw.mods.util.Lazy;

import java.util.function.Supplier;

@FunctionalInterface
public interface LazySupplier<T> extends Supplier<T> {

    @Override
    T get();

    static <T> LazySupplier<T> of(T value) {
        return of(() -> value);
    }

    static <T> LazySupplier<T> of(Supplier<T> supp) {
        return of(Lazy.of(supp));
    }

    static <T> LazySupplier<T> of(Lazy<T> lazy) {
        return lazy::get;
    }
}
