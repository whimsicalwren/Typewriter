package dev.wren.typewriter.prov;

import dev.wren.typewriter.prov.carriages.LookupFillerProvider;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.resources.ResourceKey;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DataProviderInitializer {

    private final RegistrySetBuilder datapackEntryProvider = new RegistrySetBuilder();

    private final Map<TypewriterProvider<?>, TypewriterProvider<? extends LookupFillerProvider>> dependencies = new ConcurrentHashMap<>();

    public DataProviderInitializer() {
        addDependency(TypewriterProvider.ITEM_TAG, TypewriterProvider.BLOCK_TAG);
    }

    public RegistrySetBuilder getDatapackRegistryProviders() {
        return datapackEntryProvider;
    }

    protected List<Sorted> getSortedProviders() {
        List<Sorted> sorted = new ArrayList<>();
        Set<TypewriterProvider<?>> added = new HashSet<>();
        List<Map.Entry<String, TypewriterProvider<?>>> remaining = new ArrayList<>(TypewriterDataProvider.TYPES.entrySet());
        while (!remaining.isEmpty()) {
            if (!remaining.removeIf(e -> {
                TypewriterProvider<?> type = e.getValue();
                TypewriterProvider<? extends LookupFillerProvider> dep = dependencies.get(type);
                if (dep == null || added.contains(dep)) { // no dep, or dep is already added
                    sorted.add(new Sorted(e.getKey(), type, dep));
                    added.add(type);
                    return true;
                }
                return false; // throw
            })) throw new IllegalStateException("Looping dependency detected: " + remaining);
        }
        return sorted;
    }

    public <T> void add(ResourceKey<Registry<T>> registry, RegistrySetBuilder.RegistryBootstrap<T> provider) {
        datapackEntryProvider.add(registry, provider);
    }

    public void addDependency(TypewriterProvider<?> dependent, TypewriterProvider<? extends LookupFillerProvider> dependency) {
        TypewriterProvider<? extends LookupFillerProvider> old = dependencies.put(dependent, dependency);
        if (old != null) throw new IllegalStateException("Providers can have only 1 dependency");
    }

    public record Sorted(
            String id, TypewriterProvider<?> type,
            @Nullable TypewriterProvider<? extends LookupFillerProvider> dependency
    ) {
    }

}