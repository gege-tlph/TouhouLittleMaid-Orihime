package com.github.tartaricacid.touhoulittlemaid.ai.agent.tool.implement;

import com.github.tartaricacid.touhoulittlemaid.ai.agent.tool.ITool;
import com.github.tartaricacid.touhoulittlemaid.ai.manager.entity.LLMCallback;
import com.github.tartaricacid.touhoulittlemaid.ai.service.function.schema.parameter.IntegerParameter;
import com.github.tartaricacid.touhoulittlemaid.ai.service.function.schema.parameter.ObjectParameter;
import com.github.tartaricacid.touhoulittlemaid.ai.service.function.schema.parameter.Parameter;
import com.github.tartaricacid.touhoulittlemaid.ai.service.function.schema.parameter.StringParameter;
import com.github.tartaricacid.touhoulittlemaid.api.entity.targeting.MaidTargetingContext;
import com.github.tartaricacid.touhoulittlemaid.api.task.FunctionCallSwitchResult;
import com.github.tartaricacid.touhoulittlemaid.api.task.IAttackTask;
import com.github.tartaricacid.touhoulittlemaid.api.task.IMaidTask;
import com.github.tartaricacid.touhoulittlemaid.entity.ai.targeting.MaidTargetingPolicy;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.entity.task.TaskManager;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.schedule.Activity;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;

public class SwitchWorkTaskTool implements ITool<SwitchWorkTaskTool.Result> {
    public static final String TOOL_ID = "switch_work_task";

    private static final String TOOL_DESC = """
            Use this when the user wants to change the current work task.

            For attack tasks, should first obtain the context of nearby entities, then provide the target entity id as parameter to switch immediately after switching task.
            Non attack tasks not need to provide entity id.

            Reply with the entity name ONLY, omit internal data (e.g., ID, distance).
            """.trim();

    private static final String TASK_ID_PARAMETER_ID = "task_id";
    private static final String ENTITY_ID_PARAMETER_ID = "entity_id";

    private static final String ENTITY_ID_PARAMETER_DESC = "Entity id of the unique attack target from the latest nearby-entity context";

    private static final String SUCCESS = "Switched to task %s";
    private static final String NO_CHANGE = "Already on task %s";
    private static final String MISSING_REQUIRED = "Switched to task %s, but a required item is missing";
    private static final String PARTIAL = "Switched to task %s, but some requirements are missing";
    private static final String SCHEDULED = "Switched to task %s, but current scheduled is %s, not in work time";

    private static final String TARGET_NOT_PROVIDED = "Work task was not changed: no attack target entity id was provided";
    private static final String TARGET_NOT_FOUND = "Work task was not changed: no live living entity with id %d was found";
    private static final String TARGET_NOT_ALLOWED = "Work task was not changed: %s is a protected target (player, pet, ally, or maid) and cannot be attacked";
    private static final String TARGET_SUCCESS = "The task switch succeeded, and the attack target is successfully set to %s";

