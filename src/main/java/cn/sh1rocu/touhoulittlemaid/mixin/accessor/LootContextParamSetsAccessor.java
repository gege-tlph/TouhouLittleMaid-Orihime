package cn.sh1rocu.touhoulittlemaid.mixin.accessor;

import com.google.common.collect.BiMap;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKeySet;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LootContextParamSets.class)
public interface LootContextParamSetsAccessor {
    // 1.21.11: LootContextParamSet 更名为 net.minecraft.util.context.ContextKeySet；
    // REGISTRY 字段仍在，类型为 BiMap<Identifier, ContextKeySet>（javap 确认）
    @Accessor("REGISTRY")
    static BiMap<Identifier, ContextKeySet> tlm$getRegistry() {
        throw new AssertionError();
    }
}
