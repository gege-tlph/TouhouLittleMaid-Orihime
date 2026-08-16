package com.github.tartaricacid.touhoulittlemaid.ai.agent.tool.implement;

import com.github.tartaricacid.touhoulittlemaid.ai.agent.tool.ITool;
import com.github.tartaricacid.touhoulittlemaid.ai.manager.entity.LLMCallback;
import com.github.tartaricacid.touhoulittlemaid.ai.service.function.schema.parameter.BoolParameter;
import com.github.tartaricacid.touhoulittlemaid.ai.service.function.schema.parameter.ObjectParameter;
import com.github.tartaricacid.touhoulittlemaid.ai.service.function.schema.parameter.Parameter;
import com.github.tartaricacid.touhoulittlemaid.config.subconfig.MaidConfig;
import com.github.tartaricacid.touhoulittlemaid.config.ServerRuleConfig;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

public class SwitchFollowStateTool implements ITool<SwitchFollowStateTool.Result> {
    public static final String TOOL_ID = "switch_follow_state";

    private static final String TOOL_DESC = """
            Use this for an explicit player command that changes the maid's persistent follow/home state.
            Set follow=true to follow the user, set follow=false to stop following and enable the maid's home/schedule positions.
            Do not use this to make the maid sit down.
            Do not use this to represent temporary emergency movement or combat behavior.
            A valid explicit command stops any current temporary threat response.
            """.trim();

    private static final String FOLLOW_PARAM_ID = "follow";

    private static final Codec<Result> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(Codec.BOOL.fieldOf(FOLLOW_PARAM_ID).forGetter(Result::follow))
                    .apply(instance, Result::new));

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
        BoolParameter follow = BoolParameter.create();
        root.addProperties(FOLLOW_PARAM_ID, follow);
        return root;
    }

    @Override
    public Codec<Result> codec() {
        return CODEC;
    }

    @Override
    public LLMCallback onCall(String toolId, Result result, LLMCallback callback) {
        EntityMaid maid = callback.getMaid();
        boolean emergencyStopped = maid.isEmergencyCombatActive();
        maid.getEmergencyCombatManager().onPlayerCommand();
        boolean toFollow = result.follow;
        boolean isHome = maid.isHomeModeEnable();
        if (toFollow) {
            if (!isHome) {
                return callback.addToolResult(withThreatResult("Already following the owner", emergencyStopped), toolId);
            }
            maid.setHomeTo(BlockPos.ZERO, ServerRuleConfig.get(MaidConfig.MAID_NON_HOME_RANGE));
            maid.setHomeModeEnable(false);
            return callback.addToolResult(withThreatResult("Follow mode enabled", emergencyStopped), toolId);
        }

        if (isHome) {
            return callback.addToolResult(withThreatResult("Already stop following", emergencyStopped), toolId);
        }
        maid.setHomeModeEnable(true);
        maid.getSchedulePos().setHomeModeEnable(maid, maid.blockPosition());
        return callback.addToolResult(withThreatResult("Follow mode disabled", emergencyStopped), toolId);
    }

    private static String withThreatResult(String result, boolean stopped) {
        return stopped ? result + ". Temporary threat response stopped." : result;
    }

    @Override
    public Component invocationSummaryComponent(Result result) {
        if (result.follow()) {
            return Component.translatable("ai.touhou_little_maid.chat.tool_call.switch_follow_state.yes")
                    .withStyle(ChatFormatting.GRAY);
        } else {
            return Component.translatable("ai.touhou_little_maid.chat.tool_call.switch_follow_state.no")
                    .withStyle(ChatFormatting.GRAY);
        }
    }

    public record Result(boolean follow) {
    }
}
