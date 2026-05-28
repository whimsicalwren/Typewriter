package dev.wren.typewriter.prov.carriages;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import dev.wren.typewriter.internal.TypewriterBase;
import dev.wren.typewriter.internal.waytoomanyinterfaces.IProvideData;
import dev.wren.typewriter.prov.TypewriterProvider;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.WritableRegistry;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.data.loot.packs.VanillaLootTableProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSet;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.fml.LogicalSide;
import org.apache.commons.lang3.function.TriFunction;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class TypewriterLootTableProvider extends LootTableProvider implements IProvideData {

    public interface LootType<T extends TypewriterLootTables> {

        LootType<TypewriterLootTables.BlockTables> BLOCK = register("block", LootContextParamSets.BLOCK, TypewriterLootTables.BlockTables::new);
        LootType<TypewriterLootTables.EntityTables> ENTITY = register("entity", LootContextParamSets.ENTITY, TypewriterLootTables.EntityTables::new);

        T getLootCreator(HolderLookup.Provider provider, TypewriterBase<?> parent, Consumer<T> callback);

        LootContextParamSet getLootSet();

        static <T extends TypewriterLootTables> LootType<T> register(String name, LootContextParamSet set, TriFunction<HolderLookup.Provider, TypewriterBase<?>, Consumer<T>, T> factory) {
            LootType<T> type = new LootType<>() {
                @Override
                public T getLootCreator(HolderLookup.Provider provider, TypewriterBase<?> parent, Consumer<T> callback) {
                    return factory.apply(provider, parent, callback);
                }

                @Override
                public LootContextParamSet getLootSet() {
                    return set;
                }
            };
            LOOT_TYPES.put(name, type);
            return type;
        }
    }

    private static final Map<String, LootType<?>> LOOT_TYPES = new HashMap<>();

    private final TypewriterBase<?> parent;

    private final Multimap<LootType<?>, Consumer<? super TypewriterLootTables>> specialLootActions = HashMultimap.create();
    private final Multimap<LootContextParamSet, Consumer<BiConsumer<ResourceKey<LootTable>, LootTable.Builder>>> lootActions = HashMultimap.create();
    private final Set<TypewriterLootTables> currentLootCreators = new HashSet<>();

    private CompletableFuture<HolderLookup.Provider> provider;

    public TypewriterLootTableProvider(TypewriterBase<?> parent, PackOutput packOutput, CompletableFuture<HolderLookup.Provider> provider) {
        super(packOutput, Set.of(), VanillaLootTableProvider.create(packOutput, provider).getTables(), provider);
        this.parent = parent;
        this.provider = provider;
    }

    public HolderLookup.Provider getProvider(){
        return provider.getNow(null);
    }

    public <T> Holder<T> resolve(ResourceKey<T> key) {
        return getProvider().lookupOrThrow(key.registryKey()).getOrThrow(key);
    }

    @Override
    public LogicalSide getSide() {
        return LogicalSide.SERVER;
    }

    @Override
    protected void validate(WritableRegistry<LootTable> writableRegistry, ValidationContext validationCtx, ProblemReporter.Collector prc) {
        currentLootCreators.forEach(c -> c.validate(writableRegistry, validationCtx));
    }

    @SuppressWarnings("unchecked")
    public <T extends TypewriterLootTables> void addLootAction(LootType<T> type, Consumer<T> action) {
        this.specialLootActions.put(type, (Consumer<TypewriterLootTables>) action);
    }

    public void addLootAction(LootContextParamSet set, Consumer<BiConsumer<ResourceKey<LootTable>, LootTable.Builder>> action) {
        this.lootActions.put(set, action);
    }

    private LootTableSubProvider getLootCreator(HolderLookup. Provider provider, TypewriterBase<?> parent, LootType<?> type) {
        TypewriterLootTables creator = type.getLootCreator(provider, parent, cons -> specialLootActions.get(type).forEach(c -> c.accept(cons)));
        currentLootCreators.add(creator);
        return creator;
    }

    private static final BiMap<ResourceLocation, LootContextParamSet> SET_REGISTRY = LootContextParamSets.REGISTRY; // access transformer my beloved

    @Override
    public List<SubProviderEntry> getTables() {
        parent.genData(TypewriterProvider.LOOT, this);
        currentLootCreators.clear();
        ImmutableList.Builder<SubProviderEntry> builder = ImmutableList.builder();
        for (LootType<?> type : LOOT_TYPES.values()) {
            builder.add(new SubProviderEntry(provider -> getLootCreator(provider, parent, type), type.getLootSet()));
        }
        for (LootContextParamSet set : SET_REGISTRY.values()) {
            builder.add(new SubProviderEntry((provider) -> callback -> lootActions.get(set).forEach(a -> a.accept(callback)), set));
        }
        return builder.build();
    }

}
