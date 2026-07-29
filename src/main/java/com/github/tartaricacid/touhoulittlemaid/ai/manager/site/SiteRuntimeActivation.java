package com.github.tartaricacid.touhoulittlemaid.ai.manager.site;

import com.github.tartaricacid.touhoulittlemaid.command.subcommand.AIChatCommand;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * 站点保存后立即换运行时快照，专服与集成服务器一视同仁。
 *
 * <p>专服上原本只写文件、要求管理员再跑一次 reload。那一步曾是「服务器 STT 不可用」那个 P0 的帮凶：
 * 管理员漏跑时服务端仍在用旧站点，而玩家侧看到的提示完全指不到这里。既然保存本身已经过权限校验，
 * 就没有理由再要求同一个人补一条命令。</p>
 *
 * <p>{@code AIChatCommand.reload} 内部会重载站点与技能并同步给客户端；它返回 false 表示<b>某个</b>
 * 站点文件加载失败——未必是刚保存的这份，所以提示只说「部分站点文件」并指向日志。</p>
 *
 * <p><b>这个方法原本住在 {@code SaveSTTSitePacket} 里。</b>「服务器提供 STT」整套功能撤除时，
 * 那个类要整体删除，而 LLM 与 TTS 的保存包都在调它——寄居在被删的类里会让删除操作静默打断
 * 另外两条保存链（编译照样通过）。故独立成类，不再依附于任何一种服务。</p>
 */
public final class SiteRuntimeActivation {
    private SiteRuntimeActivation() {
    }

    public static void activate(ServerPlayer player) {
        if (!AIChatCommand.reloadSites(player.level().getServer())) {
            player.displayClientMessage(Component.translatable(
                            "config.touhou_little_maid.ai_sites.save.reload_partial")
                    .withStyle(ChatFormatting.YELLOW), false);
        }
    }
}
