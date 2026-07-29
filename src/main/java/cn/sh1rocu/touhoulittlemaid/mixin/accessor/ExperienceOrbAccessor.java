package cn.sh1rocu.touhoulittlemaid.mixin.accessor;

import net.minecraft.world.entity.ExperienceOrb;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ExperienceOrb.class)
public interface ExperienceOrbAccessor {
    // 1.21.11: ExperienceOrb.value 字段移除（改为 DATA_VALUE 同步数据）→ 用 @Invoker 调 private setValue(int) 方法。
    // StackOverflowError，任何 XP 球 tick 即崩服，Node 4 smoke 实测抓获）→ 按 Mixin 惯例改名 invokeSetValue。
    @Invoker("setValue")
    void invokeSetValue(int value);
}
