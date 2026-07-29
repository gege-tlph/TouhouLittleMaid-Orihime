package com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.task;

import com.github.tartaricacid.touhoulittlemaid.entity.ai.edible.MaidEdibleBlockAction;
import com.github.tartaricacid.touhoulittlemaid.entity.favorability.FavorabilityManager;
import com.github.tartaricacid.touhoulittlemaid.entity.favorability.Type;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.init.InitEntities;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

/**
 * 桌上食物最终规则（docs/TABLE_FOOD_DESIGN.md）的确定性门禁。
 *
 * <p>上游行为是「每约 45 秒可再吃一口，但三分钟内只有第一次给 1 点好感度」，于是玩家会遇到
 * 「食物被吃光却没有奖励」。本项目按用户确认改为：每次成功偷吃必给 1~3 点好感度，随后三分钟内
 * 不再消耗桌上食物。冷却复用 {@link Type#STEAL_EDIBLE_BLOCK}，因此天然随
 * {@code FavorabilityManagerCounter} 持久化。
 */
public class MaidTableFoodGameTest {
    private static final BlockPos MAID_POS = new BlockPos(1, 2, 1);
    /** {@link Type#STEAL_EDIBLE_BLOCK} 的冷却长度，正常 TPS 下三分钟。 */
    private static final int STEAL_COOLDOWN_TICKS = 3 * 60 * 20;

    @GameTest(maxTicks = 100)
    public void successfulStealBlocksFurtherStealingForThreeMinutes(GameTestHelper helper) {
        EntityMaid maid = prepareMaid(helper);
        FavorabilityManager favorability = maid.getFavorabilityManager();

        assertTrue(helper, MaidStealEdibleUseTask.canSteal(maid),
                "a fresh maid should be allowed to eat placed food");

        // 模拟一次成功偷吃：奖励与冷却由同一次 apply 写入。
        favorability.apply(Type.STEAL_EDIBLE_BLOCK, MaidStealEdibleUseTask.rollFavorabilityPoints(
                helper.getLevel().getRandom()));

        assertTrue(helper, !MaidStealEdibleUseTask.canSteal(maid),
                "a maid still inside the steal cooldown must not consume more placed food");
        assertTrue(helper, maid.getConfigManager().isTableFoodAllowed(),
                "the steal cooldown must not be confused with the player-facing table food switch");

        for (int tick = 1; tick < STEAL_COOLDOWN_TICKS; tick++) {
            favorability.tick();
        }
        assertTrue(helper, !MaidStealEdibleUseTask.canSteal(maid),
                "the steal cooldown ended before its full three minutes elapsed");

        favorability.tick();
        assertTrue(helper, MaidStealEdibleUseTask.canSteal(maid),
                "the maid is still blocked after the full steal cooldown elapsed");

        maid.addTag("tlm_table_food_cooldown_mcp_pass");
        helper.succeed();
    }

    @GameTest(maxTicks = 100)
    public void stealCooldownSurvivesSaveAndLoad(GameTestHelper helper) {
        EntityMaid maid = prepareMaid(helper);
        maid.getFavorabilityManager().apply(Type.STEAL_EDIBLE_BLOCK, 1);
        assertTrue(helper, !MaidStealEdibleUseTask.canSteal(maid),
                "fixture failed to put the maid into the steal cooldown");

        EntityMaid reloaded = loadMaid(helper, saveMaid(helper, maid));
        assertTrue(helper, !MaidStealEdibleUseTask.canSteal(reloaded),
                "the steal cooldown was lost across a save/load round trip");

        // 冷却走完之后再存读，不得错误地复活旧冷却。
        for (int tick = 0; tick < STEAL_COOLDOWN_TICKS; tick++) {
            maid.getFavorabilityManager().tick();
        }
        EntityMaid expired = loadMaid(helper, saveMaid(helper, maid));
        assertTrue(helper, MaidStealEdibleUseTask.canSteal(expired),
                "an elapsed steal cooldown came back after a save/load round trip");

        maid.addTag("tlm_table_food_persist_mcp_pass");
        helper.succeed();
    }

