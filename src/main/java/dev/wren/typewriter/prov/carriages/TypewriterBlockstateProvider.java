package dev.wren.typewriter.prov.carriages;

import dev.wren.typewriter.internal.TypewriterBase;
import dev.wren.typewriter.internal.waytoomanyinterfaces.IProvideData;
import dev.wren.typewriter.prov.TypewriterProvider;
import net.minecraft.data.PackOutput;
import net.neoforged.fml.LogicalSide;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class TypewriterBlockstateProvider extends BlockStateProvider implements IProvideData {
    private final TypewriterBase<?> parent;

    public TypewriterBlockstateProvider(TypewriterBase<?> parent, PackOutput packOutput, ExistingFileHelper exFileHelper) {
        super(packOutput, parent.getModId(), exFileHelper);
        this.parent = parent;
    }

    @Override
    public LogicalSide getSide() {
        return LogicalSide.CLIENT;
    }

    @Override
    protected void registerStatesAndModels() {
        parent.genData(TypewriterProvider.BLOCKSTATE, this);
    }

    @Override
    public String getName() {
        return parent.getModId() + "_typewriter_blockstate";
    }
}
