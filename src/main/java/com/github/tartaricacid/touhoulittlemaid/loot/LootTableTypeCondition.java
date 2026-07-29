package com.github.tartaricacid.touhoulittlemaid.loot;

import cn.sh1rocu.touhoulittlemaid.api.extension.ILootContext;
import cn.sh1rocu.touhoulittlemaid.mixin.accessor.LootContextParamSetsAccessor;
import com.github.tartaricacid.touhoulittlemaid.init.InitLootModifier;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
// TODO: LootContextParamSet was renamed to ContextKeySet in Minecraft 1.21.11
// import net.minecraft.world.level.storage.loot.parameters.LootContextParamSet;
import net.minecraft.util.context.ContextKeySet;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;

public record LootTableTypeCondition(Identifier lootTableType,
                                     @Nullable ResourceKey<LootTable> lootTableId,
                                     ResourceKey<LootTable> lootTableAdd) implements LootItemCondition {
    public static final MapCodec<LootTableTypeCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Identifier.CODEC.fieldOf("loot_table_type").forGetter(m -> m.lootTableType),
            ResourceKey.codec(Registries.LOOT_TABLE).optionalFieldOf("loot_table_id").forGetter(m -> Optional.ofNullable(m.lootTableId)),
            ResourceKey.codec(Registries.LOOT_TABLE).fieldOf("loot_table_add").forGetter(m -> m.lootTableAdd)
    ).apply(instance, (type, id, add)
            -> new LootTableTypeCondition(type, id.orElse(null), add)));

    @Override
    public boolean test(LootContext context) {
        Identifier currentLootTable = ((ILootContext) context).tlm$getQueriedLootTableId();
        return !currentLootTable.equals(lootTableAdd.identifier()) && typeAreEquals(context) && idAreEquals(context);
    }

    private boolean typeAreEquals(LootContext context) {
        // SWEEP R9-3（2026-07-19）：原 TODO「accessor 被注释」系过期断言——LootContextParamSetsAccessor
        // 已适配 1.21.11（BiMap<Identifier,ContextKeySet>）、已编译、已入 mixin 配置；
        // LootTable.getParamSet() 亦存在（javap 证）→ 还原 origin 逻辑（LootContextParamSet→ContextKeySet 纯改名；
        // 1.21.11 HolderGetter.Provider.get 收单个 ResourceKey，不再要 registry 前缀参）
        ResourceKey<LootTable> currentLootTable = ResourceKey.create(Registries.LOOT_TABLE, ((ILootContext) context).tlm$getQueriedLootTableId());
        ContextKeySet lootContextParamSet = LootContextParamSetsAccessor.tlm$getRegistry().get(lootTableType);
        return context.getResolver().get(currentLootTable).map(lootTable ->
                        Objects.equals(lootTable.value().getParamSet(), lootContextParamSet))
                .orElse(false);
    }

    private boolean idAreEquals(LootContext context) {
        if (this.lootTableId == null) {
            return true;
        }
        // 1.21.11: ResourceKey.location() → identifier()（javap 确认；lootTableId 为 ResourceKey<LootTable>，非 TagKey）
        return ((ILootContext) context).tlm$getQueriedLootTableId().equals(this.lootTableId.identifier());
    }

    @Override
    public @NotNull LootItemConditionType getType() {
        return InitLootModifier.LOOT_TABLE_TYPE;
    }
}