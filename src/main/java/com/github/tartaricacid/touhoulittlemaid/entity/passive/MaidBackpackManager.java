package com.github.tartaricacid.touhoulittlemaid.entity.passive;

import com.github.tartaricacid.touhoulittlemaid.api.backpack.IBackpackData;
import com.github.tartaricacid.touhoulittlemaid.api.backpack.IMaidBackpack;
import com.github.tartaricacid.touhoulittlemaid.entity.backpack.BackpackManager;
import com.github.tartaricacid.touhoulittlemaid.entity.backpack.EmptyBackpack;
import com.github.tartaricacid.touhoulittlemaid.entity.data.BackpackData;
import com.github.tartaricacid.touhoulittlemaid.entity.data.BackpackStateData;
import com.github.tartaricacid.touhoulittlemaid.init.InitDataAttachment;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

@MaidManagerDef(alias = "backpackManager", exposeView = true)
public class MaidBackpackManager {
    private final EntityMaid maid;
    private IMaidBackpack backpack;
    private int backpackDelay = 0;

    MaidBackpackManager(EntityMaid maid) {
        this.maid = maid;
        this.backpack = BackpackManager.getEmptyBackpack();
    }

    public IMaidBackpack getMaidBackpackType() {
        BackpackData data = this.maid.getAttachedOrCreate(InitDataAttachment.BACKPACK);
        Identifier id = Identifier.parse(data.type());
        IMaidBackpack emptyBackpack = BackpackManager.getEmptyBackpack();
        return BackpackManager.findBackpack(id).orElse(emptyBackpack);
    }

    public boolean hasBackpack() {
        IMaidBackpack type = this.getMaidBackpackType();
        return !(type instanceof EmptyBackpack);
    }

    public void setMaidBackpackType(IMaidBackpack backpack) {
        if (backpack == this.backpack) {
            return;
        }
        this.backpack = backpack;
        BackpackData data = new BackpackData(backpack.getId().toString());
        this.maid.setAttached(InitDataAttachment.BACKPACK, data);
        // 类型切换 = 状态清场：旧数据作废（内容物由 onTakeOff 丢出），新类型从全新数据开始。
        // 行为基准同款（setMaidBackpack 里 create-or-null 那两行）。
        this.stateData().reset(this.maid, backpack);
    }

    /**
     * 熔炉/液体背包的数据对象；不带数据的背包类型（空/小/中/大/末影箱/工作台）恒为 null。
     *
     * <p>惰性绑定：读档后附件里只有待恢复的 NBT，首次访问（这里或 {@link #tick}）才按
     * 背包类型创建 runtime 并装载。客户端也会绑定出一份（内容为空、由菜单槽位同步填充），
     * 与行为基准在客户端 setMaidBackpack 时创建空数据的行为一致。</p>
     */
    @Nullable
    public IBackpackData getBackpackData() {
        BackpackStateData state = this.stateData();
        state.bindIfNeeded(this.maid, this.getMaidBackpackType());
        return state.runtime();
    }

    private BackpackStateData stateData() {
        return this.maid.getAttachedOrCreate(InitDataAttachment.BACKPACK_STATE);
    }

    public void setBackpackDelay() {
        backpackDelay = 20;
    }

    public boolean backpackHasDelay() {
        return backpackDelay > 0;
    }

    void tick() {
        if (backpackDelay > 0) {
            backpackDelay--;
        }
        // 行为基准在 aiStep 的服务端分支里 backpackData.serverTick(this)，每 tick 一次；
        // 本分支挂在 baseTick 的 manager.tick() 上，节奏相同，客户端不跑。
        if (!this.maid.level().isClientSide()) {
            IBackpackData data = this.getBackpackData();
            if (data != null) {
                data.serverTick(this.maid);
            }
        }
    }

    interface View {
        MaidBackpackManager getBackpackManager();

        default IMaidBackpack getMaidBackpackType() {
            return getBackpackManager().getMaidBackpackType();
        }

        default boolean hasBackpack() {
            return getBackpackManager().hasBackpack();
        }

        default void setMaidBackpackType(IMaidBackpack backpack) {
            getBackpackManager().setMaidBackpackType(backpack);
        }

        default void setBackpackDelay() {
            getBackpackManager().setBackpackDelay();
        }

        default boolean backpackHasDelay() {
            return getBackpackManager().backpackHasDelay();
        }

        @Nullable
        default IBackpackData getBackpackData() {
            return getBackpackManager().getBackpackData();
        }
    }
}
