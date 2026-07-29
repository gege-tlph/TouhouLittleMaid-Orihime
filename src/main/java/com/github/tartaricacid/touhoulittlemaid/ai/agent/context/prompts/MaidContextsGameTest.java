package com.github.tartaricacid.touhoulittlemaid.ai.agent.context.prompts;

import com.github.tartaricacid.touhoulittlemaid.ai.agent.context.GameContextRegister;
import com.github.tartaricacid.touhoulittlemaid.ai.manager.entity.UserPromptContexts;
import com.github.tartaricacid.touhoulittlemaid.api.entity.targeting.MaidTargetingContext;
import com.github.tartaricacid.touhoulittlemaid.entity.ai.combat.MaidCombatResponsePolicy;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.init.InitEntities;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("removal")
public class MaidContextsGameTest {
    @GameTest(maxTicks = 100)
    public void scheduledActiveAndPermanentTaskAreIndependentTruths(GameTestHelper helper) {
        EntityMaid maid = helper.spawn(InitEntities.MAID, new BlockPos(1, 2, 1));
        ServerPlayer owner = helper.makeMockServerPlayerInLevel();
        owner.setGameMode(GameType.SURVIVAL);
        BlockPos ownerPos = helper.absolutePos(new BlockPos(2, 2, 1));
        owner.snapTo(ownerPos.getX() + 0.5, ownerPos.getY(), ownerPos.getZ() + 0.5, 0, 0);
        maid.tame(owner);
        helper.getLevel().setDayTime(1000);

        assertContextValue(helper, maid, "scheduled_activity", "work");
        assertContextValue(helper, maid, "work_task", maid.getTask().getUid().toString());
        assertContextValue(helper, maid, "response_policy", "protect_owner");
        assertContextValue(helper, maid, "emergency_state", "inactive");
        assertContextValue(helper, maid, "threat_source", "none");
        // 桌上食物的两个真值是分开的：开关答「能不能」，冷却答「现在为什么不」。
        // 合成一个会让模型把冷却期说成功能被关掉了。
        assertContextValue(helper, maid, "table_food", "allowed");
        assertContextValue(helper, maid, "table_food_cooldown", "ready");
        maid.getConfigManager().setTableFoodAllowed(false);
        assertContextValue(helper, maid, "table_food", "disallowed");
        assertContextValue(helper, maid, "table_food_cooldown", "ready");
        maid.getConfigManager().setTableFoodAllowed(true);

        assertActiveActivity(helper, maid, Activity.WORK, "work");
        assertActiveActivity(helper, maid, Activity.IDLE, "idle");
        assertActiveActivity(helper, maid, Activity.REST, "rest");
        assertActiveActivity(helper, maid, Activity.PANIC, "panic");
        assertActiveActivity(helper, maid, InitEntities.RIDE_WORK, "ride_work");
        assertActiveActivity(helper, maid, InitEntities.RIDE_IDLE, "ride_idle");
        assertActiveActivity(helper, maid, InitEntities.RIDE_REST, "ride_rest");
        String olderSnapshot = UserPromptContexts.addContext(maid, "older request");
        assertContains(helper, olderSnapshot, "- active_activity: ride_rest");

        Zombie threat = helper.spawn(EntityType.ZOMBIE, new BlockPos(4, 2, 1));
        maid.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
        if (!maid.getCombatManager().beginEmergency(threat, MaidTargetingContext.PROTECT_OWNER)) {
            throw helper.assertionException("failed to enter a valid emergency context");
        }
        assertContextValue(helper, maid, "active_activity", "emergency_combat");
        assertContextValue(helper, maid, "emergency_state", "active");
        assertContextValue(helper, maid, "threat_source", "protect_owner");
        maid.getCombatManager().cancelEmergency(false);
        assertContextValue(helper, maid, "emergency_state", "inactive");
        assertContextValue(helper, maid, "threat_source", "none");
        maid.getCombatManager().setResponsePolicy(MaidCombatResponsePolicy.OFF);
        assertContextValue(helper, maid, "response_policy", "off");
        maid.getCombatManager().setResponsePolicy(MaidCombatResponsePolicy.SELF_DEFENSE);
        assertContextValue(helper, maid, "response_policy", "self_defense");
        maid.getCombatManager().setResponsePolicy(MaidCombatResponsePolicy.PROTECT_OWNER);
        assertContextValue(helper, maid, "response_policy", "protect_owner");

        maid.getBrain().setActiveActivityIfPossible(Activity.PANIC);
        String latestSnapshot = UserPromptContexts.addContext(maid, "latest request");
        assertContains(helper, latestSnapshot, "- active_activity: panic");
        assertNotContains(helper, latestSnapshot, "- active_activity: ride_rest");
        assertContains(helper, latestSnapshot, "- scheduled_activity: work");
        assertContains(helper, latestSnapshot, "- work_task: " + maid.getTask().getUid());

        maid.addTag("tlm_l3_context_mcp_pass");
        helper.runAtTickTime(20, helper::succeed);
    }

    private static void assertActiveActivity(GameTestHelper helper, EntityMaid maid,
                                             Activity activity, String expected) {
        maid.getBrain().setActiveActivityIfPossible(activity);
        assertContextValue(helper, maid, "active_activity", expected);
    }

    private static void assertContextValue(GameTestHelper helper, EntityMaid maid,
                                           String key, String expected) {
        List<String> lines = GameContextRegister.getContext(MaidContexts.CATEGORY, maid);
        Map<String, String> values = parseContext(lines);
        String actual = values.get(key);
        if (!expected.equals(actual)) {
            throw helper.assertionException("context %s expected=%s got=%s; lines=%s",
                    key, expected, actual, lines);
        }
    }

    private static Map<String, String> parseContext(List<String> lines) {
        Map<String, String> values = new LinkedHashMap<>();
        for (String line : lines) {
            if (!line.startsWith("- ")) {
                continue;
            }
            int separator = line.indexOf(": ", 2);
            if (separator > 2) {
                values.put(line.substring(2, separator), line.substring(separator + 2));
            }
        }
        return values;
    }

    private static void assertContains(GameTestHelper helper, String text, String expected) {
        if (!text.contains(expected)) {
            throw helper.assertionException("snapshot missing '%s': %s", expected, text);
        }
    }

    private static void assertNotContains(GameTestHelper helper, String text, String unexpected) {
        if (text.contains(unexpected)) {
            throw helper.assertionException("snapshot retained stale '%s': %s", unexpected, text);
        }
    }
}
