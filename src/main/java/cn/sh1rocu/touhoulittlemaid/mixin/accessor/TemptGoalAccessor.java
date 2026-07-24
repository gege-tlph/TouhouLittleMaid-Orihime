package cn.sh1rocu.touhoulittlemaid.mixin.accessor;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.function.Predicate;


@Mixin(TemptGoal.class)
/**
 * 暴露原版诱惑目标的构造参数，用于在实体加入世界时替换为女仆专用实现。
 */
public interface TemptGoalAccessor {
    @Accessor("mob")
    Mob tlm$mob();

    @Accessor("speedModifier")
    double tlm$speedModifier();

    @Accessor("items")
    Predicate<ItemStack> tlm$items();

    @Accessor("canScare")
    boolean tlm$canScare();
}
