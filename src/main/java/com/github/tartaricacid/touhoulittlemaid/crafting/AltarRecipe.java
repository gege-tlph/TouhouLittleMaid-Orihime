package com.github.tartaricacid.touhoulittlemaid.crafting;

import com.github.tartaricacid.touhoulittlemaid.entity.item.EntityBox;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.init.InitDataComponent;
import com.github.tartaricacid.touhoulittlemaid.init.InitEntities;
import com.github.tartaricacid.touhoulittlemaid.init.InitRecipes;
import com.github.tartaricacid.touhoulittlemaid.item.ItemFilm;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.Level;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.*;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;

// 1.21.11: CraftingRecipe.getType() 是 default 返回不变型 RecipeType<CraftingRecipe> → extends ShapelessRecipe 无法返回
// RecipeType<AltarRecipe>；改为直接 implements Recipe<CraftingInput>（同 26.1）。matches/placementInfo 复刻 vanilla ShapelessRecipe。
public class AltarRecipe implements Recipe<CraftingInput> {
    private final String group;
    private final CraftingBookCategory category;
    private final NonNullList<Ingredient> ingredients;
    private final float power;
    private final ItemStack result;
    private final Identifier entityType;
    private final String langKey;
    @Nullable
    private PlacementInfo placementInfo;

    public AltarRecipe(String group, CraftingBookCategory category, NonNullList<Ingredient> ingredients, float power, ItemStack result, Identifier entityType, String langKey) {
        this.group = group;
        this.category = category;
        this.ingredients = ingredients;
        this.power = power;
        this.result = result;
        this.entityType = entityType;
        this.langKey = langKey;
    }

    public Identifier getId() {
        return BuiltInRegistries.RECIPE_TYPE.getKey(InitRecipes.ALTAR_CRAFTING);
    }

    public String getRecipeString() {
        String recipeId = this.result.get(InitDataComponent.RECIPES_ID_TAG);
        return Objects.requireNonNullElse(recipeId, "spawn_box");
    }

    public boolean isItemCraft() {
        return entityType.equals(BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.ITEM));
    }

    public void spawnOutputEntity(ServerLevel world, BlockPos pos, @Nullable List<ItemStack> list) {
        // ENTITY_TYPE is a defaulted registry. Preserve 1.21.1's fallback for
        // syntactically valid but unknown ids instead of manufacturing null.
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(entityType);

        if (type == EntityType.ITEM) {
            this.spawnItem(world, pos);
            return;
        }

        if (type == InitEntities.BOX) {
            this.spawnBoxMaid(world, pos);
            return;
        }

        if (type == InitEntities.MAID) {
            this.rebornMaid(world, pos, list);
            return;
        }

        // 生成类型为 EVENT 也许更合适
        type.spawn(world, pos, EntitySpawnReason.EVENT);
    }

    private void rebornMaid(ServerLevel world, BlockPos pos, @Nullable List<ItemStack> list) {
        ItemStack itemFilm = ItemStack.EMPTY;
        if (list != null) {
            itemFilm = list.stream().filter(stack -> stack.getItem() instanceof ItemFilm).findFirst().orElse(ItemStack.EMPTY);
        }
        EntityMaid maid = new EntityMaid(world);
        CustomData compoundData = itemFilm.get(InitDataComponent.MAID_INFO);
        if (compoundData != null) {
            CompoundTag maidCompound = compoundData.copyTag();
            // 1.21.11: readAdditionalSaveData(CompoundTag) -> (ValueInput)。
            // 保持 HEAD 的 readAdditionalSaveData（26.1 在此改用 load，属其行为变更，不采纳）。
            maid.readAdditionalSaveData(TagValueInput.create(
                    ProblemReporter.DISCARDING, world.registryAccess(), maidCompound));
        } else {
            maid.finalizeSpawn(world, world.getCurrentDifficultyAt(pos), EntitySpawnReason.SPAWN_ITEM_USE, null);
        }
        maid.setPos(pos.getX(), pos.getY(), pos.getZ());
        world.addFreshEntity(maid);
    }

    private void spawnBoxMaid(ServerLevel world, BlockPos pos) {
        EntityBox box = new EntityBox(world);
        box.setPos(pos.getX(), pos.getY(), pos.getZ());

        EntityMaid maid = new EntityMaid(world);
        maid.setPos(pos.getX(), pos.getY(), pos.getZ());
        maid.finalizeSpawn(world, world.getCurrentDifficultyAt(pos), EntitySpawnReason.SPAWN_ITEM_USE, null);
        // 1.21.11: startRiding(Entity, boolean) 移除 -> (Entity, boolean, boolean)（同 26.1）
        maid.startRiding(box, true, true);

        world.tryAddFreshEntityWithPassengers(box);
    }

    private void spawnItem(ServerLevel world, BlockPos pos) {
        ItemEntity itemEntity = new ItemEntity(world, pos.getX(), pos.getY(), pos.getZ(), this.result.copy());
        world.addFreshEntity(itemEntity);
    }

    // ==== Recipe<CraftingInput> 接口实现（matches/placementInfo 复刻 vanilla ShapelessRecipe，逐字节等价行为）====
    @Override
    public boolean matches(CraftingInput input, Level level) {
        if (input.ingredientCount() != this.ingredients.size()) {
            return false;
        }
        return input.size() == 1 && this.ingredients.size() == 1
                ? this.ingredients.getFirst().test(input.getItem(0))
                : input.stackedContents().canCraft(this, null);
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider provider) {
        return this.result.copy();
    }

    @Override
    public @NotNull RecipeType<AltarRecipe> getType() {
        return InitRecipes.ALTAR_CRAFTING;
    }

    @Override
    public RecipeSerializer<AltarRecipe> getSerializer() {
        return InitRecipes.ALTAR_RECIPE_SERIALIZER;
    }

    @Override
    public PlacementInfo placementInfo() {
        if (this.placementInfo == null) {
            this.placementInfo = PlacementInfo.create(this.ingredients);
        }
        return this.placementInfo;
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        // ALTAR_CRAFTING 是独立 RecipeType（非 minecraft:crafting）→ 不进合成配方书；此值仅为接口占位
        return RecipeBookCategories.CRAFTING_MISC;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public boolean showNotification() {
        return false;
    }

    @Override
    public String group() {
        return group;
    }

    // ==== 序列化器 / 配方查看器消费方需要的 getter（getGroup/getIngredients 无 @Override，因接口方法名不同）====
    public NonNullList<Ingredient> getIngredients() {
        return ingredients;
    }

    public float getPower() {
        return power;
    }

    public String getGroup() {
        return group;
    }

    public CraftingBookCategory getCategory() {
        return category;
    }

    public ItemStack getResult() {
        return result;
    }

    public Identifier getEntityType() {
        return entityType;
    }

    public String getLangKey() {
        return langKey;
    }
}
