package com.github.tartaricacid.touhoulittlemaid.client.gui.entity.maid.ai.editor;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import com.github.tartaricacid.touhoulittlemaid.client.ClientLocalChat;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

/**
 * 能就地显示「检查配置」结果的屏。
 *
 * <p>检查跑在服务端（密钥只在那一侧），结果异步回来。它原先走 {@code displayClientMessage}
 * 打进聊天栏——而**触发它的那个界面此刻正开着，聊天栏在界面底下看不见**，管理员点完按钮什么也没有，
 * 回执要退出界面翻聊天记录才读得到。故改为回到发起它的那个屏里显示。</p>
 *
 * <p><b>界面里只放短判词</b>（「检查通过」「配置错误」……）：按钮自己临时变成状态灯，
 * 不在版面里塞一整句话。完整原因——包括 {@code unreachable} 带回来的底层异常原文——挂在
 * 同一颗按钮的悬停提示上，两种故障的区分不因为「要简洁」而丢掉：那个区分正是这个功能存在的理由。</p>
 *
 * <p>屏已经关掉时才回落到聊天栏：那时聊天栏是唯一还看得见的地方，也不再有版面约束，直接给全文。</p>
 */
@Environment(EnvType.CLIENT)
public interface SiteCheckResultDisplay {
    /**
     * @param verdict 短判词，用作按钮的临时标签
     * @param detail  完整原因，用作悬停提示
     * @param argb    <b>必须带 alpha</b>。1.21.11 的 {@code Font} 不再把 alpha=0 补成不透明，
     *                照抄一个 {@code 0xFF5555} 这样的裸 RGB 会画成全透明——事件在跑、文字算对、
     *                屏幕上什么也没有。
     */
    void showSiteCheckResult(Component verdict, Component detail, int argb);

    /**
     * 把结果交给当前屏；没有合适的屏就退回聊天栏。
     */
    static void dispatch(String messageKey, Component detail, int argb) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof SiteCheckResultDisplay display) {
            display.showSiteCheckResult(shortVerdict(messageKey), detail, argb);
            return;
        }
        // 聊天栏用的是 RGB 语义，alpha 位必须摘掉，否则 TextColor 会拿到一个越界值
        Component colored = detail.copy().withStyle(Style.EMPTY.withColor(TextColor.fromRgb(argb & 0x00FFFFFF)));
        // 这条也是本机自己打的提示，走与 ClientLocalChat 同一条路：不查屏蔽名单、不记聊天日志。
        // 26.1.2 把 ChatComponent.addMessage 私有化，公开入口按来源分成两个，取客户端来源那一个。
        ClientLocalChat.show(colored);
    }

    /**
     * 服务端回执键 → 按钮上那一个词。
     *
     * <p>键本身就是判定结果，所以不必为短判词另开一个协议字段。三种「填错了」
     * （站点不在表里 / 没填地址 / 没填密钥）在按钮上合并为「配置错误」，它们的区别在悬停提示里。</p>
     */
    static Component shortVerdict(String messageKey) {
        String suffix = messageKey.substring(messageKey.lastIndexOf('.') + 1);
        // 完整字面键，不用拼接：GuiLangKeyCoverageTest 只认得出完整字面量
        String key = switch (suffix) {
            case "reachable" -> "ai.touhou_little_maid.chat.settings.hub.check_pass";
            case "unreachable" -> "ai.touhou_little_maid.chat.settings.hub.check_unreachable";
            case "busy" -> "ai.touhou_little_maid.chat.settings.hub.check_busy";
            default -> "ai.touhou_little_maid.chat.settings.hub.check_error";
        };
        return Component.translatable(key);
    }
}
