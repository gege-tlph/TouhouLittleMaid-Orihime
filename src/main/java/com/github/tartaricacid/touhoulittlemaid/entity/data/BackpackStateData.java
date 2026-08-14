package com.github.tartaricacid.touhoulittlemaid.entity.data;

import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.github.tartaricacid.touhoulittlemaid.api.backpack.IBackpackData;
import com.github.tartaricacid.touhoulittlemaid.api.backpack.IMaidBackpack;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.component.CustomData;
import org.jetbrains.annotations.Nullable;

/**
 * 熔炉/液体背包的持久状态附件（行为基准是 {@code EntityMaid} 上的
 * {@code backpackData} 字段 + {@code MaidBackpackData} NBT；本分支的背包状态走数据附件）。
 *
 * <p><b>persistent 但有意不 syncWith</b>：GUI 进度条走容器的
 * {@code addDataSlots(ContainerData)}（原版菜单 data slot 通道），物品内容走菜单槽位同步，
 * 基准也没有任何全局同步——烧炼进度每 tick 在变，若挂 {@code syncWith(all)} 会变成
 * 对所有追踪者每 tick 重发整份状态。</p>
 *
 * <p>附件值是<b>可变 holder</b>：codec 编码时从 {@link #runtime} 拉取活状态
 * （实体存档那一刻才快照，无须任何「保存前刷新」钩子）；解码只得到待恢复的
 * {@link #pendingState}，由 {@code MaidBackpackManager} 在首次访问时按背包类型
 * 惰性创建 runtime 并 {@code load}——解码时拿不到 maid 与 level，绑定只能推迟。
 * tag 内部格式与行为基准逐字相同（含稀疏槽位修复 {@code ItemSlots}），存档可互认。</p>
 */
public final class BackpackStateData {
    private static final Codec<BackpackStateData> CODEC =
            CustomData.COMPOUND_TAG_CODEC.xmap(BackpackStateData::fromTag, BackpackStateData::snapshotTag);

    public static final AttachmentType<BackpackStateData> TYPE = AttachmentRegistry.create(
            Identifier.fromNamespaceAndPath(TouhouLittleMaid.MOD_ID, "backpack_state"), builder -> builder
                    .initializer(BackpackStateData::new)
                    .persistent(CODEC));

    /** 从存档解码出来、尚未装载进 runtime 的状态；绑定后即被消费 */
    private CompoundTag pendingState = new CompoundTag();
    @Nullable
    private IBackpackData runtime;
    @Nullable
    private EntityMaid boundMaid;

    private static BackpackStateData fromTag(CompoundTag tag) {
        BackpackStateData data = new BackpackStateData();
        data.pendingState = tag.copy();
        return data;
    }

    /** 实体存档时由 codec 调用：runtime 在就拉活状态，不在就原样保留待恢复状态 */
    private CompoundTag snapshotTag() {
        if (runtime != null && boundMaid != null) {
            CompoundTag tag = new CompoundTag();
            runtime.save(tag, boundMaid);
            return tag;
        }
        return pendingState.copy();
    }

    @Nullable
    public IBackpackData runtime() {
        return runtime;
    }

    /**
     * 惰性绑定：背包类型带数据而 runtime 还没建时，创建并把待恢复状态装载进去。
     * 读档路径（有 pendingState）与「穿上背包后首次访问」（pendingState 为空，等效全新数据）
     * 都汇到这里；对不带数据的背包类型是 no-op。
     */
    public void bindIfNeeded(EntityMaid maid, IMaidBackpack type) {
        if (runtime != null || !type.hasBackpackData()) {
            return;
        }
        IBackpackData created = type.getBackpackData(maid);
        if (created == null) {
            return;
        }
        if (!pendingState.isEmpty()) {
            created.load(pendingState, maid);
        }
        this.runtime = created;
        this.boundMaid = maid;
        this.pendingState = new CompoundTag();
    }

    /** 背包类型切换时清场：旧数据整体作废（内容物由 {@code onTakeOff} 负责丢出），新类型从全新数据开始 */
    public void reset(EntityMaid maid, IMaidBackpack newType) {
        this.pendingState = new CompoundTag();
        this.boundMaid = maid;
        this.runtime = newType.hasBackpackData() ? newType.getBackpackData(maid) : null;
    }
}