    @GameTest(maxTicks = 100)
    public void stealRewardIsAlwaysOneToThreePoints(GameTestHelper helper) {
        RandomSource random = helper.getLevel().getRandom();
        boolean[] seen = new boolean[4];

        for (int roll = 0; roll < 600; roll++) {
            int points = MaidStealEdibleUseTask.rollFavorabilityPoints(random);
            assertTrue(helper, points >= 1 && points <= 3,
                    "steal reward left the 1..3 range: " + points);
            seen[points] = true;
        }
        assertTrue(helper, seen[1] && seen[2] && seen[3],
                "steal reward never produced the full 1..3 range");

        helper.succeed();
    }

    /**
     * 偷吃/摆盘持有 {@code TARGET_POS} 超时后必须让位给工作行为。
     *
     * <p>偷吃注册在优先级 8，工作行为在 5~6 且同样要求 {@code TARGET_POS} 为空，因此偷吃一旦先取得
     * 目标，工作行为连判据都不会被调用。这里断言超时释放确实发生，且 {@code WALK_TARGET} 一并清除
     * ——只清 {@code TARGET_POS} 的话女仆会继续走向食物，工作依旧启动不了，等于没让位。</p>
     */
    @GameTest(maxTicks = 100)
    public void stealYieldsItsTargetAfterHoldingTooLong(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        EntityMaid maid = prepareMaid(helper);
        Brain<EntityMaid> brain = maid.getBrain();
        MaidStealEdibleUseTask task = new MaidStealEdibleUseTask(2);

        // 目标远在天边，但 WALK_TARGET 正指着它：既有的「太远」分支据此**不**释放，
        // 于是这条用例只可能因为超时而通过，不会被别的释放路径蒙混过去。
        BlockPos target = helper.absolutePos(MAID_POS).offset(12, 0, 0);
        armHold(brain, target, level.getGameTime() + 200);

        assertTrue(helper, !task.checkExtraStartConditions(level, maid),
                "the maid ate food it had not reached yet");
        assertTrue(helper, brain.hasMemoryValue(InitEntities.TARGET_POS),
                "an unexpired hold dropped its target");
        assertTrue(helper, brain.hasMemoryValue(MemoryModuleType.WALK_TARGET),
                "an unexpired hold dropped its walk target");

        // 让持有到期
        brain.setMemory(InitEntities.MAID_EDIBLE_HOLD_EXPIRY, level.getGameTime());

        assertTrue(helper, !task.checkExtraStartConditions(level, maid),
                "an expired hold must not also start eating");
        assertTrue(helper, !brain.hasMemoryValue(InitEntities.TARGET_POS),
                "an expired hold kept TARGET_POS, so work behaviours stay unreachable");
        assertTrue(helper, !brain.hasMemoryValue(MemoryModuleType.WALK_TARGET),
                "an expired hold kept WALK_TARGET, which work behaviours also require absent");
        assertTrue(helper, !brain.hasMemoryValue(InitEntities.MAID_EDIBLE_HOLD_EXPIRY),
                "the expiry marker leaked after the hold was released");

        maid.addTag("tlm_table_food_yield_mcp_pass");
        helper.succeed();
    }

    private static void armHold(Brain<EntityMaid> brain, BlockPos target, long expiry) {
        brain.setMemory(InitEntities.MAID_EDIBLE_BLOCK_ACTION, MaidEdibleBlockAction.TRY_STEAL);
        brain.setMemory(InitEntities.TARGET_POS, new BlockPosTracker(target));
        brain.setMemory(MemoryModuleType.WALK_TARGET,
                new WalkTarget(new BlockPosTracker(target), 0.6f, 0));
        brain.setMemory(InitEntities.MAID_EDIBLE_HOLD_EXPIRY, expiry);
    }

    private static EntityMaid prepareMaid(GameTestHelper helper) {
        EntityMaid maid = helper.spawn(InitEntities.MAID, MAID_POS);
        maid.tame(helper.makeMockServerPlayerInLevel());
        return maid;
    }

    private static CompoundTag saveMaid(GameTestHelper helper, EntityMaid maid) {
        TagValueOutput output = TagValueOutput.createWithContext(
                ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
        maid.saveWithoutId(output);
        return output.buildResult();
    }

    private static EntityMaid loadMaid(GameTestHelper helper, CompoundTag tag) {
        EntityMaid maid = InitEntities.MAID.create(helper.getLevel(), EntitySpawnReason.LOAD);
        if (maid == null) {
            throw helper.assertionException("failed to construct maid reload fixture");
        }
        maid.load(TagValueInput.create(
                ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), tag));
        return maid;
    }

    private static void assertTrue(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            throw helper.assertionException(message);
        }
    }
}
