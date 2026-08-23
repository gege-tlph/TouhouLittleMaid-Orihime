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
        for (ILittleMaid littleMaid : TouhouLittleMaid.EXTENSIONS) {
            littleMaid.addExtraMaidBrain(manager);
        }
    }

    public void addExtraMaidBrain(IExtraMaidBrain extraMaidBrain) {
        EXTRA_MAID_BRAINS.add(extraMaidBrain);
    }

    /**
     * 任一附属否决即不许攀爬——这是「排除」语义，不是「投票」：
     * 兼容层只会把它自己那类方块摘出去，彼此不该互相覆盖。
     */
    public static boolean canClimbBlock(EntityMaid maid, BlockPos pos, BlockState state) {
        for (IExtraMaidBrain extraMaidBrain : EXTRA_MAID_BRAINS) {
            if (!extraMaidBrain.canClimbBlock(maid, pos, state)) {
                return false;
            }
        }
        return true;
    }
}