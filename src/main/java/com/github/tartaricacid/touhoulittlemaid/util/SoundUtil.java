package com.github.tartaricacid.touhoulittlemaid.util;

import cn.sh1rocu.touhoulittlemaid.mixin.accessor.BiomeAccessor;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.init.InitSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

public final class SoundUtil {
    private static final long MORNING_START = 0;
    private static final long MORNING_END = 3000;
    private static final long EVENING_START = 12000;
    private static final long EVENING_END = 15000;

    public static SoundEvent environmentSound(EntityMaid maid, SoundEvent fallback, float probability) {
        Level world = maid.level();
        RandomSource rand = maid.getRandom();
        BlockPos pos = maid.blockPosition();
        long dayTime = world.getDayTime();
        Biome biome = world.getBiome(pos).value();
        // B5: 1.21.11 Biome.getPrecipitationAt/coldEnoughToSnow 新增 int seaLevel 参数（javap 确认）
        int seaLevel = world.getSeaLevel();

        // 差不多早上 6:00 - 9:00
        if (rand.nextFloat() < probability && MORNING_START < dayTime && dayTime < MORNING_END) {
            return InitSounds.MAID_MORNING;
        }
        // 差不多下午 6:00 - 9:00
        if (rand.nextFloat() < probability && EVENING_START < dayTime && dayTime < EVENING_END) {
            return InitSounds.MAID_NIGHT;
        }
        if (rand.nextFloat() < probability && world.isRaining() && isRainBiome(biome, pos, seaLevel)) {
            return InitSounds.MAID_RAIN;
        }
        if (rand.nextFloat() < probability && world.isRaining() && isSnowyBiome(biome, pos, seaLevel)) {
            return InitSounds.MAID_SNOW;
        }
        if (rand.nextFloat() < probability && biome.coldEnoughToSnow(pos, seaLevel)) {
            return InitSounds.MAID_COLD;
        }
        if (rand.nextFloat() < probability && shouldSnowGolemBurn(biome, pos, seaLevel)) {
            return InitSounds.MAID_HOT;
        }
        return fallback;
    }

    public static SoundEvent attackSound(EntityMaid maid, SoundEvent fallback, float probability) {
        RandomSource rand = maid.getRandom();
        if (rand.nextFloat() < probability) {
            return InitSounds.MAID_FIND_TARGET;
        }
        return fallback;
    }

    public static boolean isRainBiome(Biome biome, BlockPos pos, int seaLevel) {
        return biome.getPrecipitationAt(pos, seaLevel) == Biome.Precipitation.RAIN && !shouldSnowGolemBurn(biome, pos, seaLevel);
    }

    public static boolean isSnowyBiome(Biome biome, BlockPos pos, int seaLevel) {
        return biome.getPrecipitationAt(pos, seaLevel) == Biome.Precipitation.SNOW;
    }

    private static boolean shouldSnowGolemBurn(Biome biome, BlockPos pos, int seaLevel) {
        // 1.21.11 made this positional temperature helper private. Invoke the
        // vanilla method so altitude adjustment remains identical to 1.21.1.
        return ((BiomeAccessor) (Object) biome).touhouLittleMaid$invokeGetTemperature(pos, seaLevel) > 1.0F;
    }
}
