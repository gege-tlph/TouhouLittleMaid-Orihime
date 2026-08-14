package com.github.tartaricacid.touhoulittlemaid.compat.gun.common;

import com.github.tartaricacid.touhoulittlemaid.config.subconfig.MaidConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * 枪种 → 识别距离配置项的映射。**唯一一份**，`canSee` 与 `searchRadius` 共用。
 *
 * <p>基准里这份映射只长在 {@code TacInnerCompat.canSee} 里，而 {@code TaskGunAttack.searchRadius}
 * 恒取 {@code MAID_GUN_LONG_DISTANCE}（64）。于是**扫描半径与实际交战半径不是一个数**：
 * 拿冲锋枪的女仆按 32 格判定能不能打，却按 64 格去扫描世界——而那个 64 还会顺着
 * {@code EntityMaid.searchRadius()} 流进 {@code MaidNearestLivingEntitySensor} 的扫描盒
 * 与 {@code MaidPathFindingBFS} 的寻路半径，让「找方块」类行为一并放大到 64 格。
 * 现在两处都按枪种取值，共用本类这一份映射。</p>
 *
 * <p>刻意只吃**小写枪种字符串**而不吃 TaCZ 的类型：这样映射本身可以在没有 TaCZ 的环境里被单元测试
 * 覆盖（TaCZ 是 modCompileOnly，运行期不在 classpath 上）。取字符串那一步留在
 * {@code TacInnerCompat}，那步只是一次 API 调用，没有判定逻辑。</p>
 */
public final class GunRecognitionRange {
    /** 与 TaCZ 的 {@code GunTabType} 常量同名，此处存小写字面量以便脱离 TaCZ 测试。 */
    private static final String SNIPER = "sniper";
    private static final String SHOTGUN = "shotgun";
    private static final String PISTOL = "pistol";
    private static final String SMG = "smg";

    private GunRecognitionRange() {
    }

    /**
     * @param weaponType TaCZ 的枪种字符串；{@code null} 表示手里不是枪或枪包索引查不到
     * @return 对应的识别距离配置项；{@code null} 表示没有可用的枪械距离，调用方应回落到自己的默认值
     */
    @Nullable
    public static ModConfigSpec.IntValue configFor(@Nullable String weaponType) {
        if (weaponType == null) {
            return null;
        }
        String type = weaponType.toLowerCase(Locale.ENGLISH);
        // 狙击枪：远距离
        if (SNIPER.equals(type)) {
            return MaidConfig.MAID_GUN_LONG_DISTANCE;
        }
        // 霰弹枪 / 手枪 / 冲锋枪：近距离
        if (SHOTGUN.equals(type) || PISTOL.equals(type) || SMG.equals(type)) {
            return MaidConfig.MAID_GUN_NEAR_DISTANCE;
        }
        // 其余：中等距离
        return MaidConfig.MAID_GUN_MEDIUM_DISTANCE;
    }
}
