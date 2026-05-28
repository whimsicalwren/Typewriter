package dev.wren.typewriter.prov;

import dev.wren.typewriter.internal.TypewriterBase;
import dev.wren.typewriter.internal.waytoomanyinterfaces.IProvideData;
import dev.wren.typewriter.prov.carriages.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.fml.LogicalSide;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

@FunctionalInterface
@SuppressWarnings("deprecation")
public interface TypewriterProvider<P extends IProvideData> {

    // server
    TypewriterProvider<TypewriterDataMapProvider> DATAMAP = registerServer("datamap", TypewriterDataMapProvider::new);
    TypewriterProvider<TypewriterDatapackProvider> DATAPACK = registerServer("datapack", TypewriterDatapackProvider::new);
    TypewriterProvider<TypewriterRecipeProvider> RECIPE = registerServer("recipe", TypewriterRecipeProvider::new);
    TypewriterProvider<TypewriterAdvancementProvider> ADVANCEMENT = registerServer("advancement", TypewriterAdvancementProvider::new);
    TypewriterProvider<TypewriterLootTableProvider> LOOT = registerServer("loot", TypewriterLootTableProvider::new);
    //tags
    TypewriterProvider<TypewriterTagProvider.Intrinsic<Block>> BLOCK_TAG = registerTagIntrinsic("tags/block", "blocks", Registries.BLOCK, b -> b.builtInRegistryHolder().key());
    TypewriterProvider<TypewriterTagProvider.ItemTagProvider> ITEM_TAG = registerTag("tags/item", Registries.ITEM, ctx -> new TypewriterTagProvider.ItemTagProvider(ctx.parent, ctx.type, "items", ctx.output, ctx.provider, ctx.get(BLOCK_TAG).contentsGetter(), ctx.fileHelper));
    TypewriterProvider<TypewriterTagProvider.Intrinsic<Fluid>> FLUID_TAG = registerTagIntrinsic("tags/fluid", "fluids", Registries.FLUID, f -> f.builtInRegistryHolder().key());
    TypewriterProvider<TypewriterTagProvider.Intrinsic<EntityType<?>>> ENTITY_TAG = registerTagIntrinsic("tags/entity", "entity_types", Registries.ENTITY_TYPE, et -> et.builtInRegistryHolder().key());
    TypewriterProvider<TypewriterTagProvider.Impl<Enchantment>> ENCHANTMENT_TAG = registerTag("tags/enchantment", "enchantments", Registries.ENCHANTMENT);

    // client
    TypewriterProvider<TypewriterLangProvider> LANG = register("lang", context -> new TypewriterLangProvider(context.parent, context.output));
    TypewriterProvider<TypewriterBlockstateProvider> BLOCKSTATE = register("blockstate", context -> new TypewriterBlockstateProvider(context.parent, context.output, context.fileHelper));
    TypewriterProvider<TypewriterItemModelProvider> ITEM_MODEL = register("item_model", context -> new TypewriterItemModelProvider(context.parent, context.output, context.fileHelper));

    // generic data
    TypewriterProvider<GenericTypewriterProvider> CLIENT_GENERIC = register("client_generic", ctx -> new GenericTypewriterProvider(ctx.parent, LogicalSide.CLIENT, ctx.event, ctx.type));
    TypewriterProvider<GenericTypewriterProvider> SERVER_GENERIC = register("server_generic", ctx -> new GenericTypewriterProvider(ctx.parent, LogicalSide.SERVER, ctx.event, ctx.type));

    P create(Context<P> context);

    record Context<T extends IProvideData>(TypewriterProvider<T> type, TypewriterBase<?> parent, GatherDataEvent event,
                                           Map<TypewriterProvider<?>, IProvideData> existing,
                                           PackOutput output, ExistingFileHelper fileHelper,
                                           CompletableFuture<HolderLookup.Provider> provider) {

        @SuppressWarnings("unchecked")
        public <R extends IProvideData> R get(TypewriterProvider<R> other) {
            return (R) existing().get(other);
        }

    }

    interface ServerDataFactory<T extends IProvideData> extends TypewriterProvider<T> {
        T create(TypewriterBase<?> parent, PackOutput output, CompletableFuture<HolderLookup.Provider> provider);

        @Override
        default T create(Context<T> context) {
            return create(context.parent(), context.output(), context.provider());
        }
    }


    @Nonnull
    static <T extends IProvideData> TypewriterProvider<T> registerServer(String name, ServerDataFactory<T> factory) {
        return register(name, factory);
    }

    @Nonnull
    static <T extends IProvideData> TypewriterProvider<T> register(String name, TypewriterProvider<T> type) {
        TypewriterDataProvider.TYPES.put(name, type);
        return type;
    }


    @Nonnull
    @SuppressWarnings("unchecked")
    static <T, R extends TypewriterTagProvider<T>> TypewriterProvider<R> registerTag(String name, ResourceKey<? extends Registry<T>> key, TypewriterProvider<R> type) {
        if (TypewriterDataProvider.TAG_TYPES.containsKey(key)) {
            return (TypewriterProvider<R>) TypewriterDataProvider.TAG_TYPES.get(key);
        }
        TypewriterDataProvider.TAG_TYPES.put(key, type);
        TypewriterDataProvider.TYPES.put(name, type);
        return type;
    }

    @Nonnull
    static <T> TypewriterProvider<TypewriterTagProvider.Intrinsic<T>> registerTagIntrinsic(String providerName, String typeName, ResourceKey<? extends Registry<T>> registry, Function<T, ResourceKey<T>> keyExtractor) {
        return registerTag(providerName, registry, c -> new TypewriterTagProvider.Intrinsic<>(c.parent(), c.type(), typeName, c.output(), registry, c.provider(), keyExtractor, c.fileHelper()));
    }

    @Nonnull
    static <T> TypewriterProvider<TypewriterTagProvider.Impl<T>> registerTag(String providerName, String typeName, ResourceKey<Registry<T>> registry) {
        return registerTag(providerName, registry, c -> new TypewriterTagProvider.Impl<>(c.parent(), c.type(), typeName, c.output(), registry, c.provider(), c.fileHelper()));
    }

    static <T extends IProvideData> T create(TypewriterProvider<T> type, TypewriterBase<?> parent, GatherDataEvent event, Map<TypewriterProvider<?>, IProvideData> existing, CompletableFuture<HolderLookup.Provider> provider) {
        return type.create(new Context<>(type, parent, event, existing, event.getGenerator().getPackOutput(), event.getExistingFileHelper(), provider));
    }

}
