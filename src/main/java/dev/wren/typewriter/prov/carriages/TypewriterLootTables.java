package dev.wren.typewriter.prov.carriages;

import dev.wren.typewriter.internal.TypewriterBase;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.WritableRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.data.loot.packs.VanillaBlockLoot;
import net.minecraft.data.loot.packs.VanillaEntityLoot;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.ValidationContext;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface TypewriterLootTables extends LootTableSubProvider {
    default void validate(WritableRegistry<LootTable> writableRegistry, ValidationContext validationCtx) {}

    class BlockTables extends VanillaBlockLoot implements TypewriterLootTables {
        private final TypewriterBase<?> parent;
        private final Consumer<BlockTables> callback;

        public BlockTables(HolderLookup.Provider provider, TypewriterBase<?> parent, Consumer<BlockTables> callback) {
            super(provider);
            this.parent = parent;
            this.callback = callback;
        }

        @Override
        protected void generate() {
            callback.accept(this);
        }

        @Override
        protected @NotNull Iterable<Block> getKnownBlocks() {
            return parent.getAll(Registries.BLOCK).stream().map(Supplier::get).collect(Collectors.toList());
        }
    }

    class EntityTables extends VanillaEntityLoot implements TypewriterLootTables {
        private final TypewriterBase<?> parent;
        private final Consumer<EntityTables> callback;

        public EntityTables(HolderLookup.Provider prov, TypewriterBase<?> parent, Consumer<EntityTables> callback) {
            super(prov);
            this.parent = parent;
            this.callback = callback;
        }

        @Override
        public void generate() {
            callback.accept(this);
        }

        @Override
        protected @NotNull Stream<EntityType<?>> getKnownEntityTypes() {
            return parent.getAll(Registries.ENTITY_TYPE).stream().map(Supplier::get);
        }
    }
}
