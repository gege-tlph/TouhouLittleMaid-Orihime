package com.github.tartaricacid.touhoulittlemaid.compat.kaleidoscopetavern;

import com.github.tartaricacid.touhoulittlemaid.api.ILittleMaid;
import com.github.tartaricacid.touhoulittlemaid.compat.kaleidoscopetavern.brain.TavernMaidBrain;
import com.github.tartaricacid.touhoulittlemaid.compat.kaleidoscopetavern.meal.TavernMealCompat;
import com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.ExtraMaidBrainManager;
import com.github.tartaricacid.touhoulittlemaid.entity.task.meal.MaidMealManager;
import net.fabricmc.loader.api.FabricLoader;

/**
 * 万花筒酒馆的独立 Fabric 扩展入口点。核心代码只发现标准的little_maid_extension合约，而从不直接命名这个兼容性模块。
 */
public final class TavernCompat implements ILittleMaid {
    public static final String MOD_ID = "kaleidoscope_tavern";

    public TavernCompat() {
    }

    @Override
    public void addExtraMaidBrain(ExtraMaidBrainManager manager) {
        if (FabricLoader.getInstance().isModLoaded(MOD_ID)) {
            manager.addExtraMaidBrain(new TavernMaidBrain());
        }
    }

    @Override
    public void addMaidMeal(MaidMealManager manager) {
        if (FabricLoader.getInstance().isModLoaded(MOD_ID)) {
            manager.addWorkMealExclusion(TavernMealCompat::isRawHarvestedGrape);
        }
    }
}
