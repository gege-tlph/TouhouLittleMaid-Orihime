package com.github.tartaricacid.touhoulittlemaid.entity.ai.brain;

import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.github.tartaricacid.touhoulittlemaid.api.ILittleMaid;
import com.github.tartaricacid.touhoulittlemaid.api.entity.ai.IExtraMaidBrain;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.google.common.collect.Lists;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public final class ExtraMaidBrainManager {
    static List<IExtraMaidBrain> EXTRA_MAID_BRAINS = Lists.newArrayList();

    public static void init() {
        ExtraMaidBrainManager manager = new ExtraMaidBrainManager();
        // SWEEP R9-1（2026-07-19）：原「EXTENSIONS not available (26.1 feature)」TODO 系误判——
        // TouhouLittleMaid.EXTENSIONS(:21) 本树存在且他处在用；还原 origin 的 addon 扩展点循环
        for (ILittleMaid littleMaid : TouhouLittleMaid.EXTENSIONS) {
            littleMaid.addExtraMaidBrain(manager);
        }
    }

    public void addExtraMaidBrain(IExtraMaidBrain extraMaidBrain) {
        EXTRA_MAID_BRAINS.add(extraMaidBrain);
    }

    public static boolean canClimbBlock(EntityMaid maid, BlockPos pos, BlockState state) {
        for (IExtraMaidBrain extraMaidBrain : EXTRA_MAID_BRAINS) {
            if (!extraMaidBrain.canClimbBlock(maid, pos, state)) {
                return false;
            }
        }
        return true;
    }
}
