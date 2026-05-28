package dev.wren.typewriter.spec;

import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class ShaderSpec implements Supplier<ShaderInstance> {
    private final ResourceLocation name;
    private final VertexFormat format;

    @Nullable
    private ShaderInstance instance;

    public ShaderSpec(ResourceLocation name, VertexFormat format) {
        this.name = name;
        this.format = format;
    }

    @Override
    @Nonnull
    public ShaderInstance get() {
        return Objects.requireNonNull(instance, () -> "Shader not yet registered: " + name);
    }

    public ResourceLocation getId() {
        return name;
    }

    public boolean isPresent() {
        return instance != null;
    }

    public Optional<ShaderInstance> asOptional() {
        return Optional.ofNullable(instance);
    }

    public Optional<ShaderSpec> filter(Predicate<ShaderInstance> predicate) {
        Objects.requireNonNull(predicate);
        if (isPresent() && predicate.test(get())) {
            return Optional.of(this);
        }
        return Optional.empty();
    }

    public boolean is(ShaderInstance other) {
        return isPresent() && get() == other;
    }

    void onRegister(RegisterShadersEvent event) throws IOException {
        event.registerShader(
                new ShaderInstance(event.getResourceProvider(), name, format),
                shader -> this.instance = shader
        );
    }
}