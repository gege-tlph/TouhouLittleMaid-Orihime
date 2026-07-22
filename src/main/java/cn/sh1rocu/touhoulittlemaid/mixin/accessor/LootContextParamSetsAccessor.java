package cn.sh1rocu.touhoulittlemaid.mixin.accessor;

import com.google.common.collect.BiMap;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKeySet;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LootContextParamSets.class)
public interface LootContextParamSetsAccessor {
    // 暴露战利品上下文参数集注册表，供自定义战利品类型注册使用。
    @Accessor("REGISTRY")
    static BiMap<Identifier, ContextKeySet> tlm$getRegistry() {
        throw new AssertionError();
    }
}
