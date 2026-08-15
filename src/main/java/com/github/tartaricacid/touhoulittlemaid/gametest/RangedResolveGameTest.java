package com.github.tartaricacid.touhoulittlemaid.gametest;

import com.github.tartaricacid.touhoulittlemaid.api.task.IRangedAttackTask;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.entity.task.TaskBowAttack;
import com.github.tartaricacid.touhoulittlemaid.entity.task.TaskCrossBowAttack;
import com.github.tartaricacid.touhoulittlemaid.entity.task.TaskManager;
import com.github.tartaricacid.touhoulittlemaid.init.InitEntities;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 弓弩解绑工作任务：{@code IRangedAttackTask.resolveImplementation} 的判定面
 * （用户 2026-08-14 裁决，1.21.11 分支定案随行为基准前移）。
 *
 * <p>三条分别钉住：① 非远程任务也解析得出实现（解绑本体——此前「农场女仆手持弓有箭」
 * 在威胁响应里打不响）；② 当前远程任务胜出且**不额外要求 isWeapon**（1.21.11 红测实证：
 * 多要求会让「弓兵任务+手持弩」落到弩的实现、模组远程武器全哑）；③ 手里没被认领的武器
 * 且任务非远程时解析为 null（调用方据此不开火，与基准的空操作对齐）。</p>
 */
public class RangedResolveGameTest {

    private EntityMaid spawnMaid(GameTestHelper helper) {
        return helper.spawn(InitEntities.MAID, new BlockPos(1, 2, 1));
    }

    /** ① 默认（非远程）任务 + 手持弓 → 按武器扫注册表，解析出弓兵实现。 */
    @GameTest
    public void nonRangedTaskStillResolvesTheBowImplementation(GameTestHelper helper) {
        EntityMaid maid = spawnMaid(helper);
        maid.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BOW));
        IRangedAttackTask resolved = IRangedAttackTask.resolveImplementation(maid, maid.getMainHandItem());
        if (resolved == null || !TaskBowAttack.UID.equals(resolved.getUid())) {
            helper.fail("非远程任务手持弓应解析出弓兵实现，实得："
                    + (resolved == null ? "null" : resolved.getUid()));
            return;
        }
        helper.succeed();
    }

    /** ② 当前任务是弓兵 + 手持弩 → 当前任务胜出（不得因 isWeapon 落到弩的实现）。 */
    @GameTest
    public void currentRangedTaskWinsEvenWithAnotherWeapon(GameTestHelper helper) {
        EntityMaid maid = spawnMaid(helper);
        maid.setTask(TaskManager.findTask(TaskBowAttack.UID).orElseThrow());
        maid.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.CROSSBOW));
        IRangedAttackTask resolved = IRangedAttackTask.resolveImplementation(maid, maid.getMainHandItem());
        if (resolved == null || !TaskBowAttack.UID.equals(resolved.getUid())) {
            helper.fail("弓兵任务手持弩应仍用当前任务的实现，实得："
                    + (resolved == null ? "null" : resolved.getUid()));
            return;
        }
        // 反向对照：弩确实有自己的任务在注册表里，胜出不是因为弩没人认领
        if (TaskManager.findTask(TaskCrossBowAttack.UID).isEmpty()) {
            helper.fail("对照失效：注册表里找不到弩兵任务");
            return;
        }
        helper.succeed();
    }

    /** ③ 非远程任务 + 手持锄头 → null（没人认领，调用方不开火）。 */
    @GameTest
    public void unclaimedWeaponResolvesToNull(GameTestHelper helper) {
        EntityMaid maid = spawnMaid(helper);
        maid.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_HOE));
        IRangedAttackTask resolved = IRangedAttackTask.resolveImplementation(maid, maid.getMainHandItem());
        if (resolved != null) {
            helper.fail("锄头不该被任何远程任务认领，实得：" + resolved.getUid());
            return;
        }
        helper.succeed();
    }
}
