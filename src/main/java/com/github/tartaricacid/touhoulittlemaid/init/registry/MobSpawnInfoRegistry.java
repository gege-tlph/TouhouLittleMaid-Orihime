package com.github.tartaricacid.touhoulittlemaid.init.registry;

import cn.sh1rocu.touhoulittlemaid.api.event.PotentialSpawnsEvent;
import com.github.tartaricacid.touhoulittlemaid.config.subconfig.MiscConfig;
import com.github.tartaricacid.touhoulittlemaid.config.ServerRuleConfig;
import com.github.tartaricacid.touhoulittlemaid.init.InitEntities;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.util.random.Weighted;

import java.util.List;

public final class MobSpawnInfoRegistry {
    // B6b: 1.21.11 MobSpawnSettings.SpawnerData 的 weight 移出 → 外包 Weighted<SpawnerData>（javap 确认）
    private static Weighted<MobSpawnSettings.SpawnerData> SPAWNER_DATA;

    public static void addMobSpawnInfo(PotentialSpawnsEvent event) {
        if (event.getLevel() instanceof ServerLevel level) {
            int spawnProbability = ServerRuleConfig.get(MiscConfig.MAID_FAIRY_SPAWN_PROBABILITY);
            if (spawnProbability <= 0) {
                // 优先判断等于 0 的情况，减少性能消耗
                return;
            }
            Identifier dimension = level.dimension().identifier();
            if (event.getMobCategory() == MobCategory.MONSTER && dimensionIsOkay(dimension)) {
                List<Weighted<MobSpawnSettings.SpawnerData>> spawnerData = event.getSpawnerDataList();
                // B6b: SpawnerData.type 字段变 private → 记录访问器 type()；列表元素外包 Weighted → data.value().type()
                boolean canZombieSpawn = spawnerData.stream().anyMatch(data -> data.value().type().equals(EntityType.ZOMBIE));
                // Weighted.weight() 取代 SpawnerData.getWeight().asInt()；SpawnerData 现为 (type,min,max) 3 参，weight 入 Weighted
                if (SPAWNER_DATA == null || SPAWNER_DATA.weight() != spawnProbability) {
                    SPAWNER_DATA = new Weighted<>(new MobSpawnSettings.SpawnerData(InitEntities.FAIRY, 2, 4), spawnProbability);
                }
                if (canZombieSpawn) {
                    event.addSpawnerData(SPAWNER_DATA);
                }
            }
        }
    }

    private static boolean dimensionIsOkay(Identifier id) {
        return !ServerRuleConfig.get(MiscConfig.MAID_FAIRY_BLACKLIST_DIMENSION).contains(id.toString());
    }
}
