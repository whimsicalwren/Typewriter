package dev.wren.typewriter.prov.carriages;

import dev.wren.typewriter.internal.TypewriterBase;
import dev.wren.typewriter.prov.TypewriterProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.IntrinsicHolderTagsProvider;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagBuilder;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.fml.LogicalSide;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public interface TypewriterTagProvider<T> extends LookupFillerProvider {

    TagsProvider.TagAppender<T> addTag(TagKey<T> tag);

    CompletableFuture<TagsProvider.TagLookup<T>> contentsGetter();

    ResourceKey<? extends Registry<T>> registry();

    class Impl<T> extends TagsProvider<T> implements TypewriterTagProvider<T> {
        private final TypewriterBase<?> owner;
        private final TypewriterProvider<? extends Impl<T>> type;
        private final String name;

        public Impl(TypewriterBase<?> owner, TypewriterProvider<? extends Impl<T>> type, String name, PackOutput packOutput, ResourceKey<? extends Registry<T>> registryIn, CompletableFuture<HolderLookup.Provider> registriesLookup, ExistingFileHelper existingFileHelper) {
            super(packOutput, registryIn, registriesLookup, owner.getModId(), existingFileHelper);

            this.owner = owner;
            this.type = type;
            this.name = name;
        }

        @Override
        public String getName() {
            return "Tags (%s)".formatted(name);
        }

        @Override
        protected void addTags(HolderLookup.Provider provider) {
            owner.genData(type, this);
        }

        @Override
        public LogicalSide getSide() {
            return LogicalSide.SERVER;
        }

        @Override
        public TagAppender<T> addTag(TagKey<T> tag) {
            return super.tag(tag);
        }

        @Override
        public CompletableFuture<HolderLookup.Provider> getFilledProvider() {
            return createContentsProvider();
        }

        @Override
        public ResourceKey<? extends Registry<T>> registry() {
            return registryKey;
        }

    }

    class Intrinsic<T> extends IntrinsicHolderTagsProvider<T> implements TypewriterTagProvider<T> {
        private final TypewriterBase<?> owner;
        private final TypewriterProvider<? extends Intrinsic<T>> type;
        private final String name;

        public Intrinsic(TypewriterBase<?> owner, TypewriterProvider<? extends Intrinsic<T>> type, String name, PackOutput packOutput, ResourceKey<? extends Registry<T>> registryIn, CompletableFuture<HolderLookup.Provider> registriesLookup, Function<T, ResourceKey<T>> keyExtractor, ExistingFileHelper existingFileHelper) {
            super(packOutput, registryIn, registriesLookup, keyExtractor, owner.getModId(), existingFileHelper);

            this.owner = owner;
            this.type = type;
            this.name = name;
        }

        @Override
        public String getName() {
            return "Tags (%s)".formatted(name);
        }

        @Override
        protected void addTags(HolderLookup.Provider provider) {
            owner.genData(type, this);
        }

        @Override
        public LogicalSide getSide() {
            return LogicalSide.SERVER;
        }

        @Override
        public IntrinsicTagAppender<T> addTag(TagKey<T> tag) {
            return super.tag(tag);
        }

        @Override
        public CompletableFuture<HolderLookup.Provider> getFilledProvider() {
            return createContentsProvider();
        }

        @Override
        public ResourceKey<? extends Registry<T>> registry() {
            return registryKey;
        }

    }

    class ItemTagProvider extends Intrinsic<Item> {

        private final CompletableFuture<TagLookup<Block>> blockTags;
        private final Map<TagKey<Block>, TagKey<Item>> tagsToCopy = new HashMap<>();

        public ItemTagProvider(TypewriterBase<?> owner, TypewriterProvider<ItemTagProvider> type, String name, PackOutput output, CompletableFuture<HolderLookup.Provider> registriesLookup, CompletableFuture<TagLookup<Block>> blockTags, ExistingFileHelper existingFileHelper) {
            super(owner, type, name, output, Registries.ITEM, registriesLookup, item -> item.builtInRegistryHolder().key(), existingFileHelper);
            this.blockTags = blockTags;
        }

        public void copy(TagKey<Block> p_206422_, TagKey<Item> p_206423_) {
            this.tagsToCopy.put(p_206422_, p_206423_);
        }

        @Override
        protected CompletableFuture<HolderLookup.Provider> createContentsProvider() {
            return super.createContentsProvider().thenCombineAsync(this.blockTags, (p_274766_, p_274767_) -> {
                this.tagsToCopy.forEach((p_274763_, p_274764_) -> {
                    TagBuilder tagbuilder = this.getOrCreateRawBuilder(p_274764_);
                    Optional<TagBuilder> optional = p_274767_.apply(p_274763_);
                    optional.orElseThrow(() -> new IllegalStateException("Missing block tag " + p_274764_.location())).build().forEach(tagbuilder::add);
                });
                return p_274766_;
            });
        }
    }

}
