package cn.sh1rocu.touhoulittlemaid.mixin.accessor;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.function.Predicate;

/**
 * B6b: 1.21.11 起 TemptGoal 的 mob/speedModifier 为 protected、items/canScare 为 private，
 * EntityJoinWorldEvent 需读取它们把 vanilla TemptGoal 换成 MaidTemptGoal → 提供 accessor。
 * 字段名经 javap 确认（mob:Mob · speedModifier:double · items:Predicate<ItemStack> · canScare:boolean）。
 */
@Mixin(TemptGoal.class)
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
