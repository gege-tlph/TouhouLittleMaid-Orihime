package cn.sh1rocu.touhoulittlemaid.util.recipe;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.criterion.RecipeUnlockedTrigger;
import net.minecraft.core.HolderSet;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class ItemStackShapelessRecipeBuilder implements RecipeBuilder {
    private final RecipeCategory category;
    private final ItemStack resultStack;
    private final NonNullList<Ingredient> ingredients = NonNullList.create();
    private final Map<String, Criterion<?>> criteria = new LinkedHashMap<>();
    @Nullable
    private String group;

    public ItemStackShapelessRecipeBuilder(RecipeCategory category, ItemStack resultStack) {
        this.category = category;
        this.resultStack = resultStack;
    }

    public static ItemStackShapelessRecipeBuilder shapeless(RecipeCategory category, ItemStack resultStack) {
        return new ItemStackShapelessRecipeBuilder(category, resultStack);
    }

    public ItemStackShapelessRecipeBuilder requires(TagKey<Item> tag) {
        // SWEEP VQ-6：origin 无条件 requires(Ingredient.of(tag))；此前 ifPresent 会在 tag 缺失时
        // 静默丢一个材料（配方悄悄变便宜）。改 getOrThrow = 缺 tag 时 datagen 立即失败（fail-fast，
        // 语义不弱于 origin）。当前唯一调用方为冻结的 Patchouli 配方（注释态）。
        Registry<Item> registry = BuiltInRegistries.ITEM;
        this.requires(Ingredient.of(registry.getOrThrow(tag)));
        return this;
    }

    public ItemStackShapelessRecipeBuilder requires(ItemLike item) {
        return this.requires(item, 1);
    }

    public ItemStackShapelessRecipeBuilder requires(ItemLike item, int quantity) {
        for (int i = 0; i < quantity; ++i) {
            this.requires(Ingredient.of(item));
        }

        return this;
    }

    public ItemStackShapelessRecipeBuilder requires(Ingredient ingredient) {
        return this.requires(ingredient, 1);
    }

    public ItemStackShapelessRecipeBuilder requires(Ingredient ingredient, int quantity) {
        for (int i = 0; i < quantity; ++i) {
            this.ingredients.add(ingredient);
        }

        return this;
    }

    public @NotNull ItemStackShapelessRecipeBuilder unlockedBy(@NotNull String name, @NotNull Criterion<?> criterion) {
        this.criteria.put(name, criterion);
        return this;
    }

    public @NotNull ItemStackShapelessRecipeBuilder group(@Nullable String groupName) {
        this.group = groupName;
        return this;
    }

    @Override
    public @NotNull Item getResult() {
        return resultStack.getItem();
    }

    @Override
    public void save(RecipeOutput recipeOutput, @NotNull ResourceKey<Recipe<?>> id) {
        this.ensureValid(id);
        Advancement.Builder builder = recipeOutput.advancement()
                .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(id))
                .rewards(AdvancementRewards.Builder.recipe(id))
                .requirements(AdvancementRequirements.Strategy.OR);
        this.criteria.forEach(builder::addCriterion);
        ShapelessRecipe shapelessrecipe = new ShapelessRecipe(
                Objects.requireNonNullElse(this.group, ""),
                RecipeBuilder.determineBookCategory(this.category),
                this.resultStack,
                this.ingredients
        );
        recipeOutput.accept(id, shapelessrecipe, builder.build(Identifier.fromNamespaceAndPath(id.registry().getNamespace(), id.registry().getPath()).withPrefix("recipes/" + this.category.getFolderName() + "/")));
    }

    // TODO: Legacy save method for compatibility - remove when all callers are updated
    public void save(RecipeOutput recipeOutput, @NotNull Identifier id) {
        this.save(recipeOutput, ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE, id));
    }

    private void ensureValid(Identifier id) {
        if (this.criteria.isEmpty()) {
            throw new IllegalStateException("No way of obtaining recipe " + id);
        }
    }

    private void ensureValid(ResourceKey<?> id) {
        if (this.criteria.isEmpty()) {
            throw new IllegalStateException("No way of obtaining recipe " + id);
        }
    }
}
