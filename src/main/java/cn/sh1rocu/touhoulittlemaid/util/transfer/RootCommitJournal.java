/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package cn.sh1rocu.touhoulittlemaid.util.transfer;

import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;

/**
 * A journal that can be added to a transaction with {@link #updateSnapshots(TransactionContext)},
 * and will invoke a callback {@linkplain #rootCommitCallback if/when the root transaction is committed}.
 *
 * <p>This journal does not save any state snapshot by itself,
 * so it should be used in conjunction with other {@linkplain SnapshotJournal journals} that handle the actual state.
 *
 * <p>⚠️ 快照必须是<b>非空哨兵</b>而不能像 NeoForge 原版那样用 {@code @Nullable Void}：
 * 本类经 {@link SnapshotJournal} 嫁接在 Fabric 的 {@code SnapshotParticipant} 上，
 * 后者的 {@code updateSnapshots} 对 {@code createSnapshot()} 做非空断言——null 快照让
 * {@code VanillaContainerWrapper} 的整条取放路径必炸 NPE（2026-08-14 实机复现：
 * 熔炉背包燃烧时被替换，onTakeOff 丢物即崩；GameTest
 * {@code takeOffDropPathExtractsThroughVanillaContainerWrapper} 钉着）。
 * 顺带修掉 null 快照的另一半隐患：{@code SnapshotJournal.onClose} 里
 * 「快照列表槽位为 null = 尚无快照」与「快照值本身是 null」会混淆，
 * 且 {@code originalState == null} 的「回调未注册」判定会失真。</p>
 */
public final class RootCommitJournal extends SnapshotJournal<Object> {
    /** 无状态哨兵：本日志不保存任何状态，但 Fabric 基类要求快照非空 */
    private static final Object NO_STATE = new Object();

    private final Runnable rootCommitCallback;

    public RootCommitJournal(Runnable rootCommitCallback) {
        this.rootCommitCallback = rootCommitCallback;
    }

    @Override
    protected Object createSnapshot() {
        return NO_STATE;
    }

    @Override
    protected void readSnapshot(Object snapshot) {
    }

    @Override
    protected void onRootCommit(Object originalState) {
        rootCommitCallback.run();
    }
}
