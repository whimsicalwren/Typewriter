package dev.wren.typewriter.prov.carriages;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.wren.typewriter.internal.TypewriterBase;
import dev.wren.typewriter.internal.waytoomanyinterfaces.IProvideData;
import dev.wren.typewriter.prov.TypewriterProvider;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.LogicalSide;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.common.conditions.WithConditions;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class TypewriterAdvancementProvider implements IProvideData, Consumer<AdvancementHolder> {

    private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().create();

    private final TypewriterBase<?> owner;
    private final PackOutput packOutput;
    private final CompletableFuture<HolderLookup.Provider> registriesLookup;
    private final List<CompletableFuture<?>> advancementsToSave = Lists.newArrayList();

    private HolderLookup.Provider provider;

    public HolderLookup.Provider getProvider() {
        return provider;
    }

    public TypewriterAdvancementProvider(TypewriterBase<?> owner, PackOutput packOutputIn, CompletableFuture<HolderLookup.Provider> registriesLookupIn) {
        this.owner = owner;
        this.packOutput = packOutputIn;
        this.registriesLookup = registriesLookupIn;
    }

    public <T> Holder<T> resolve(ResourceKey<T> key) {
        return provider.lookupOrThrow(key.registryKey()).getOrThrow(key);
    }

    @Override
    public LogicalSide getSide() {
        return LogicalSide.SERVER;
    }

    public MutableComponent title(String category, String name, String title) {
        return owner.addLang("advancements", ResourceLocation.fromNamespaceAndPath(category, name), "title", title);
    }

    public MutableComponent desc(String category, String name, String desc) {
        return owner.addLang("advancements", ResourceLocation.fromNamespaceAndPath(category, name), "description", desc);
    }

    private @Nullable CachedOutput cachedOutput;
    private Set<ResourceLocation> seenAdvancements = new HashSet<>();

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        return registriesLookup.thenCompose(lookup -> {
            this.provider = lookup;
            advancementsToSave.clear();

            try {
                this.cachedOutput = cache;
                this.seenAdvancements.clear();
                owner.genData(TypewriterProvider.ADVANCEMENT, this);
            } finally {
                this.cachedOutput = null;
            }

            return CompletableFuture.allOf(advancementsToSave.toArray(CompletableFuture[]::new));
        });
    }

    @Override
    public void accept(@Nullable AdvancementHolder holder) {
        withConditions(holder, List.of());
    }

    public void withConditions(@Nullable AdvancementHolder holder, List<ICondition> conditions) {
        this.registriesLookup.thenAccept(lookup -> {
            CachedOutput cached = this.cachedOutput;
            if (cached == null) {
                throw new IllegalStateException("Cannot accept advancements outside of act");
            }
            Objects.requireNonNull(holder, "Cannot accept a null advancement");
            Path path = getPath(this.packOutput.getOutputFolder(), holder);
            if (!seenAdvancements.add(holder.id())) {
                throw new IllegalStateException("Duplicate advancement " + holder.id());
            } else if (conditions.isEmpty()) {
                advancementsToSave.add(DataProvider.saveStable(cached, lookup, Advancement.CODEC, holder.value(), path));
            } else {
                advancementsToSave.add(DataProvider.saveStable(cached, lookup, Advancement.CONDITIONAL_CODEC,
                        Optional.of(new WithConditions<>(conditions, holder.value())), path));
            }
        });
    }

    private static Path getPath(Path pathIn, AdvancementHolder holder) {
        return pathIn.resolve("data/" + holder.id().getNamespace() + "/advancement/" + holder.id().getPath() + ".json");
    }

    public String getName() {
        return "Advancements";
    }

}
