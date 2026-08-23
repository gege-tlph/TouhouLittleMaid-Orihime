package com.github.tartaricacid.touhoulittlemaid.loot;

import com.github.tartaricacid.touhoulittlemaid.entity.backpack.data.TankBackpackData;
import com.github.tartaricacid.touhoulittlemaid.init.InitDataComponent;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.base.SingleFluidStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.github.tartaricacid.touhoulittlemaid.init.InitDataComponent.TANK_BACKPACK_TAG;

/**
 * 战利品函数：给液体背包预填流体（下界要塞箱里的岩浆储罐）。
 *
 * <p>写法对新基：{@code getType()} 返回 {@code LootItemFunctionType} → {@code codec()} 返回
 * {@code MapCodec}（与 {@code RandomBoardStateFunction} 同款漂移）；储罐写出用 8.0.x 的静态
 * {@code SingleVariantStorage.writeValue}（见 {@link TankBackpackData} 的漂移注）。</p>
 */
public class SetTankCountFunction extends LootItemConditionalFunction {
    public static final MapCodec<SetTankCountFunction> CODEC = RecordCodecBuilder.mapCodec(instance -> commonFields(instance)
            .and(instance.group(
                    Identifier.CODEC.fieldOf("fluid_id").forGetter(function -> function.fluidId),
                    Codec.LONG.fieldOf("count").forGetter(function -> function.count)
            )).apply(instance, SetTankCountFunction::new));

    private final Identifier fluidId;
    private final long count;

    public SetTankCountFunction(List<LootItemCondition> predicates, Identifier fluidId, long count) {
        super(predicates);
        this.fluidId = fluidId;
        this.count = count;
    }

    @Override
    public @NotNull MapCodec<? extends LootItemConditionalFunction> codec() {
        return CODEC;
    }

    @Override
    protected @NotNull ItemStack run(ItemStack stack, @NotNull LootContext context) {
        CompoundTag tags = stack.get(TANK_BACKPACK_TAG);
        if (tags == null) {
            tags = new CompoundTag();
        }
        SingleFluidStorage tank = SingleFluidStorage.withFixedCapacity(TankBackpackData.CAPACITY, () -> {
        });
        FluidVariant fluidStack = FluidVariant.of(BuiltInRegistries.FLUID.getValue(this.fluidId), DataComponentPatch.EMPTY);
        try (Transaction transaction = Transaction.openOuter()) {
            tank.insert(fluidStack, count, transaction);
            transaction.commit();
            TagValueOutput tankOut = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, context.getLevel().registryAccess());
            SingleVariantStorage.writeValue(tank, FluidVariant.CODEC, tankOut);
            tags.merge(tankOut.buildResult());
            stack.set(InitDataComponent.TANK_BACKPACK_TAG, tags);
            return stack;
        }
    }

    public static class Builder extends LootItemConditionalFunction.Builder<SetTankCountFunction.Builder> {
        private final Fluid fluid;
        private final int bucketCount;

        public Builder(Fluid fluid, int bucketCount) {
            this.fluid = fluid;
            this.bucketCount = bucketCount;
        }

        @Override
        protected @NotNull Builder getThis() {
            return this;
        }

        @Override
        public @NotNull LootItemFunction build() {
            Identifier key = BuiltInRegistries.FLUID.getKey(fluid);
            return new SetTankCountFunction(this.getConditions(), key, bucketCount * FluidConstants.BUCKET);
        }
    }
}
