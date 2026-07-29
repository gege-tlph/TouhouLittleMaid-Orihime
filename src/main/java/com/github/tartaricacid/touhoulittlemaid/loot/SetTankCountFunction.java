package com.github.tartaricacid.touhoulittlemaid.loot;

import com.github.tartaricacid.touhoulittlemaid.entity.backpack.data.TankBackpackData;
import com.github.tartaricacid.touhoulittlemaid.init.InitDataComponent;
import com.github.tartaricacid.touhoulittlemaid.init.InitLootModifier;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.base.SingleFluidStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.github.tartaricacid.touhoulittlemaid.init.InitDataComponent.TANK_BACKPACK_TAG;

public class SetTankCountFunction extends LootItemConditionalFunction {
    public static MapCodec<SetTankCountFunction> CODEC = RecordCodecBuilder.mapCodec(instance -> commonFields(instance)
            .and(instance.group(
                    Identifier.CODEC.fieldOf("fluid_id").forGetter(f -> f.fluidId),
                    Codec.LONG.fieldOf("count").forGetter(f -> f.count)
            )).apply(instance, SetTankCountFunction::new));

    private final Identifier fluidId;
    private final long count;

    public SetTankCountFunction(List<LootItemCondition> predicates, Identifier fluidId, long count) {
        super(predicates);
        this.fluidId = fluidId;
        this.count = count;
    }

    @Override
    public @NotNull LootItemFunctionType<? extends LootItemConditionalFunction> getType() {
        return InitLootModifier.SET_TANK_COUNT_FUNCTION;
    }

    @Override
    protected @NotNull ItemStack run(ItemStack stack, @NotNull LootContext context) {
        CompoundTag tags = stack.get(TANK_BACKPACK_TAG);
        if (tags == null) {
            tags = new CompoundTag();
        }
        SingleFluidStorage tank = SingleFluidStorage.withFixedCapacity(TankBackpackData.CAPACITY, () -> {
        });
        // 1.21.11: Registry.get(Identifier) 返 Optional<Reference> → getValue(Identifier) 直返 Fluid（javap 确认）
        FluidVariant fluidStack = FluidVariant.of(BuiltInRegistries.FLUID.getValue(this.fluidId), DataComponentPatch.EMPTY);
        try (Transaction transaction = Transaction.openOuter()) {
            tank.insert(fluidStack, count, transaction);
            transaction.commit();
            // 1.21.11 + Fabric 6.0.x: SingleFluidStorage.writeNbt → writeData(ValueOutput)，merge 回既有 tags（格式兼容）
            TagValueOutput tankOut = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, context.getLevel().registryAccess());
            tank.writeData(tankOut);
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