package dev.wren.typewriter.prov;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import dev.wren.typewriter.internal.TypewriterBase;
import dev.wren.typewriter.internal.waytoomanyinterfaces.IProvideData;
import dev.wren.typewriter.prov.carriages.LookupFillerProvider;
import dev.wren.typewriter.prov.carriages.TypewriterTagProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.resources.ResourceKey;
import net.neoforged.fml.LogicalSide;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import static dev.wren.typewriter.internal.TypewriterBase.log;

public class TypewriterDataProvider implements DataProvider {

    static final BiMap<String, TypewriterProvider<?>> TYPES = HashBiMap.create();

    static final Map<ResourceKey<? extends Registry<?>>, TypewriterProvider<?>> TAG_TYPES = new ConcurrentHashMap<>();

    public static @Nullable String getTypeName(TypewriterProvider<?> type) {
        return TYPES.inverse().get(type);
    }

    private final String modId;
    private final Map<TypewriterProvider<?>, IProvideData> subProviders = new LinkedHashMap<>();
    private final CompletableFuture<HolderLookup.Provider> registriesLookup;


    public TypewriterDataProvider(TypewriterBase<?> parent, String modId, GatherDataEvent event) {
        this.modId = modId;
        this.registriesLookup = event.getLookupProvider();

        EnumSet<LogicalSide> sides = EnumSet.noneOf(LogicalSide.class);
        if (event.includeServer()) {
            sides.add(LogicalSide.SERVER);
        }
        if (event.includeClient()) {
            sides.add(LogicalSide.CLIENT);
        }

        log.debug("Gathering providers for sides: {}", sides);
        Map<TypewriterProvider<?>, IProvideData> known = new HashMap<>();
        for (DataProviderInitializer.Sorted sorted :parent.getDataProviderInitializer().getSortedProviders()) {
            TypewriterProvider<?> type = sorted.type();
            var lookup = registriesLookup;
            if (sorted.dependency() != null) lookup = ((LookupFillerProvider) known.get(sorted.dependency())).getFilledProvider();
            IProvideData prov = TypewriterProvider.create(type, parent, event, known, lookup);
            if (prov instanceof TypewriterTagProvider<?> tagsProvider && TAG_TYPES.get(tagsProvider.registry()) != type) {
                throw new IllegalStateException("Tag providers must be registered through ProviderType::registerTag");
            }
            known.put(type, prov);
            if (sides.contains(prov.getSide())) {
                log.debug("Adding provider for type: {}", sorted.id());
                subProviders.put(type, prov);
            }
        }
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        return registriesLookup.thenCompose(provider -> {
            ArrayList<CompletableFuture<?>> list = new ArrayList<>();

            for (Map.Entry<TypewriterProvider<?>, IProvideData> e : subProviders.entrySet()) {
                log.debug("Generating data for type: {}", getTypeName(e.getKey()));
                list.add(e.getValue().run(cache));
            };

            return CompletableFuture.allOf(list.toArray(CompletableFuture[]::new));
        });
    }

    @Override
    public String getName() {
        return modId + "typewriter_provider";
    }

    @SuppressWarnings("unchecked")
    public <P extends IProvideData> Optional<P> getSubProvider(TypewriterProvider<P> type) {
        return Optional.ofNullable((P) subProviders.get(type));
    }
}
