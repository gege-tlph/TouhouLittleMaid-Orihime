package com.github.tartaricacid.touhoulittlemaid.debug.target;

import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.github.tartaricacid.touhoulittlemaid.api.ILittleMaid;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.VisibleForDebug;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * 管理调试数据
 */
@VisibleForDebug
public class DebugMaidManager {
    public static ConcurrentHashMap<UUID, Set<UUID>> PLAYER_DEBUGGING_MAID = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<UUID, Set<UUID>> MAID_DEBUGGING_PLAYER = new ConcurrentHashMap<>();
    public static List<Function<EntityMaid, List<DebugTarget>>> DEBUG_TARGETS = new ArrayList<>();

    public static void init() {
        DEBUG_TARGETS.addAll(DefaultTargets.getDefaultTargets());
        // B2 已恢复：原 TODO 称「EXTENSIONS not available (26.1 feature)」—— **该理由是假的**。
        //   三方校验：HEAD 有此循环；26.1 亦有（L26）；`TouhouLittleMaid.EXTENSIONS` 由
        //   `AnnotatedInstanceUtil.getModExtensions()`（Fabric `little_maid_extension` entrypoint）填充，
        //   `ILittleMaid.getMaidDebugTargets()` 在 HEAD 与本仓皆存在 → 与 26.1 无关。
        //   曾致：addon 注册的女仆调试目标**全部丢失**。
        for (ILittleMaid littleMaid : TouhouLittleMaid.EXTENSIONS) {
            DEBUG_TARGETS.addAll(littleMaid.getMaidDebugTargets());
        }
    }

    /**
     * 获取女仆的所有调试目标
     *
     * @param maid 女仆
     * @return 调试目标列表
     */
    public static List<DebugTarget> getDebugTargets(EntityMaid maid) {
        return DEBUG_TARGETS.stream().flatMap(f -> f.apply(maid).stream()).toList();
    }

    /**
     * 根据女仆获取调试该女仆的玩家
     *
     * @param maid 女仆
     * @return 玩家列表
     */
    public static List<ServerPlayer> getDebuggingPlayer(EntityMaid maid) {
        Set<UUID> playerId = MAID_DEBUGGING_PLAYER.get(maid.getUUID());
        if (playerId == null) {
            return List.of();
        }
        MinecraftServer server = maid.level.getServer();
        if (server == null) {
            return List.of();
        }
        return playerId.stream()
                .map(uuid -> server.getPlayerList().getPlayer(uuid))
                .filter(Objects::nonNull).toList();
    }

    /**
     * 根据玩家获取正在调试该玩家的女仆
     *
     * @param player 玩家
     * @return 女仆列表
     */
    public static List<EntityMaid> getDebuggingMaid(ServerPlayer player) {
        Set<UUID> maidId = PLAYER_DEBUGGING_MAID.get(player.getUUID());
        if (maidId == null) {
            return List.of();
        }
        return maidId.stream()
                // 1.21.11: ServerPlayer.serverLevel() 已移除（javap 确认），但 ServerPlayer.level()
                // 已**协变收窄为返回 ServerLevel** → 无需强转、无需 import。语义与 HEAD 的 serverLevel() 相同。
                .map(uuid -> player.level().getEntity(uuid))
                .filter(Objects::nonNull)
                .filter(EntityMaid.class::isInstance)
                .map(EntityMaid.class::cast).toList();
    }

    /**
     * 设置正在调试的女仆
     *
     * @param player 玩家
     * @param maid   女仆
     */
    public static void setDebuggingMaid(ServerPlayer player, EntityMaid maid) {
        removeDebuggingMaid(player, maid);
        PLAYER_DEBUGGING_MAID.computeIfAbsent(player.getUUID(), k -> new HashSet<>()).add(maid.getUUID());
        MAID_DEBUGGING_PLAYER.computeIfAbsent(maid.getUUID(), k -> new HashSet<>()).add(player.getUUID());
    }

    /**
     * 移除正在调试的女仆
     *
     * @param player 玩家
     * @param maid   女仆
     */
    public static void removeDebuggingMaid(ServerPlayer player, EntityMaid maid) {
        if (PLAYER_DEBUGGING_MAID.containsKey(player.getUUID())) {
            PLAYER_DEBUGGING_MAID.get(player.getUUID()).remove(maid.getUUID());
        }
        if (MAID_DEBUGGING_PLAYER.containsKey(maid.getUUID())) {
            MAID_DEBUGGING_PLAYER.get(maid.getUUID()).remove(player.getUUID());
        }
    }

    /**
     * 切换该女仆的调试状态
     *
     * @param player 玩家
     * @param maid   女仆
     */
    public static void triggerDebuggingMaid(ServerPlayer player, EntityMaid maid) {
        if (PLAYER_DEBUGGING_MAID.containsKey(player.getUUID()) && PLAYER_DEBUGGING_MAID.get(player.getUUID()).contains(maid.getUUID())) {
            removeDebuggingMaid(player, maid);
            // B2 已还原为 HEAD 写法：移植期曾改成 displayClientMessage(...,false) —— **无正当理由的行为变更**。
            // 三方校验：HEAD 用 sendSystemMessage；26.1 亦用 sendSystemMessage(L115/118)；javap 确认该方法仍在。
            player.sendSystemMessage(Component.translatable("debug.touhou_little_maid.debug_stick.show_path_finder.disable"));
        } else {
            setDebuggingMaid(player, maid);
            player.sendSystemMessage(Component.translatable("debug.touhou_little_maid.debug_stick.show_path_finder.enable"));
        }
    }
}