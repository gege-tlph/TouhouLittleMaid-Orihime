package com.github.tartaricacid.touhoulittlemaid.ai.agent.tool.implement;

import com.github.tartaricacid.touhoulittlemaid.ai.manager.entity.LLMCallback;
import com.github.tartaricacid.touhoulittlemaid.ai.service.llm.LLMMessage;
import com.github.tartaricacid.touhoulittlemaid.api.task.IMaidTask;
import com.github.tartaricacid.touhoulittlemaid.api.entity.targeting.MaidTargetingContext;
import com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.MaidSchedule;
import cn.sh1rocu.touhoulittlemaid.util.transfer.ItemUtil;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.entity.task.TaskAttack;
import com.github.tartaricacid.touhoulittlemaid.entity.task.TaskManager;
import com.github.tartaricacid.touhoulittlemaid.init.InitEntities;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

import java.util.ArrayList;
import java.util.List;

public class MaidControlToolsGameTest {
    private static final String CALL_ID = "gametest-call";

    @GameTest(maxTicks = 100)
    public void missingAttackTargetLeavesEveryGameplayFieldUnchanged(GameTestHelper helper) {
        EntityMaid maid = preparedMaid(helper);
        Zombie previousTarget = helper.spawn(EntityType.ZOMBIE, new BlockPos(4, 2, 1));
        installPreviousCombatState(maid, previousTarget);

        String result = invokeWorkTool(maid, new SwitchWorkTaskTool.Result(TaskAttack.UID, -1));

        assertInvalidAttackIsAtomic(helper, maid, previousTarget, result, "no attack target");
        helper.succeed();
    }

    @GameTest(maxTicks = 100)
    public void missingAttackEntityLeavesEveryGameplayFieldUnchanged(GameTestHelper helper) {
        EntityMaid maid = preparedMaid(helper);
        Zombie previousTarget = helper.spawn(EntityType.ZOMBIE, new BlockPos(4, 2, 1));
        installPreviousCombatState(maid, previousTarget);

        String result = invokeWorkTool(maid, new SwitchWorkTaskTool.Result(TaskAttack.UID, Integer.MAX_VALUE));

        assertInvalidAttackIsAtomic(helper, maid, previousTarget, result, "no live living entity");
        helper.succeed();
    }

    @GameTest(maxTicks = 100)
    public void forbiddenTamedTargetLeavesEveryGameplayFieldUnchanged(GameTestHelper helper) {
        EntityMaid maid = preparedMaid(helper);
        Zombie previousTarget = helper.spawn(EntityType.ZOMBIE, new BlockPos(4, 2, 1));
        installPreviousCombatState(maid, previousTarget);

        Player owner = helper.makeMockPlayer(GameType.CREATIVE);
        Wolf forbiddenTarget = helper.spawn(EntityType.WOLF, new BlockPos(3, 2, 3));
        forbiddenTarget.tame(owner);

        String result = invokeWorkTool(maid,
                new SwitchWorkTaskTool.Result(TaskAttack.UID, forbiddenTarget.getId()));

        // 609546e1 放宽了工具拒绝文案（SwitchWorkTaskTool.TARGET_NOT_ALLOWED），断言当时漏改，
        // GameTest 自那次提交起一直红着。这里对齐现行文案的稳定片段，不再锁整句。
        assertInvalidAttackIsAtomic(helper, maid, previousTarget, result, "is a protected target");
        helper.succeed();
    }

