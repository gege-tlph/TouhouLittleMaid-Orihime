package com.github.tartaricacid.touhoulittlemaid.ai.agent.tool;

import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.github.tartaricacid.touhoulittlemaid.ai.agent.tool.implement.*;
import com.github.tartaricacid.touhoulittlemaid.api.ILittleMaid;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import javax.annotation.Nullable;
import java.util.Map;

public class ToolRegister {
    private static Map<String, ITool<?>> TOOLS = Maps.newLinkedHashMap();

    public static void init() {
        ToolRegister register = new ToolRegister();

        register.register(new UseSkillTool());
        register.register(new QueryMinecraftWikiTool());
        register.register(new QueryGameContextTool());
        register.register(new SwitchFollowStateTool());
        register.register(new SwitchWorkTaskTool());
        register.register(new SwitchScheduleTool());
        register.register(new SwitchSitTool());

        // SWEEP R9-1（2026-07-19）：原「EXTENSIONS not available (26.1 feature)」TODO 系误判——
        // TouhouLittleMaid.EXTENSIONS(:21) 本树存在且他处在用；还原 origin 的 addon 扩展点循环
        for (ILittleMaid littleMaid : TouhouLittleMaid.EXTENSIONS) {
            littleMaid.registerAITool(register);
        }

        TOOLS = ImmutableMap.copyOf(TOOLS);
    }

    public void register(ITool<?> skill) {
        TOOLS.put(skill.id(), skill);
    }

    @Nullable
    public static ITool<?> getTool(String name) {
        return TOOLS.get(name);
    }

    public static Map<String, ITool<?>> getAllTools() {
        return TOOLS;
    }
}
