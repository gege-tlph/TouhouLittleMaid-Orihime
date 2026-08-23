package com.github.tartaricacid.touhoulittlemaid.init.registry;

import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.github.tartaricacid.touhoulittlemaid.ai.agent.context.GameContextRegister;
import com.github.tartaricacid.touhoulittlemaid.ai.agent.skill.SkillLoader;
import com.github.tartaricacid.touhoulittlemaid.ai.agent.tool.ToolRegister;
import com.github.tartaricacid.touhoulittlemaid.ai.service.SerializerRegister;
import com.github.tartaricacid.touhoulittlemaid.block.multiblock.MultiBlockManager;
import com.github.tartaricacid.touhoulittlemaid.compat.ysm.YsmCompat;
import com.github.tartaricacid.touhoulittlemaid.debug.target.DebugMaidManager;
import com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.ExtraMaidBrainManager;
import com.github.tartaricacid.touhoulittlemaid.entity.ai.edible.MaidEdibleBlockManager;
import com.github.tartaricacid.touhoulittlemaid.entity.ai.fishing.FishingTypeManager;
import com.github.tartaricacid.touhoulittlemaid.entity.backpack.BackpackManager;
//B4_CHATBUBBLE_RESTORE: 取消注释以恢复。阻塞源为真：entity/chatbubble/** 被排除，
//   其 IChatBubbleData 引用 client.renderer.entity.chatbubble.IChatBubbleRenderer（26.1 保持同样耦合）。
//   同一阻塞亦卡住 InitEntities:120。恢复见 CURRENT_STATUS.md 的 B4。
import com.github.tartaricacid.touhoulittlemaid.entity.chatbubble.ChatBubbleRegister;
import com.github.tartaricacid.touhoulittlemaid.entity.data.TaskDataRegister;
import com.github.tartaricacid.touhoulittlemaid.entity.info.ServerCustomPackLoader;
import com.github.tartaricacid.touhoulittlemaid.entity.item.control.BroomControlManager;
import com.github.tartaricacid.touhoulittlemaid.entity.task.TaskManager;
import com.github.tartaricacid.touhoulittlemaid.entity.task.crop.SpecialCropManager;
import com.github.tartaricacid.touhoulittlemaid.entity.task.meal.MaidMealManager;
import com.github.tartaricacid.touhoulittlemaid.item.bauble.BaubleManager;
import com.github.tartaricacid.touhoulittlemaid.util.AnnotatedInstanceUtil;

public final class CommonRegistry {
    public static void onSetupEvent() {
        ServerCustomPackLoader.reloadPacks();
        modApiInit();
        YsmCompat.init();
    }

    private static void modApiInit() {
        TouhouLittleMaid.EXTENSIONS = AnnotatedInstanceUtil.getModExtensions();
        ExtraMaidBrainManager.init();
        TaskManager.init();
        BackpackManager.init();
        BaubleManager.init();
        MultiBlockManager.init();
        MaidMealManager.init();
        TaskDataRegister.init();
        FishingTypeManager.init();
        SerializerRegister.init();
        // FunctionCallRegister.init();
        SkillLoader.init();
        GameContextRegister.init();
        ToolRegister.init();
        //B4_CHATBUBBLE_RESTORE: 取消注释以恢复（顺序：必须留在 ToolRegister 与 DebugMaidManager 之间，与 HEAD/26.1 一致）
        ChatBubbleRegister.init();
        DebugMaidManager.init();
        BroomControlManager.init();
        SpecialCropManager.init();
        MaidEdibleBlockManager.init();
    }
}
