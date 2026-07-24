package com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.task;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.init.InitEntities;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

public class MaidFollowOwnerGameTest {
    @GameTest(maxTicks = 100)
    public void ownerFollowYieldsToActiveGoals(GameTestHelper helper) {
        EntityMaid maid = helper.spawn(InitEntities.MAID, new BlockPos(1, 2, 1));
        Player owner = helper.makeMockPlayer(GameType.CREATIVE);

        assertFalse(helper, MaidFollowOwnerTask.hasCompetingGoal(maid, owner),
                "an idle maid should be eligible to follow");

        WalkTarget workTarget = new WalkTarget(new BlockPosTracker(maid.blockPosition().offset(3, 0, 0)), 0.6F, 0);
        maid.getBrain().setMemory(MemoryModuleType.WALK_TARGET, workTarget);
        assertTrue(helper, MaidFollowOwnerTask.hasCompetingGoal(maid, owner),
                "a non-owner walk target must block following");

        maid.getBrain().setMemory(MemoryModuleType.WALK_TARGET,
                new WalkTarget(new EntityTracker(owner, true), 0.6F, 2));
        assertFalse(helper, MaidFollowOwnerTask.hasCompetingGoal(maid, owner),
                "the existing owner target is follow-owned, not competing work");

        maid.getBrain().setMemory(InitEntities.TARGET_POS, new BlockPosTracker(maid.blockPosition().offset(2, 0, 0)));
        assertTrue(helper, MaidFollowOwnerTask.hasCompetingGoal(maid, owner),
                "a block interaction target must block following");
        maid.getBrain().eraseMemory(InitEntities.TARGET_POS);

        maid.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, owner);
        assertTrue(helper, MaidFollowOwnerTask.hasCompetingGoal(maid, owner),
                "an attack target must block following");
        maid.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
        maid.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);

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

    @SuppressWarnings("removal")
    @GameTest(maxTicks = 120)
    public void activeWorkTargetSurvivesFastFlyingOwner(GameTestHelper helper) {
        EntityMaid maid = helper.spawn(InitEntities.MAID, new BlockPos(1, 2, 1));
        ServerPlayer owner = helper.makeMockServerPlayerInLevel();
        BlockPos initialOwnerPos = helper.absolutePos(new BlockPos(10, 2, 1));
        BlockPos distantOwnerPos = helper.absolutePos(new BlockPos(25, 2, 1));

        for (int x = 20; x <= 30; x++) {
            for (int z = -4; z <= 6; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
                helper.setBlock(new BlockPos(x, 2, z), Blocks.AIR);
                helper.setBlock(new BlockPos(x, 3, z), Blocks.AIR);
            }
        }

        owner.snapTo(initialOwnerPos.getX() + 0.5, initialOwnerPos.getY(), initialOwnerPos.getZ() + 0.5, 0, 0);
        owner.getAbilities().mayfly = true;
        owner.getAbilities().flying = true;
        owner.onUpdateAbilities();
        maid.tame(owner);
        maid.setHomeModeEnable(false);

        BlockPos workPos = helper.absolutePos(new BlockPos(3, 2, 1));
        maid.getBrain().setMemory(InitEntities.TARGET_POS, new BlockPosTracker(workPos));
        maid.getBrain().setMemory(MemoryModuleType.WALK_TARGET,
                new WalkTarget(new BlockPosTracker(workPos), 0.6F, 0));

        helper.runAtTickTime(5, () ->
                owner.snapTo(distantOwnerPos.getX() + 0.5, distantOwnerPos.getY(), distantOwnerPos.getZ() + 0.5, 0, 0));

        helper.runAtTickTime(25, () -> {
            assertTrue(helper, maid.getBrain().hasMemoryValue(InitEntities.TARGET_POS),
                    "fast owner movement erased the active work target");
            assertTrue(helper, maid.distanceTo(owner) > 15,
                    "maid teleported to the owner while a work target was active");
            maid.getBrain().eraseMemory(InitEntities.TARGET_POS);
            maid.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        });

        helper.runAtTickTime(55, () -> {
            assertTrue(helper, maid.distanceTo(owner) < 8,
                    "maid did not resume following after the work target ended");
            maid.addTag("tlm_node1_follow_mcp_pass");
            helper.succeed();
        });
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
