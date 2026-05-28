package dev.wren.typewriter.prov;

import dev.wren.typewriter.builder.Builder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Supplier;

/**
 * A context bean passed to data generator callbacks. Contains the entry that data is being created for, and some metadata about the entry.
 *
 * @param <R>
 *            Type of the registry to which the entry belongs
 * @param <E>
 *            Type of the object for which data is being generated
 */
public class DatagenContext<R, E extends R> implements Supplier<E> {

    Supplier<E> entry;
    String name;
    ResourceLocation id;

    public DatagenContext(Supplier<E> entry, String name, ResourceLocation id) {
        this.entry = entry;
        this.name = name;
        this.id = id;
    }

    @Deprecated
    public static <R, E extends R> DatagenContext<R, E> from(Builder<R, E, ?, ?> builder, ResourceKey<? extends Registry<R>> type) {
        return from(builder);
    }

    public static <R, E extends R> DatagenContext<R, E> from(Builder<R, E, ?, ?> builder) {
        return new DatagenContext<>(builder.getOwner().get(builder.getName(), builder.getRegistryKey()), builder.getName(),
                ResourceLocation.fromNamespaceAndPath(builder.getOwner().getModId(), builder.getName()));
    }

    @Override
    public E get() {
        return entry.get();
    }

    public String getName() {
        return name;
    }

    public ResourceLocation getId() {
        return id;
    }
}