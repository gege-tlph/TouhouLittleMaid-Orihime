package com.github.tartaricacid.touhoulittlemaid.entity.backpack.data;

import com.github.tartaricacid.touhoulittlemaid.api.backpack.IBackpackData;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import javax.annotation.Nullable;

public class FurnaceBackpackData extends SimpleContainer implements IBackpackData {
    private static final int INPUT_INDEX = 0;
    private static final int FUEL_INDEX = 1;
    private static final int OUTPUT_INDEX = 2;
    private int litTime;
    private int litDuration;
    private int cookingProgress;
    private int cookingTotalTime;
    private final RecipeManager.CachedCheck<SingleRecipeInput, SmeltingRecipe> quickCheck;
    private final Level level;
    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> FurnaceBackpackData.this.litTime;
                case 1 -> FurnaceBackpackData.this.litDuration;
                case 2 -> FurnaceBackpackData.this.cookingProgress;
                case 3 -> FurnaceBackpackData.this.cookingTotalTime;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> FurnaceBackpackData.this.litTime = value;
                case 1 -> FurnaceBackpackData.this.litDuration = value;
                case 2 -> FurnaceBackpackData.this.cookingProgress = value;
                case 3 -> FurnaceBackpackData.this.cookingTotalTime = value;
            }
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    public FurnaceBackpackData(EntityMaid maid) {
        super(3);
        this.quickCheck = RecipeManager.createCheck(RecipeType.SMELTING);
        this.level = maid.level;
    }

    @Override
    public ContainerData getDataAccess() {
        return dataAccess;
    }

    @Override
    public void load(CompoundTag tag, EntityMaid maid) {
        this.litTime = tag.getIntOr("BurnTime", 0);
        this.cookingProgress = tag.getIntOr("CookTime", 0);
        this.cookingTotalTime = tag.getIntOr("CookTimeTotal", 0);
        this.litDuration = this.getBurnDuration(this.getItem(FUEL_INDEX));

        this.fromItemList(TagValueInput.create(ProblemReporter.DISCARDING, this.level.registryAccess(), tag).listOrEmpty("Items", ItemStack.CODEC));
    }

    @Override
    public void save(CompoundTag tag, EntityMaid maid) {
        tag.putInt("BurnTime", this.litTime);
        tag.putInt("CookTime", this.cookingProgress);
        tag.putInt("CookTimeTotal", this.cookingTotalTime);

        TagValueOutput itemsOutput = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, this.level.registryAccess());
        this.storeAsItemList(itemsOutput.list("Items", ItemStack.CODEC));
        tag.put("Items", itemsOutput.buildResult().getListOrEmpty("Items"));
    }

    @Override
    public void serverTick(EntityMaid maid) {
        Level level = maid.level();
        // 如果是燃烧状态，继续燃烧
        if (this.isLit()) {
            --this.litTime;
        }
        ItemStack fuelItem = this.getItem(FUEL_INDEX);
        boolean inputNotEmpty = !this.getItem(INPUT_INDEX).isEmpty();
        boolean fuelNotEmpty = !fuelItem.isEmpty();
        boolean readyForLit = inputNotEmpty && fuelNotEmpty;
        // 要么正在燃烧，要么具备燃烧条件
        if (this.isLit() || readyForLit) {
            // 从缓存中获取配方
            SmeltingRecipe recipe = null;
            if (inputNotEmpty) {
                recipe = this.quickCheck.getRecipeFor(new SingleRecipeInput(this.getItem(INPUT_INDEX)), (ServerLevel) level).map(RecipeHolder::value).orElse(null);
            }

            int maxStackSize = this.getMaxStackSize();
            // 没有燃烧，但是可以燃！
            if (!this.isLit() && this.canBurn(level.registryAccess(), recipe, this, maxStackSize)) {
                this.litTime = this.getBurnDuration(fuelItem);
                this.litDuration = this.litTime;
                // 如果此时点燃了
                if (this.isLit()) {
                    // 如果燃料有残留物，比如熔岩桶燃烧后残留一个桶
                    if (!fuelItem.getItem().getCraftingRemainder().isEmpty()) {
                        this.setItem(FUEL_INDEX, fuelItem.getRecipeRemainder());
                    } else if (fuelNotEmpty) {
                        // 普通燃料减一
                        fuelItem.shrink(1);
                        if (fuelItem.isEmpty()) {
                            this.setItem(FUEL_INDEX, fuelItem.getItem().getRecipeRemainder(fuelItem));
                        }
                    }
                }
            }

            // 点燃了，而且也能燃！
            if (this.isLit() && this.canBurn(level.registryAccess(), recipe, this, maxStackSize)) {
                // 各种进度增加
                ++this.cookingProgress;
                // 如果进度满了，重置，并给出产物
                if (this.cookingProgress == this.cookingTotalTime) {
                    this.cookingProgress = 0;
                    this.cookingTotalTime = getTotalCookTime((ServerLevel) level);
                    // 如果烧制成功，把经验给女仆
                    if (this.burn(level.registryAccess(), recipe, this, maxStackSize)) {
                        int exp = this.createExperience(recipe.experience());
                        maid.setExperience(maid.getExperience() + exp);
                    }
                }
            } else {
                // 否则直接重置烧制进度
                this.cookingProgress = 0;
            }
        } else if (this.cookingProgress > 0) {
            // 什么，燃料不足，那就逐 tick 递减烧制进度
            this.cookingProgress = Mth.clamp(this.cookingProgress - 2, 0, this.cookingTotalTime);
        }
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        ItemStack slotItem = this.getItem(index);
        boolean isSameItem = !stack.isEmpty() && ItemStack.isSameItemSameComponents(slotItem, stack);
        super.setItem(index, stack);
        // 客户端也会更新槽位，但配方缓存只能由服务端重算；计时值随后通过容器数据同步。
        if (index == INPUT_INDEX && !isSameItem && this.level instanceof ServerLevel serverLevel) {
            this.cookingTotalTime = getTotalCookTime(serverLevel);
            this.cookingProgress = 0;
        }
    }

    private int createExperience(float recipeExp) {
        int integer = Mth.floor(recipeExp);
        float decimal = Mth.frac(recipeExp);
        if (decimal != 0 && Math.random() < (double) decimal) {
            ++integer;
        }
        return integer;
    }

    private boolean isLit() {
        return this.litTime > 0;
    }

    private int getBurnDuration(ItemStack fuel) {
        return fuel.isEmpty() ? 0 : this.level.fuelValues().burnDuration(fuel);
    }

    private boolean canBurn(RegistryAccess access, @Nullable SmeltingRecipe recipe, SimpleContainer container, int maxStackSize) {
        // 先检查输入物品和配方
        if (!container.getItem(INPUT_INDEX).isEmpty() && recipe != null) {
            // 先检查配方结果
            ItemStack result = recipe.assemble(new SingleRecipeInput(this.getItem(INPUT_INDEX)), access);
            // 没结果，不能燃烧
            if (result.isEmpty()) {
                return false;
            } else {
                // 检查输出栏
                ItemStack output = container.getItem(OUTPUT_INDEX);
                if (output.isEmpty()) {
                    // 空的，可以放
                    return true;
                } else if (!ItemStack.isSameItem(output, result)) {
                    // 不同物品，不行
                    return false;
                } else if (output.getCount() + result.getCount() <= maxStackSize && output.getCount() + result.getCount() <= output.getMaxStackSize()) {
                    // 锻造修复：使熔炉尊重熔炉配方中的堆叠尺寸
                    return true;
                } else {
                    // 锻造修复：使熔炉尊重熔炉配方中的堆叠尺寸
                    return output.getCount() + result.getCount() <= result.getMaxStackSize();
                }
            }
        } else {
            return false;
        }
    }

    private boolean burn(RegistryAccess access, @Nullable SmeltingRecipe recipe, SimpleContainer container, int maxStackSize) {
        if (recipe != null && this.canBurn(access, recipe, container, maxStackSize)) {
            ItemStack input = container.getItem(INPUT_INDEX);
            ItemStack result = recipe.assemble(new SingleRecipeInput(this.getItem(INPUT_INDEX)), access);
            ItemStack output = container.getItem(OUTPUT_INDEX);
            // 如果输出栏为空
            if (output.isEmpty()) {
                // 放东西
                container.setItem(OUTPUT_INDEX, result.copy());
            } else if (output.is(result.getItem())) {
                // 相同物品，增长数量即可
                output.grow(result.getCount());
            }
            // 如果是海绵和桶
            if (input.is(Blocks.WET_SPONGE.asItem()) && !container.getItem(FUEL_INDEX).isEmpty() && container.getItem(FUEL_INDEX).is(Items.BUCKET)) {
                container.setItem(FUEL_INDEX, new ItemStack(Items.WATER_BUCKET));
            }
            input.shrink(1);
            return true;
        } else {
            return false;
        }
    }

    private int getTotalCookTime(ServerLevel level) {

        return quickCheck.getRecipeFor(new SingleRecipeInput(this.getItem(INPUT_INDEX)), level).map(recipeHolder ->
                recipeHolder.value().cookingTime()).orElse(200);
    }
}
