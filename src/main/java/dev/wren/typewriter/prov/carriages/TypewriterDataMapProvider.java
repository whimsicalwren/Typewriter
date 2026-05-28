package dev.wren.typewriter.prov.carriages;

import dev.wren.typewriter.internal.TypewriterBase;
import dev.wren.typewriter.internal.waytoomanyinterfaces.IProvideData;
import dev.wren.typewriter.prov.TypewriterProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.neoforged.fml.LogicalSide;
import net.neoforged.neoforge.common.data.DataMapProvider;

import java.util.concurrent.CompletableFuture;

public class TypewriterDataMapProvider extends DataMapProvider implements IProvideData {

    private final TypewriterBase<?> parent;

    private HolderLookup.Provider provider;

    public TypewriterDataMapProvider(TypewriterBase<?> parent, PackOutput output, CompletableFuture<HolderLookup.Provider> prov) {
        super(output, prov);
        this.parent = parent;
    }

    @Override
    public LogicalSide getSide() {
        return LogicalSide.SERVER;
    }

    /**
     * Generate data map entries.
     *
     * @param provider
     */
    @Override
    protected void gather(HolderLookup.Provider provider) {
        this.provider = provider;
        parent.genData(TypewriterProvider.DATAMAP, this);
        this.provider = null;
    }

    public HolderLookup.Provider getProvider() {
        if (provider == null) throw new IllegalStateException("Holder Lookup Provider is not available now");
        return provider;
    }

}
