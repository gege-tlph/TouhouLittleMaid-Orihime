package com.github.tartaricacid.touhoulittlemaid.entity.backpack.data;

import com.github.tartaricacid.touhoulittlemaid.api.backpack.IBackpackData;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

import javax.annotation.Nullable;

/**
 * 熔炉背包的数据与烧炼逻辑，自行为基准 {@code port/1.21.11-fabric} 搬入。
 *
 * <p>26.1.2 侧的三处 API 漂移（对照原版 {@code AbstractFurnaceBlockEntity} 反编译源实查）：
 * {@code Recipe.assemble} 去掉了 {@code RegistryAccess} 参数（单参）；燃料残留物从
 * {@code ItemStack.getRecipeRemainder} 改为 {@code Item.getCraftingRemainder()} 返回
 * {@code ItemStackTemplate}，消耗 idiom 是「先 shrink，空了再 {@code remainder.create()}」
 * ——对真实燃料（残留物燃料都 stacksTo(1)）与基准行为等价；
 * {@code CachedCheck.getRecipeFor(I, ServerLevel)} 与 1.21.11 相同。</p>
 */
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

    /**
     * 稀疏槽位索引键。上游缺陷（TartaricAcid/TouhouLittleMaid#1053）：
     * {@code storeAsItemList} 只按槽位顺序写出**非空**物品且不带索引，
     * {@code fromItemList} 又用 {@code addItem} 逐个塞进**第一个可用槽**。
     * 于是「输入空、燃料或产物非空」的稀疏状态一存一读就被压紧：
     * 燃料被读进输入槽，产物被读进燃料槽——重进世界后燃料会被当作原料烧掉，属实打实的物品损坏。
     * <p>
     * 这里沿用行为基准的**纯增量**修法：照旧写 {@code Items}（老版本读到的东西完全一致，
     * 不制造向前不兼容），另写一份与之同序的槽位索引。存在即按索引精确还原，缺失则回落旧行为，
     * 因此基准分支的存档能原样读入。
     */
    private static final String ITEM_SLOTS_TAG = "ItemSlots";

    @Override
    public void load(CompoundTag tag, EntityMaid maid) {
        var itemList = TagValueInput.create(ProblemReporter.DISCARDING, this.level.registryAccess(), tag)
                .listOrEmpty("Items", ItemStack.CODEC);
        int[] slots = tag.getIntArray(ITEM_SLOTS_TAG).orElse(null);
        if (slots == null) {
            // 旧存档：没有索引可用，只能维持原有的压紧行为
            this.fromItemList(itemList);
        } else {
            this.clearContent();
            int index = 0;
            for (ItemStack stack : itemList) {
                int slot = index < slots.length ? slots[index] : -1;
                if (slot >= 0 && slot < this.getContainerSize()) {
                    this.setItem(slot, stack);
                } else {
                    // 索引与物品数对不上（外部篡改）时退回旧行为，至少不丢物品
                    this.addItem(stack);
                }
                index++;
            }
        }
        // 计时字段必须在放置物品**之后**恢复：本类覆写的 setItem 会在输入槽内容变化时
        // 把 cookingProgress 清零，先读后放会让烧制进度每次重进世界都归零。
        this.litTime = tag.getIntOr("BurnTime", 0);
        this.cookingProgress = tag.getIntOr("CookTime", 0);
        this.cookingTotalTime = tag.getIntOr("CookTimeTotal", 0);
        this.litDuration = this.getBurnDuration(this.getItem(FUEL_INDEX));
    }

    @Override
    public void save(CompoundTag tag, EntityMaid maid) {
        tag.putInt("BurnTime", this.litTime);
        tag.putInt("CookTime", this.cookingProgress);
        tag.putInt("CookTimeTotal", this.cookingTotalTime);
        TagValueOutput itemsOutput = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, this.level.registryAccess());
        this.storeAsItemList(itemsOutput.list("Items", ItemStack.CODEC));
        tag.put("Items", itemsOutput.buildResult().getListOrEmpty("Items"));
        // 与 storeAsItemList 同序：它按槽位升序只写非空项，这里就按同一遍历收集其槽位号
        int[] slots = new int[this.getContainerSize()];
        int count = 0;
        for (int slot = 0; slot < this.getContainerSize(); slot++) {
            if (!this.getItem(slot).isEmpty()) {
                slots[count++] = slot;
            }
        }
        tag.putIntArray(ITEM_SLOTS_TAG, java.util.Arrays.copyOf(slots, count));
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
                recipe = this.quickCheck.getRecipeFor(new SingleRecipeInput(this.getItem(INPUT_INDEX)), (ServerLevel) level)
                        .map(RecipeHolder::value).orElse(null);
            }

            int maxStackSize = this.getMaxStackSize();
            // 没有燃烧，但是可以燃！
            if (!this.isLit() && this.canBurn(recipe, this, maxStackSize)) {
                this.litTime = this.getBurnDuration(fuelItem);
                this.litDuration = this.litTime;
                // 如果此时点燃了，消耗燃料（26.1.2 idiom：shrink 后空则以 ItemStackTemplate 生成残留物，
                // 比如熔岩桶烧完剩一个桶）
                if (this.isLit() && fuelNotEmpty) {
                    Item fuel = fuelItem.getItem();
                    fuelItem.shrink(1);
                    if (fuelItem.isEmpty()) {
                        ItemStackTemplate remainder = fuel.getCraftingRemainder();
                        this.setItem(FUEL_INDEX, remainder != null ? remainder.create() : ItemStack.EMPTY);
                    }
                }
            }

            // 点燃了，而且也能燃！
            if (this.isLit() && this.canBurn(recipe, this, maxStackSize)) {
                // 各种进度增加
                ++this.cookingProgress;
                // 如果进度满了，重置，并给出产物
                if (this.cookingProgress == this.cookingTotalTime) {
                    this.cookingProgress = 0;
                    this.cookingTotalTime = getTotalCookTime((ServerLevel) level);
                    // 如果烧制成功，把经验给女仆
                    if (this.burn(recipe, this, maxStackSize)) {
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
        // ClientboundContainerSetContentPacket also calls setItem on the client.
        // CachedCheck requires ServerLevel; dataAccess synchronizes the
        // authoritative server timing value afterwards.
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

    private boolean canBurn(@Nullable SmeltingRecipe recipe, SimpleContainer container, int maxStackSize) {
        // 先检查输入物品和配方
        if (!container.getItem(INPUT_INDEX).isEmpty() && recipe != null) {
            // 先检查配方结果（26.1.2：assemble 单参，RegistryAccess 已去掉）
            ItemStack result = recipe.assemble(new SingleRecipeInput(this.getItem(INPUT_INDEX)));
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
                    // Forge fix: make furnace respect stack sizes in furnace recipes
                    return true;
                } else {
                    // Forge fix: make furnace respect stack sizes in furnace recipes
                    return output.getCount() + result.getCount() <= result.getMaxStackSize();
                }
            }
        } else {
            return false;
        }
    }

    private boolean burn(@Nullable SmeltingRecipe recipe, SimpleContainer container, int maxStackSize) {
        if (recipe != null && this.canBurn(recipe, container, maxStackSize)) {
            ItemStack input = container.getItem(INPUT_INDEX);
            ItemStack result = recipe.assemble(new SingleRecipeInput(this.getItem(INPUT_INDEX)));
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
        return quickCheck.getRecipeFor(new SingleRecipeInput(this.getItem(INPUT_INDEX)), level)
                .map(recipeHolder -> recipeHolder.value().cookingTime()).orElse(200);
    }
}
