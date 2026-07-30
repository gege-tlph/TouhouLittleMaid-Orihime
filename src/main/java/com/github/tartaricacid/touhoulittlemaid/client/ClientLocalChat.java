package com.github.tartaricacid.touhoulittlemaid.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * 在客户端本地打一行聊天，<b>不经过 {@code ChatListener}</b>。
 *
 * <h3>为什么不能用 {@code LocalPlayer.displayClientMessage}</h3>
 *
 * <p>实测（2026-07-30，jcmd 连续采样渲染线程，9 个连续样本同一栈、CPU 时间不变，
 * 连续阻塞 4.5 秒）：语音识别结果一打出来，渲染线程就卡在一次到 Mojang 的同步 HTTPS 上——</p>
 *
 * <pre>
 * STTCallback.onSuccess -> LocalPlayer.displayClientMessage
 *   -> ChatListener.handleSystemMessage
 *   -> Minecraft.isBlocked -> PlayerSocialManager.isBlocked
 *   -> YggdrasilUserApiService.forceFetchBlockList  ← 阻塞 HTTPS
 * </pre>
 *
 * <p>原版那两处判据（1.21.11 源码）：</p>
 *
 * <pre>
 * // ChatListener
 * if (!options.hideMatchedNames().get() || !minecraft.isBlocked(guessChatUUID(component))) { ... }
 * // PlayerSocialManager
 * public boolean isBlocked(UUID id) {
 *     if (!this.onlineMode) return false;
 *     this.pendingBlockListRefresh.join();          // 渲染线程干等
 *     return this.service.isBlockedPlayer(id);      // 未缓存则同步取名单
 * }
 * </pre>
 *
 * <p>三件要点：① {@code hideMatchedNames} 默认开启；② {@code guessChatUUID} 取的是
 * {@code <...>} 里的名字，而语音识别结果本来就格式化成 {@code <玩家名> 文本}，正好命中；
 * ③ <b>{@code NIL_UUID} 不短路</b>——`isBlockedPlayer` 照样会去取名单，所以「不带尖括号」并不能规避。</p>
 *
 * <p>这个卡顿是原版的：任何系统聊天消息都会付一次，取到名单后缓存。但对访问
 * {@code api.minecraftservices.com} 不畅的玩家，失败不缓存，于是<b>每次识别都卡</b>。
 * 我们自己的本地提示没有任何理由去查屏蔽名单——那名单是给别的玩家发来的消息用的。</p>
 *
 * <p>本方法照抄原版未被屏蔽时的那一支（{@code gui.getChat().addMessage} +
 * 朗读器），只少了屏蔽判定与聊天日志记录。<b>不记聊天日志是有意的</b>：
 * 那份日志用于举报他人发言，而这些行是本机自己打的提示，不是任何人的发言。</p>
 */
@Environment(EnvType.CLIENT)
public final class ClientLocalChat {
    private ClientLocalChat() {
    }

    public static void show(Component message) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.gui.getChat().addMessage(message);
        // 无障碍不能因为绕过 ChatListener 就丢掉
        minecraft.getNarrator().saySystemChatQueued(message);
    }
}
