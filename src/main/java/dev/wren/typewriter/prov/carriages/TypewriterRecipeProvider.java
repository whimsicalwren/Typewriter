package dev.wren.typewriter.prov.carriages;

import dev.wren.typewriter.internal.TypewriterBase;
import dev.wren.typewriter.internal.waytoomanyinterfaces.IProvideData;
import dev.wren.typewriter.prov.TypewriterProvider;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.neoforged.fml.LogicalSide;
import net.neoforged.neoforge.common.conditions.ICondition;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class TypewriterRecipeProvider extends RecipeProvider implements IProvideData, RecipeOutput {

    private final TypewriterBase<?> owner;
    @Nullable
    private HolderLookup.Provider provider = null;

    @Nullable
    private RecipeOutput output;


    public TypewriterRecipeProvider(TypewriterBase<?> owner, PackOutput output, CompletableFuture<HolderLookup.Provider> provider) {
        super(output, provider);
        this.owner = owner;
    }

    @Override
    public LogicalSide getSide() {
        return LogicalSide.SERVER;
    }

    @Override
    protected void buildRecipes(RecipeOutput recipeOutput) {
        this.output = recipeOutput;
        owner.genData(TypewriterProvider.RECIPE, this);
        this.output = null;
    }

    @Override
    public Advancement.Builder advancement() {
        if (output == null) {
            throw new IllegalStateException("Cannot get advancement outside of a call to registerRecipes");
        }
        return output.advancement();
    }

    @Override
    protected CompletableFuture<?> run(CachedOutput output, HolderLookup.Provider registries) {
        this.provider = registries;
        return super.run(output, registries);
    }

    @Override
    public void accept(ResourceLocation id, Recipe<?> recipe, @Nullable AdvancementHolder advancement, ICondition... conditions) {
        if (output == null) {
            throw new IllegalStateException("Cannot accept recipes outside of a call to registerRecipes");
        }
        output.accept(id, recipe, advancement, conditions);
    }
}
