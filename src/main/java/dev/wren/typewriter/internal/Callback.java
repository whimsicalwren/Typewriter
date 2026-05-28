package dev.wren.typewriter.internal;

import dev.wren.typewriter.builder.Builder;
import dev.wren.typewriter.spec.Specification;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.function.Function;
import java.util.function.Supplier;

public interface Callback {

    <R, T extends R> Specification<R, T> accept(String name, ResourceKey<? extends Registry<R>> type, Builder<R, T, ?, ?> builder, Supplier<? extends T> factory, Function<DeferredHolder<R, T>, ? extends Specification<R, T>> entryFactory);

    default <R, T extends R> Specification<R, T> accept(String name, ResourceKey<? extends Registry<R>> type, Builder<R, T, ?, ?> builder, Supplier<? extends T> factory) {
        return accept(name, type, builder, factory, key -> new Specification<>(builder.getOwner(), key));
    }

}
