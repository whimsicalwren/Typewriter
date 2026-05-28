package dev.wren.typewriter.builder;

import dev.wren.typewriter.internal.Callback;
import dev.wren.typewriter.internal.TypewriterBase;
import dev.wren.typewriter.internal.waytoomanyinterfaces.IBuilder;
import dev.wren.typewriter.internal.waytoomanyinterfaces.IParent;
import dev.wren.typewriter.internal.waytoomanyinterfaces.ITransformable;
import dev.wren.typewriter.spec.Specification;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.MinecartItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CryingObsidianBlock;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.function.Consumer;

/**
 * Base abstract builder class.
 * @param <R> The base type of this builder's result, like {@link Block} or {@link Item}
 * @param <A> The actual type of this builder's result, like {@link CryingObsidianBlock} or {@link MinecartItem}
 * @param <P> the parent of this builder.
 * @param <SELF> Self type, for extending.
 */
public abstract class Builder<R, A extends R, P extends IParent<R>, SELF extends Builder<R, A, P, SELF>> implements ITransformable<SELF>, IBuilder<R> {

    private final TypewriterBase<?> owner;
    private final P parent;
    private final String name;
    private final Callback callback;
    private final ResourceKey<? extends Registry<R>> registryKey;

    // getters
    public ResourceKey<? extends Registry<R>> getRegistryKey() { return registryKey; }
    public String getName() { return name; }
    public TypewriterBase<?> getOwner() { return owner; }
    public P getParent() { return parent; }

    public Builder(TypewriterBase<?> owner, P parent, String name, Callback callback, ResourceKey<? extends Registry<R>> registryKey) {
        this.owner = owner;
        this.parent = parent;
        this.name = name;
        this.callback = callback;
        this.registryKey = registryKey;
    }

    public Specification<R, A> get() {
        return getOwner().get(getName(), getRegistryKey());
    }

    protected abstract A createEntry();

    public Specification<R, A> register() {
        return callback.accept(name, registryKey, this, this::createEntry, this::createEntryWrapper);
    }

    protected Specification<R, A> createEntryWrapper(DeferredHolder<R, A> delegate) {
        return new Specification<>(getOwner(), delegate);
    }


    public SELF onRegister(Consumer<A> callback) {
        getOwner().addRegisterCallback(getName(), getRegistryKey(), callback);
        return self();
    }

    public <D> SELF onRegister(ResourceKey<? extends Registry<D>> dep, Consumer<A> callback) {
        return onRegister(a -> {
            if (getOwner().isRegistered(dep)) callback.accept(a); // if already registered, run the callback immediately
            else getOwner().addAfterRegisterCallback(dep, () -> callback.accept(a));
        });
    }

    public P build() {
        register();
        return getParent();
    }
}
