package dev.wren.typewriter.prov.carriages;

import dev.wren.typewriter.internal.TypewriterBase;
import dev.wren.typewriter.internal.waytoomanyinterfaces.IProvideData;
import dev.wren.typewriter.prov.TypewriterProvider;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.PackOutput;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.neoforged.fml.LogicalSide;
import net.neoforged.neoforge.common.data.LanguageProvider;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Collectors;


public class TypewriterLangProvider extends LanguageProvider implements IProvideData {

    private final TypewriterBase<?> owner;

    public TypewriterLangProvider(TypewriterBase<?> owner, PackOutput packOutput) {
        super(packOutput, owner.getModId(), "en_us");
        this.owner = owner;
    }

    @Override
    public LogicalSide getSide() {
        return LogicalSide.CLIENT;
    }

    @Override
    public String getName() {
        return "lang_en_us";
    }

    @Override
    protected void addTranslations() {
        owner.genData(TypewriterProvider.LANG, this);
    }

    public static String toEnglishName(String internalName) {
        return Arrays.stream(internalName.toLowerCase(Locale.ROOT).split("_"))
                .map(StringUtils::capitalize)
                .collect(Collectors.joining(" "));
    }

    @SuppressWarnings("unchecked")
    public <T> String getAutomaticName(Supplier<? extends T> sup, ResourceKey<? extends Registry<T>> registry) {
        return toEnglishName(((Registry<Registry<T>>) BuiltInRegistries.REGISTRY).get(registry.location()).getKey(sup.get()).getPath());
    }

    public void addBlock(Supplier<? extends Block> block) {
        addBlock(block, getAutomaticName(block, Registries.BLOCK));
    }

    public void addBlockWithTooltip(Supplier<? extends Block> block, String tooltip) {
        addBlock(block);
        addTooltip(block, tooltip);
    }

    public void addBlockWithTooltip(Supplier<? extends Block> block, String name, String tooltip) {
        addBlock(block, name);
        addTooltip(block, tooltip);
    }

    public void addItem(Supplier<? extends Item> item) {
        addItem(item, getAutomaticName(item, Registries.ITEM));
    }

    public void addItemWithTooltip(Supplier<? extends Item> item, String name, List<String> tooltips) {
        addItem(item, name);
        addTooltip(item, tooltips);
    }

    public void addTooltip(Supplier<? extends ItemLike> itemLike, String tooltip) {
        add(itemLike.get().asItem().getDescriptionId() + ".desc", tooltip);
    }

    public void addTooltip(Supplier<? extends ItemLike> itemLike, List<String> tooltips) {
        for (int i = 0; i < tooltips.size(); i++) {
            add(itemLike.get().asItem().getDescriptionId() + ".desc." + i, tooltips.get(i));
        }
    }

    public void add(CreativeModeTab tab, String name) {
        var contents = tab.getDisplayName().getContents();
        if (contents instanceof TranslatableContents lang) {
            add(lang.getKey(), name);
        } else {
            throw new IllegalArgumentException("Creative tab does not have a translatable name: " + tab.getDisplayName());
        }
    }

    public void addEntityType(Supplier<? extends EntityType<?>> entity) {
        addEntityType(entity, getAutomaticName(entity, Registries.ENTITY_TYPE));
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        return super.run(cache);
    }

}
