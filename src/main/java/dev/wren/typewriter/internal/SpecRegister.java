package dev.wren.typewriter.internal;


import com.google.common.base.Preconditions;
import dev.wren.typewriter.internal.function.LazySupplier;
import dev.wren.typewriter.spec.Specification;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@ApiStatus.Internal
public class SpecRegister<R, A extends R> {
    ResourceLocation name;
    ResourceKey<? extends Registry<R>> type;
    LazySupplier<? extends A> creator;
    Specification<R, A> specification;

    List<Consumer<? super A>> callbacks = new ArrayList<>();

    public ResourceLocation getName() { return name; }


    SpecRegister(ResourceLocation name, ResourceKey<? extends Registry<R>> type, Supplier<? extends A> creator, Function<DeferredHolder<R, A>, ? extends Specification<R, A>> entryFactory) {
        this.name = name;
        this.type = type;
        this.creator = LazySupplier.of(creator);
        this.specification = entryFactory.apply(DeferredHolder.create(type, name));
    }

    public Specification<R, A> get() {
        return specification;
    }

    public void register(RegisterEvent event) {
        A entry = creator.get();
        event.register(type, rh -> rh.register(name, entry));
        callbacks.forEach(c -> c.accept(entry));
        callbacks.clear();
    }

    public void addRegisterCallback(Consumer<? super A> callback) {
        Preconditions.checkNotNull(callback, "Callback must not be null");
        callbacks.add(callback);
    }
}