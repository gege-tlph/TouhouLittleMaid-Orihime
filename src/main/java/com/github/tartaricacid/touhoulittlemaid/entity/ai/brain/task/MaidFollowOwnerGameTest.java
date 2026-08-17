package com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.task;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.init.InitBrains;
import com.github.tartaricacid.touhoulittlemaid.init.InitEntities;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;

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
            throw helper.assertionException("follow target ttl expected=1 got=%d", ttl);
        }

        helper.succeed();
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
