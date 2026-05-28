package dev.wren.typewriter.prov.carriages;

import dev.wren.typewriter.internal.TypewriterBase;
import dev.wren.typewriter.internal.waytoomanyinterfaces.IProvideData;
import dev.wren.typewriter.prov.TypewriterProvider;
import net.minecraft.data.PackOutput;
import net.neoforged.fml.LogicalSide;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class TypewriterItemModelProvider extends ItemModelProvider implements IProvideData {

    private final TypewriterBase<?> parent;

    public TypewriterItemModelProvider(TypewriterBase<?> parent, PackOutput packOutput, ExistingFileHelper existingFileHelper) {
        super(packOutput, parent.getModId(), existingFileHelper);
        this.parent = parent;
    }

    @Override
    public LogicalSide getSide() {
        return LogicalSide.CLIENT;
    }

    @Override
    protected void registerModels() {
        parent.genData(TypewriterProvider.ITEM_MODEL, this);
    }

    @Override
    public String getName() {
        return "Item models";
    }

}
