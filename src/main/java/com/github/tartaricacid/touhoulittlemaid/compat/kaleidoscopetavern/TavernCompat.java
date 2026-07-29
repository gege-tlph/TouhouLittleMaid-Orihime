package com.github.tartaricacid.touhoulittlemaid.compat.kaleidoscopetavern;

import com.github.tartaricacid.touhoulittlemaid.api.ILittleMaid;
import com.github.tartaricacid.touhoulittlemaid.compat.kaleidoscopetavern.brain.TavernMaidBrain;
import com.github.tartaricacid.touhoulittlemaid.compat.kaleidoscopetavern.meal.TavernMealCompat;
import com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.ExtraMaidBrainManager;
import com.github.tartaricacid.touhoulittlemaid.entity.task.meal.MaidMealManager;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Self-contained Fabric extension entrypoint for Kaleidoscope Tavern.
 * Core code only discovers the standard little_maid_extension contract and
 * never names this compatibility module directly.
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
