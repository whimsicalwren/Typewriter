package dev.wren.typewriter.spec;

import dev.wren.typewriter.internal.TypewriterBase;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class Specification<R, A extends R> extends DeferredHolder<R, A> implements Supplier<A> {

    private final TypewriterBase<?> owner;

    public Specification(TypewriterBase<?> owner, DeferredHolder<R, A> key) {
        super(key.getKey());

        if (owner == null)
            throw new NullPointerException("Owner must not be null");
        this.owner = owner;
    }

    // R2D2
    public <R2, A2 extends R2> Specification<R2, A2> getSibling(ResourceKey<? extends Registry<R2>> registryType) {
        return owner.get(getId().getPath(), registryType);
    }

    public <R2, A2 extends R2> Specification<R2, A2> getSibling(Registry<R2> registry) {
        return getSibling(registry.key());
    }

    /**
     * If an entry is present, and the entry matches the given predicate, return an {@link Optional<Specification>} describing the value, otherwise return an empty {@link Optional}.
     *
     * @param predicate
     *            a {@link Predicate predicate} to apply to the entry, if present
     * @return an {@link Specification} describing the value of this {@link Specification} if the entry is present and matches the given predicate, otherwise an empty {@link Specification}
     * @throws NullPointerException
     *             if the predicate is null
     */
    public Optional<Specification<R, A>> filter(Predicate<R> predicate) {
        Objects.requireNonNull(predicate);
        if (predicate.test(get())) {
            return Optional.of(this);
        }
        return Optional.empty();
    }

    public <S> boolean is(S spec) {
        return get() == spec;
    }

    @SuppressWarnings("unchecked")
    protected static <S extends Specification<?, ?>> S cast(Class<? super S> clazz, Specification<?, ?> entry) {
        if (clazz.isInstance(entry)) {
            return (S) entry;
        }
        throw new IllegalArgumentException("Could not convert Specification: expecting " + clazz + ", found " + entry.getClass());
    }

}
