package com.github.tartaricacid.touhoulittlemaid.entity.ai.path;

import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.github.tartaricacid.touhoulittlemaid.debug.target.DebugMaidManager;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.pathfinder.NodeEvaluator;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathFinder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * 用于加入debug信息
 */
public class MaidWrappedPathFinder extends PathFinder {
    protected Mob mob;

    public MaidWrappedPathFinder(NodeEvaluator nodeEvaluator, int maxVisitedNodes) {
        super(nodeEvaluator, maxVisitedNodes);
    }

    @Nullable
    @Override
    public Path findPath(@NotNull PathNavigationRegion region, @NotNull Mob mob, @NotNull Set<BlockPos> targetPositions, float maxRange, int accuracy, float searchDepthMultiplier) {
        this.mob = mob;
        // 1.21.11: 原版 PathFinder 已内建寻路 debug 捕获（字段 captureDebug + setCaptureDebug(BooleanSupplier)），
        // 其私有 findPath 在 captureDebug 为真时自行调用 Path.setDebug(openSet, closedSet, targets) ——
        // 与本类原先手动 wrap BinaryHeap.pop() 收集 open/closed 集所做的事完全一致。
        // 故原先覆盖的 findPath(ProfilerFiller, Node, Map, ...) 已变为 private（扩展点不再需要），
        // 改用原版官方 API。此写法与 26.1 一致。
        this.setCaptureDebug(() -> TouhouLittleMaid.DEBUG && mob instanceof EntityMaid maid && DebugMaidManager.getDebuggingPlayer(maid) != null);
        return super.findPath(region, mob, targetPositions, maxRange, accuracy, searchDepthMultiplier);
    }
}
