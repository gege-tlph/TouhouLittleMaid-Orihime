package com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.task;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.init.InitBrains;
import com.github.tartaricacid.touhoulittlemaid.init.InitEntities;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.behavior.MoveToTargetSink;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.level.GameType;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Set;

public class MaidFollowOwnerGameTest {
    /**
     * A follow-mode maid must come to its owner even while it still has queued work. Only genuine
     * combat (an emergency response or an active attack target) may hold following back. This
     * replaces the earlier "follow yields to any active work" behavior, which players experienced
     * as the maid refusing to come when called while it had farm/interaction work pending.
     */
    @GameTest(maxTicks = 100)
    public void ownerFollowOnlyYieldsToCombat(GameTestHelper helper) {
        EntityMaid maid = helper.spawn(InitEntities.MAID, new BlockPos(1, 2, 1));
        Player owner = helper.makeMockPlayer(GameType.CREATIVE);

        assertFalse(helper, MaidFollowOwnerTask.hasCompetingGoal(maid, owner),
                "an idle maid should be eligible to follow");

        // A pending non-owner walk target must NOT block following.
        maid.getBrain().setMemory(MemoryModuleType.WALK_TARGET,
                new WalkTarget(new BlockPosTracker(maid.blockPosition().offset(3, 0, 0)), 0.6F, 0));
        assertFalse(helper, MaidFollowOwnerTask.hasCompetingGoal(maid, owner),
                "a pending work walk target must not block following");
        maid.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);

        // A pending block interaction (work) target must NOT block following.
        maid.getBrain().setMemory(InitBrains.TARGET_POS,
                new BlockPosTracker(maid.blockPosition().offset(2, 0, 0)));
        assertFalse(helper, MaidFollowOwnerTask.hasCompetingGoal(maid, owner),
                "a pending work interaction target must not block following");
        maid.getBrain().eraseMemory(InitBrains.TARGET_POS);

        // Only genuine combat blocks following.
        maid.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, owner);
        assertTrue(helper, MaidFollowOwnerTask.hasCompetingGoal(maid, owner),
                "an active attack target must block following");
        maid.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);

        // The follow-target install path still tracks the owner with the expected TTL.
        MaidFollowOwnerTask.setExpiringFollowTarget(maid, owner, 0.6F, 2);
        WalkTarget followTarget = maid.getBrain().getMemory(MemoryModuleType.WALK_TARGET)
                .orElseThrow(() -> helper.assertionException("follow target was not installed"));
        if (!(followTarget.getTarget() instanceof EntityTracker tracker) || !tracker.getEntity().equals(owner)) {
            throw helper.assertionException("follow target does not track the owner");
        }
        long ttl = maid.getBrain().getTimeUntilExpiry(MemoryModuleType.WALK_TARGET);
        if (ttl != 1) {
            throw helper.assertionException("follow target ttl expected=1 got=%d".formatted(ttl));
        }

        helper.succeed();
    }

    /**
     * 跟随的注册优先级是一个纯数字，而数字最容易被静默改掉——它不在任何文件面 / 提交面 /
     * 公开面的枚举里（本轮正是靠跨版本 wiki 的行为面指纹才照出本树漏了 3→4）。这里把它钉进仓内。
     *
     * <p>断言的是**契约而不是那个字面量**：跟随必须先于全部工作与机会行为启动（否则被叫的女仆
     * 不会过来），且必须晚于 CORE 的移动 sink（它只是兜底，不该抢在移动之前）。下限断言与结论正交
     * ——先要求「真的认出了若干个行为」，否则反射一旦失效就会零覆盖恒绿。</p>
     */
    @GameTest(maxTicks = 100)
    public void followRunsBeforeWorkButAfterCoreMovement(GameTestHelper helper) {
        EntityMaid maid = helper.spawn(InitEntities.MAID, new BlockPos(1, 2, 1));
        Map<Integer, Map<Activity, Set<BehaviorControl<?>>>> byPriority = readBehaviorsByPriority(helper, maid);

        int recognised = 0;
        int followPriority = Integer.MIN_VALUE;
        int vehicleFollowPriority = Integer.MIN_VALUE;
        int coreMoveSinkPriority = Integer.MIN_VALUE;
        int minWorkPriority = Integer.MAX_VALUE;
        for (Map.Entry<Integer, Map<Activity, Set<BehaviorControl<?>>>> byPriorityEntry : byPriority.entrySet()) {
            int priority = byPriorityEntry.getKey();
            for (Map.Entry<Activity, Set<BehaviorControl<?>>> byActivity : byPriorityEntry.getValue().entrySet()) {
                for (BehaviorControl<?> behavior : byActivity.getValue()) {
                    recognised++;
                    if (behavior instanceof MaidFollowOwnerTask) {
                        followPriority = priority;
                    } else if (behavior instanceof MaidFollowOwnerVehicleTask) {
                        vehicleFollowPriority = priority;
                    } else if (behavior instanceof MoveToTargetSink) {
                        coreMoveSinkPriority = priority;
                    } else if (byActivity.getKey() == Activity.WORK) {
                        minWorkPriority = Math.min(minWorkPriority, priority);
                    }
                }
            }
        }

        assertTrue(helper, recognised >= 20,
                "only recognised %d registered behaviors, the brain reflection is probably broken".formatted(recognised));
        assertTrue(helper, followPriority != Integer.MIN_VALUE && vehicleFollowPriority != Integer.MIN_VALUE,
                "follow behaviors are not registered in the brain at all");
        assertTrue(helper, coreMoveSinkPriority != Integer.MIN_VALUE,
                "core MoveToTargetSink is not registered, the comparison below would be vacuous");
        assertTrue(helper, minWorkPriority != Integer.MAX_VALUE,
                "no WORK activity behaviors found, the comparison below would be vacuous");

        assertTrue(helper, followPriority == vehicleFollowPriority,
                "on-foot and on-vehicle following must share one priority: %d vs %d"
                        .formatted(followPriority, vehicleFollowPriority));
        assertTrue(helper, followPriority > coreMoveSinkPriority,
                "following must start after the core movement sink: follow=%d sink=%d"
                        .formatted(followPriority, coreMoveSinkPriority));
        assertTrue(helper, followPriority < minWorkPriority,
                "following must start before every work behavior or a called maid never comes: follow=%d minWork=%d"
                        .formatted(followPriority, minWorkPriority));

        helper.succeed();
    }

    @SuppressWarnings("unchecked")
    private static Map<Integer, Map<Activity, Set<BehaviorControl<?>>>> readBehaviorsByPriority(
            GameTestHelper helper, EntityMaid maid) {
        try {
            Field field = Brain.class.getDeclaredField("availableBehaviorsByPriority");
            field.setAccessible(true);
            return (Map<Integer, Map<Activity, Set<BehaviorControl<?>>>>) field.get(maid.getBrain());
        } catch (ReflectiveOperationException | RuntimeException exception) {
            // 读不到就红，不要静默跳过——那会让本用例变成一个永远通过的空壳
            throw helper.assertionException("cannot read Brain.availableBehaviorsByPriority: " + exception);
        }
    }

    private static void assertTrue(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            throw helper.assertionException(message);
        }
    }

    private static void assertFalse(GameTestHelper helper, boolean condition, String message) {
        assertTrue(helper, !condition, message);
    }
}
