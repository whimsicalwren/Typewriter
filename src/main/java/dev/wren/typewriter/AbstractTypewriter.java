package dev.wren.typewriter;

import com.mojang.blaze3d.vertex.VertexFormat;
import dev.wren.typewriter.internal.TypewriterBase;
import dev.wren.typewriter.internal.waytoomanyinterfaces.ITransformable;
import dev.wren.typewriter.spec.ShaderSpec;
import net.minecraft.resources.ResourceLocation;

public abstract class AbstractTypewriter<SELF extends AbstractTypewriter<SELF>> extends TypewriterBase<SELF> implements ITransformable<SELF> {


    public AbstractTypewriter(String modId) {
        super(modId);
    }

    public ShaderSpec shader(String name, VertexFormat format) {
        return new ShaderSpec(ResourceLocation.fromNamespaceAndPath(modId, name), format);
    }

    public ShaderSpec shader(VertexFormat format) {
        return new ShaderSpec(ResourceLocation.fromNamespaceAndPath(modId, currentName), format);
    }

}
