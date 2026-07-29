package com.github.tartaricacid.touhoulittlemaid.network.client.ai;

import com.github.tartaricacid.touhoulittlemaid.client.gui.entity.maid.ai.editor.LLMSiteEditorScreen;
import com.github.tartaricacid.touhoulittlemaid.client.gui.entity.maid.ai.editor.TTSSiteEditorScreen;
import com.github.tartaricacid.touhoulittlemaid.client.gui.entity.maid.ai.settings.AIChatSettingsHubScreen;
import com.github.tartaricacid.touhoulittlemaid.network.message.ai.SyncAISitesPacket;
import com.github.tartaricacid.touhoulittlemaid.util.migrate.ScreenUtil;

public final class SyncAISitesPacketProxy {
    public static void handle(SyncAISitesPacket message) {
        if (ScreenUtil.getScreen() instanceof LLMSiteEditorScreen editor) {
            editor.getParentHub().reopenSelf(message.llmSites(), message.ttsSites());
        } else if (ScreenUtil.getScreen() instanceof TTSSiteEditorScreen editor) {
            editor.getParentHub().reopenSelf(message.llmSites(), message.ttsSites());
        } else if (ScreenUtil.getScreen() instanceof AIChatSettingsHubScreen hubScreen) {
            hubScreen.reopenSelf(message.llmSites(), message.ttsSites());
        } else if (!message.openScreen()) {
            return;
        } else {
            // 当前屏（T 屏 / Cloth 菜单 / null）就是返回目标；落在哪个标签由 openDefault 按权限决定
            ScreenUtil.setScreen(AIChatSettingsHubScreen.openDefault(
                    ScreenUtil.getScreen(), message.llmSites(), message.ttsSites(), message.insufficientPermissions()));
        }
    }
}