    @GameTest(maxTicks = 100)
    public void validHostileTargetCommitsWithoutForgingDamageHistory(GameTestHelper helper) {
        EntityMaid maid = preparedMaid(helper);
        Zombie previousDamageSource = helper.spawn(EntityType.ZOMBIE, new BlockPos(4, 2, 1));
        Zombie target = helper.spawn(EntityType.ZOMBIE, new BlockPos(3, 2, 3));
        maid.setLastHurtByMob(previousDamageSource);

        String result = invokeWorkTool(maid, new SwitchWorkTaskTool.Result(TaskAttack.UID, target.getId()));

        assertSame(helper, TaskAttack.UID.toString(), maid.getTask().getUid().toString(),
                "valid target did not commit the permanent attack task");
        LivingEntity actualTarget = maid.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET)
                .orElseThrow(() -> helper.assertionException("valid target did not commit ATTACK_TARGET"));
        if (actualTarget != target) {
            throw helper.assertionException("ATTACK_TARGET changed to an unexpected entity");
        }
        if (maid.getLastHurtByMob() != previousDamageSource) {
            throw helper.assertionException("valid target forged maid damage history");
        }
        assertItem(helper, maid.getMainHandItem(), Items.IRON_SWORD,
                "valid attack task did not run its existing weapon-equipping hook");
        assertContains(helper, result, "successfully set");
        maid.addTag("tlm_l4_tool_transaction_mcp_pass");
        helper.succeed();
    }

    @GameTest(maxTicks = 100)
    public void persistentControlToolsReportAndCommitTheirOwnState(GameTestHelper helper) {
        EntityMaid maid = helper.spawn(InitEntities.MAID, new BlockPos(1, 2, 1));

        maid.setHomeModeEnable(true);
        String followEnabled = invokeFollowTool(maid, true);
        assertFalse(helper, maid.isHomeModeEnable(), "follow=true did not disable persistent home mode");
        assertContains(helper, followEnabled, "enabled");
        String alreadyFollowing = invokeFollowTool(maid, true);
        assertContains(helper, alreadyFollowing, "Already following");

        String followDisabled = invokeFollowTool(maid, false);
        assertTrue(helper, maid.isHomeModeEnable(), "follow=false did not enable persistent home mode");
        assertSame(helper, maid.blockPosition(), maid.getHomePosition(),
                "follow=false did not commit the current position as home");
        assertContains(helper, followDisabled, "disabled");

        assertContains(helper, invokeSitTool(maid, true), "Success sitting");
        assertTrue(helper, maid.isMaidInSittingPose(), "sit=true did not commit the sitting state");
        assertContains(helper, invokeSitTool(maid, true), "Already sitting");
        assertContains(helper, invokeSitTool(maid, false), "Success standing");
        assertFalse(helper, maid.isMaidInSittingPose(), "sit=false did not commit the standing state");

        maid.setSchedule(MaidSchedule.DAY);
        String invalidSchedule = invokeScheduleTool(maid, "INVALID");
        assertSame(helper, MaidSchedule.DAY, maid.getSchedule(), "invalid schedule mutated persistent state");
        assertContains(helper, invalidSchedule, "Invalid parameter");
        assertContains(helper, invokeScheduleTool(maid, "ALL"), "switched to ALL");
        assertSame(helper, MaidSchedule.ALL, maid.getSchedule(), "valid schedule did not commit");

        helper.succeed();
    }

    @GameTest(maxTicks = 100)
    public void persistentToolsClearTemporaryThreatResponseEvenWhenStateIsUnchanged(GameTestHelper helper) {
        Zombie threat = helper.spawn(EntityType.ZOMBIE, new BlockPos(7, 2, 7));
        threat.setNoAi(true);

        EntityMaid followMaid = emergencyMaid(helper, new BlockPos(1, 2, 1), threat);
        assertContains(helper, invokeFollowTool(followMaid, true), "Temporary threat response stopped");
        assertFalse(helper, followMaid.isEmergencyCombatActive(), "same follow state retained emergency combat");
        assertFalse(helper, followMaid.getEmergencyCombatManager().beginEmergency(threat, MaidTargetingContext.SELF_DEFENSE),
                "follow command did not suppress immediate threat reentry");

        EntityMaid sitMaid = emergencyMaid(helper, new BlockPos(1, 2, 3), threat);
        assertContains(helper, invokeSitTool(sitMaid, false), "Temporary threat response stopped");
        assertFalse(helper, sitMaid.isEmergencyCombatActive(), "same standing state retained emergency combat");

        EntityMaid scheduleMaid = emergencyMaid(helper, new BlockPos(1, 2, 5), threat);
        assertContains(helper, invokeScheduleTool(scheduleMaid, scheduleMaid.getSchedule().name()),
                "Temporary threat response stopped");
        assertFalse(helper, scheduleMaid.isEmergencyCombatActive(), "same schedule retained emergency combat");

        EntityMaid workMaid = emergencyMaid(helper, new BlockPos(3, 2, 1), threat);
        assertContains(helper, invokeWorkTool(workMaid,
                        new SwitchWorkTaskTool.Result(TaskManager.getIdleTask().getUid(), -1)),
                "Temporary threat response stopped");
        assertFalse(helper, workMaid.isEmergencyCombatActive(), "same work task retained emergency combat");
        helper.succeed();
    }

    private static EntityMaid preparedMaid(GameTestHelper helper) {
        EntityMaid maid = helper.spawn(InitEntities.MAID, new BlockPos(1, 2, 1));
        maid.setSchedule(MaidSchedule.ALL);
        maid.setTask(TaskManager.getIdleTask());
        maid.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
        // 宿主的女仆背包走 Fabric transfer：写入用 set(slot, ItemVariant, count)
        maid.getMaidInv().set(0, ItemVariant.of(Items.IRON_SWORD), 1);
        return maid;
    }

    private static EntityMaid emergencyMaid(GameTestHelper helper, BlockPos pos, Zombie threat) {
        EntityMaid maid = helper.spawn(InitEntities.MAID, pos);
        maid.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
        assertTrue(helper, maid.getEmergencyCombatManager().beginEmergency(
                threat, MaidTargetingContext.SELF_DEFENSE), "failed to prepare emergency combat state");
        return maid;
    }

    private static void installPreviousCombatState(EntityMaid maid, Zombie previousTarget) {
        maid.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, previousTarget);
        maid.setLastHurtByMob(previousTarget);
    }

    private static void assertInvalidAttackIsAtomic(GameTestHelper helper, EntityMaid maid,
                                                    Zombie previousTarget, String result,
                                                    String expectedMessage) {
        IMaidTask currentTask = maid.getTask();
        if (currentTask != TaskManager.getIdleTask()) {
            throw helper.assertionException("invalid attack target changed permanent task to %s",
                    currentTask.getUid());
        }
        LivingEntity actualTarget = maid.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET)
                .orElseThrow(() -> helper.assertionException("invalid attack target erased ATTACK_TARGET"));
        if (actualTarget != previousTarget) {
            throw helper.assertionException("invalid attack target replaced ATTACK_TARGET");
        }
        if (maid.getLastHurtByMob() != previousTarget) {
            throw helper.assertionException("invalid attack target changed damage history");
        }
        assertItem(helper, maid.getMainHandItem(), Items.STICK,
                "invalid attack target changed the main hand");
        // 读出的是拷贝，比内容不能比引用
        assertItem(helper, ItemUtil.getStack(maid.getMaidInv(), 0), Items.IRON_SWORD,
                "invalid attack target changed the backpack");
        assertContains(helper, result, expectedMessage);
        assertContains(helper, result, "not changed");
    }

    private static String invokeWorkTool(EntityMaid maid, SwitchWorkTaskTool.Result result) {
        List<LLMMessage> messages = new ArrayList<>();
        LLMCallback callback = callback(maid, messages);
        new SwitchWorkTaskTool().onCall(CALL_ID, result, callback);
        return messages.getLast().message();
    }

    private static String invokeFollowTool(EntityMaid maid, boolean follow) {
        List<LLMMessage> messages = new ArrayList<>();
        new SwitchFollowStateTool().onCall(CALL_ID, new SwitchFollowStateTool.Result(follow),
                callback(maid, messages));
        return messages.getLast().message();
    }

    private static String invokeSitTool(EntityMaid maid, boolean sit) {
        List<LLMMessage> messages = new ArrayList<>();
        new SwitchSitTool().onCall(CALL_ID, new SwitchSitTool.Result(sit), callback(maid, messages));
        return messages.getLast().message();
    }

    private static String invokeScheduleTool(EntityMaid maid, String schedule) {
        List<LLMMessage> messages = new ArrayList<>();
        new SwitchScheduleTool().onCall(CALL_ID, schedule, callback(maid, messages));
        return messages.getLast().message();
    }

    private static LLMCallback callback(EntityMaid maid, List<LLMMessage> messages) {
        return new LLMCallback(maid.getAiChatManager(), messages, true);
    }

    private static void assertItem(GameTestHelper helper, ItemStack stack,
                                   net.minecraft.world.item.Item expected, String message) {
        if (!stack.is(expected) || stack.getCount() != 1) {
            throw helper.assertionException("%s: got %s", message, stack);
        }
    }

    private static void assertContains(GameTestHelper helper, String actual, String expected) {
        if (!actual.contains(expected)) {
            throw helper.assertionException("tool result missing '%s': %s", expected, actual);
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

    private static void assertSame(GameTestHelper helper, Object expected, Object actual, String message) {
        if (!expected.equals(actual)) {
            throw helper.assertionException("%s: expected=%s got=%s", message, expected, actual);
        }
    }
}