    private static final Codec<Result> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.fieldOf(TASK_ID_PARAMETER_ID).forGetter(Result::id),
            Codec.INT.optionalFieldOf(ENTITY_ID_PARAMETER_ID, -1).forGetter(Result::entityId)
    ).apply(instance, Result::new));

    @Override
    public String id() {
        return TOOL_ID;
    }

    @Override
    public String summary(EntityMaid maid) {
        return TOOL_DESC;
    }

    @Override
    public Parameter parameters(ObjectParameter root, EntityMaid maid) {
        StringParameter taskId = StringParameter.create()
                .setDescription(this.getTaskIdParameterDesc());
        IntegerParameter entityId = IntegerParameter.create()
                .setDescription(ENTITY_ID_PARAMETER_DESC);

        List<IMaidTask> tasks = TaskManager.getTaskIndex();
        tasks.stream().map(IMaidTask::getUid)
                .map(Identifier::toString)
                .forEach(taskId::addEnumValues);

        root.addProperties(TASK_ID_PARAMETER_ID, taskId);
        root.addProperties(ENTITY_ID_PARAMETER_ID, entityId, false);
        return root;
    }

    @Override
    public Codec<Result> codec() {
        return CODEC;
    }

    @Override
    public LLMCallback onCall(String toolId, Result result, LLMCallback callback) {
        Identifier taskId = result.id;
        List<IMaidTask> tasks = TaskManager.getTaskIndex();
        Optional<IMaidTask> optional = TaskManager.findTask(taskId);

        if (optional.isEmpty()) {
            List<String> values = tasks.stream()
                    .map(IMaidTask::getUid)
                    .map(Identifier::toString)
                    .toList();
            String text = "Unknown task_id '%s'".formatted(taskId);
            return callback.addToolResult(ITool.invalidParam(TASK_ID_PARAMETER_ID, values, text), toolId);
        }

        EntityMaid maid = callback.getMaid();
        IMaidTask task = optional.get();
        IMaidTask currentTask = maid.getTask();
        FunctionCallSwitchResult switchResult;
        int entityId = result.entityId();

        LivingEntity attackTarget = null;
        if (task instanceof IAttackTask) {
            AttackTargetValidation validation = this.validateAttackTarget(maid, entityId);
            if (!validation.valid()) {
                return callback.addToolResult(validation.message(), toolId);
            }
            attackTarget = Objects.requireNonNull(validation.target());
        }

        boolean emergencyStopped = maid.isEmergencyCombatActive();
        maid.getEmergencyCombatManager().onPlayerCommand();

        if (task != currentTask) {
            maid.setTaskWithoutPlayerCommand(task);
        }
        switchResult = task.onFunctionCallSwitch(maid);

        // The permanent task can change outside work time, but execution waits for the schedule.
        Activity activity = maid.getScheduleDetail();
        if (activity != Activity.WORK) {
            String msg = SCHEDULED.formatted(taskId, maid.getSchedule().name());
            return callback.addToolResult(withThreatResult(msg, emergencyStopped), toolId);
        }

        if (attackTarget != null) {
            maid.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, attackTarget);
            // Mark this as an explicit owner order so execution keeps attacking it even if it is a
            // peaceful / non-angry neutral mob that autonomous targeting would otherwise drop.
            maid.getEmergencyCombatManager().setOwnerCommandedAttackTarget(attackTarget.getUUID());
            return callback.addToolResult(withThreatResult(
                    TARGET_SUCCESS.formatted(attackTarget.getName().getString()), emergencyStopped), toolId);
        }

        String msg = this.switchResult(taskId, task == currentTask, switchResult);
        return callback.addToolResult(withThreatResult(msg, emergencyStopped), toolId);
    }

    @Override
    public Component invocationSummaryComponent(Result result) {
        Identifier id = result.id();
        return TaskManager.findTask(id).map(task -> {
            MutableComponent name = task.getName();
            return Component.translatable("ai.touhou_little_maid.chat.tool_call.switch_work_task", name)
                    .withStyle(ChatFormatting.GRAY);
        }).orElse(Component.empty());
    }

    private String switchResult(Identifier taskId, boolean sameTask, FunctionCallSwitchResult switchResult) {
        if (sameTask) {
            return switch (switchResult) {
                case NO_CHANGE -> NO_CHANGE.formatted(taskId);
                case MISSING_REQUIRED_ITEM -> MISSING_REQUIRED.formatted(taskId);
                case PARTIAL_OK -> PARTIAL.formatted(taskId);
                case OK -> SUCCESS.formatted(taskId);
            };
        }
        return switch (switchResult) {
            case NO_CHANGE, OK -> SUCCESS.formatted(taskId);
            case MISSING_REQUIRED_ITEM -> MISSING_REQUIRED.formatted(taskId);
            case PARTIAL_OK -> PARTIAL.formatted(taskId);
        };
    }

    private static String withThreatResult(String result, boolean stopped) {
        return stopped ? result + " Temporary threat response stopped." : result;
    }

    private AttackTargetValidation validateAttackTarget(EntityMaid maid, int entityId) {
        if (entityId == -1) {
            return AttackTargetValidation.invalid(TARGET_NOT_PROVIDED);
        }

        Entity entity = maid.level.getEntity(entityId);
        if (!(entity instanceof LivingEntity target) || !target.isAlive()) {
            return AttackTargetValidation.invalid(TARGET_NOT_FOUND.formatted(entityId));
        }

        // An explicit owner command obeys for any living target except the hard-safety set
        // (players, tamed pets, allies, other maids, protected / ignored types).
        if (!MaidTargetingPolicy.canAttackOnOwnerCommand(maid, target)) {
            return AttackTargetValidation.invalid(TARGET_NOT_ALLOWED.formatted(target.getName().getString()));
        }
        return AttackTargetValidation.valid(target);
    }

    private String getTaskIdParameterDesc() {
        StringJoiner joiner = new StringJoiner("\n", "Brief explanation of parameters: \n", "");
        TaskManager.getTaskIndex().forEach(task -> {
            String path = task.getUid().getPath();
            String summary = task.getMaidActionSummary();
            joiner.add("- %s: %s".formatted(path, summary));
        });
        return joiner.toString();
    }

    public record Result(Identifier id, int entityId) {
    }

    private record AttackTargetValidation(boolean valid, LivingEntity target, String message) {
        private static AttackTargetValidation valid(LivingEntity target) {
            return new AttackTargetValidation(true, target, "");
        }

        private static AttackTargetValidation invalid(String message) {
            return new AttackTargetValidation(false, null, message);
        }
    }
}
