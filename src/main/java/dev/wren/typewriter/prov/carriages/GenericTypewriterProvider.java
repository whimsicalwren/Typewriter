package dev.wren.typewriter.prov.carriages;

import dev.wren.typewriter.internal.TypewriterBase;
import dev.wren.typewriter.internal.waytoomanyinterfaces.IProvideData;
import dev.wren.typewriter.prov.TypewriterProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.neoforged.fml.LogicalSide;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public class GenericTypewriterProvider implements IProvideData {
    private final TypewriterBase<?> parent;
    private final PackOutput output;
    private final LogicalSide side;
    private final CompletableFuture<HolderLookup.Provider> registries;
    private final ExistingFileHelper existingFileHelper;
    private final TypewriterProvider<GenericTypewriterProvider> prov;
    private final List<RunnableDataProvider> providers = new ArrayList<>();

    public GenericTypewriterProvider(TypewriterBase<?> parent, LogicalSide side, GatherDataEvent event, TypewriterProvider<GenericTypewriterProvider> prov) {
        this.parent = parent;
        this.prov = prov;
        this.side = side;

        output = event.getGenerator().getPackOutput();
        registries = event.getLookupProvider();
        existingFileHelper = event.getExistingFileHelper();
    }

    public GenericTypewriterProvider add(RunnableDataProvider generator) {
        providers.add(generator);
        return this;
    }

    @Override
    public @NotNull CompletableFuture<?> run(CachedOutput cachedOutput) {
        providers.clear();
        parent.genData(prov, this);

        return CompletableFuture.allOf(providers
                .stream()
                .map(prov -> prov.createProvider(output, registries, existingFileHelper))
                .map(provider -> provider.run(cachedOutput))
                .toArray(CompletableFuture[]::new)
        );
    }

    @Override
    public String getName() {
        return "generic_" + side.toString().toLowerCase(Locale.ROOT);
    }

    @Override
    public LogicalSide getSide() {
        return side;
    }

    public interface RunnableDataProvider  {
        DataProvider createProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries, ExistingFileHelper fileHelper);
    }

}