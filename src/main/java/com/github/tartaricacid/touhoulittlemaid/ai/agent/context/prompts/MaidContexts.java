package com.github.tartaricacid.touhoulittlemaid.ai.agent.context.prompts;

import com.github.tartaricacid.touhoulittlemaid.ai.agent.context.AbstractMaidContext;
import com.github.tartaricacid.touhoulittlemaid.ai.agent.context.GameContextRegister;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.schedule.Activity;

import static com.github.tartaricacid.touhoulittlemaid.ai.manager.setting.papi.StringConstant.HEALTHY_FORMAT;

public final class MaidContexts {
    public static final String CATEGORY = "status";
    private static final String SUMMARY = "Self some status";

    private MaidContexts() {
    }

    public static void registerAll(GameContextRegister register) {
        register.registerCategory(CATEGORY, SUMMARY, true);
        register.registerContext(CATEGORY, new MaidHealthContext());
        register.registerContext(CATEGORY, new MaidIsSleepContext());
        register.registerContext(CATEGORY, new FollowStateContext());
        register.registerContext(CATEGORY, new SittingContext());
        register.registerContext(CATEGORY, new RideContext());
        register.registerContext(CATEGORY, new ScheduleModeContext());
        register.registerContext(CATEGORY, new ScheduledActivityContext());
        register.registerContext(CATEGORY, new ActiveActivityContext());
        register.registerContext(CATEGORY, new ResponsePolicyContext());
        register.registerContext(CATEGORY, new EmergencyStateContext());
        register.registerContext(CATEGORY, new ThreatSourceContext());
        register.registerContext(CATEGORY, new CurrentTaskContext());
        // table_food / table_food_cooldown 两个上下文随审计 §3.I 一起补，见下方恢复锚点
    }

    private static final class MaidHealthContext extends AbstractMaidContext {
        private MaidHealthContext() {
            super("healthy", "Self health");
        }

        @Override
        public String getValue(EntityMaid maid) {
            float maxHealth = maid.getMaxHealth();
            float health = maid.getHealth();
            return HEALTHY_FORMAT.formatted(health, maxHealth);
        }
    }

    private static final class MaidIsSleepContext extends AbstractMaidContext {
        private MaidIsSleepContext() {
            super("sleep_state", "Is sleeping");
        }

        @Override
        public String getValue(EntityMaid maid) {
            return maid.isSleeping() ? "yes" : "no";
        }
    }

    private static final class FollowStateContext extends AbstractMaidContext {
        private FollowStateContext() {
            super("follow_state", "Is following");
        }

        @Override
        public String getValue(EntityMaid maid) {
            return maid.isHomeModeEnable() ? "no" : "yes";
        }
    }

    private static final class SittingContext extends AbstractMaidContext {
        private SittingContext() {
            super("sitting", "Sitting");
        }

        @Override
        public String getValue(EntityMaid maid) {
            return maid.isMaidInSittingPose() ? "yes" : "no";
        }
    }

    private static final class RideContext extends AbstractMaidContext {
        private RideContext() {
            super("riding", "Is riding");
        }

        @Override
        public String getValue(EntityMaid maid) {
            Entity vehicle = maid.getVehicle();
            if (vehicle == null) {
                return "not";
            }
            Identifier type = BuiltInRegistries.ENTITY_TYPE.getKey(vehicle.getType());
            if (type == null) {
                return "not";
            }
            return "riding %s".formatted(type);
        }
    }

    private static final class ScheduleModeContext extends AbstractMaidContext {
        private ScheduleModeContext() {
            super("schedule", "Schedule");
        }

        @Override
        public String getValue(EntityMaid maid) {
            return switch (maid.getSchedule()) {
                case DAY -> "DAY";
                case NIGHT -> "NIGHT";
                case ALL -> "ALL";
            };
        }
    }

    private static final class ScheduledActivityContext extends AbstractMaidContext {
        private ScheduledActivityContext() {
            super("scheduled_activity", "scheduled_activity");
        }

        @Override
        public String getValue(EntityMaid maid) {
            return canonicalActivityName(maid.getScheduleDetail());
        }
    }

    private static final class ActiveActivityContext extends AbstractMaidContext {
        private ActiveActivityContext() {
            super("active_activity", "active_activity");
        }

        @Override
        public String getValue(EntityMaid maid) {
            return maid.getBrain().getActiveNonCoreActivity()
                    .map(MaidContexts::canonicalActivityName)
                    .orElse("none");
        }
    }

    private static final class CurrentTaskContext extends AbstractMaidContext {
        private CurrentTaskContext() {
            super("work_task", "work_task");
        }

        @Override
        public String getValue(EntityMaid maid) {
            return maid.getTask().getUid().toString();
        }
    }

    private static final class EmergencyStateContext extends AbstractMaidContext {
        private EmergencyStateContext() {
            super("emergency_state", "emergency_state");
        }

        @Override
        public String getValue(EntityMaid maid) {
            return maid.isEmergencyCombatActive() ? "active" : "inactive";
        }
    }

    /*
     * ⚠️ 恢复锚点（审计 §3.I 桌上食物）：行为基准这里还有两个上下文——
     * {@code table_food}（开关：她能不能吃）与 {@code table_food_cooldown}（冷却：她现在为什么不吃）。
     * 两个是分开的真值，合并会让模型把冷却期说成「功能被关了」。
     *
     * 本分支**尚未移植 §3.I**：{@code MaidConfigManager} 上没有 isTableFoodAllowed/setTableFoodAllowed，
     * 桌上食物奖励与冷却整块都不在。此处不预留空壳——装了返回常量的上下文，模型会拿它当事实说出去，
     * 比没有更糟。§3.I 那一刀落地时连同 {@code MaidContextsGameTest} 里对应的三条断言一起补回。
     */

    private static final class ResponsePolicyContext extends AbstractMaidContext {
        private ResponsePolicyContext() {
            super("response_policy", "response_policy");
        }

        @Override
        public String getValue(EntityMaid maid) {
            return maid.getEmergencyCombatManager().getResponsePolicy().name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    private static final class ThreatSourceContext extends AbstractMaidContext {
        private ThreatSourceContext() {
            super("threat_source", "threat_source");
        }

        @Override
        public String getValue(EntityMaid maid) {
            if (!maid.isEmergencyCombatActive()) {
                return "none";
            }
            return switch (maid.getEmergencyCombatManager().getTargetingContext()) {
                case SELF_DEFENSE -> "self_defense";
                case PROTECT_OWNER -> "protect_owner";
                case PLANNED_ATTACK -> "none";
            };
        }
    }

    private static String canonicalActivityName(Activity activity) {
        String name = activity.getName();
        return name.startsWith("tlm_") ? name.substring("tlm_".length()) : name;
    }
}
