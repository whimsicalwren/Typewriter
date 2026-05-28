package dev.wren.typewriter.prov.carriages;

import dev.wren.typewriter.internal.TypewriterBase;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.registries.RegistryPatchGenerator;
import net.neoforged.fml.LogicalSide;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class TypewriterDatapackProvider extends DatapackBuiltinEntriesProvider implements LookupFillerProvider {

    public TypewriterDatapackProvider(TypewriterBase<?> parent, PackOutput output, CompletableFuture<HolderLookup.Provider> provider) {
        super(output, RegistryPatchGenerator.createLookup(provider, parent.getDataProviderInitializer().getDatapackRegistryProviders()), Set.of(parent.getModId()));
    }

    @Override
    public CompletableFuture<HolderLookup.Provider> getFilledProvider() {
        return getRegistryProvider();
    }

    @Override
    public LogicalSide getSide() {
        return LogicalSide.SERVER;
    }
}
