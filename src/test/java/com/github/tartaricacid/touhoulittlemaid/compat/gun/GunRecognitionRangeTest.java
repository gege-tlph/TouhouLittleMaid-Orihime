package com.github.tartaricacid.touhoulittlemaid.compat.gun;

import com.github.tartaricacid.touhoulittlemaid.compat.gun.common.GunRecognitionRange;
import com.github.tartaricacid.touhoulittlemaid.config.ServerConfig;
import com.github.tartaricacid.touhoulittlemaid.config.subconfig.MaidConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * 枪种 → 识别距离档位的映射。
 *
 * <p>这份映射此前只长在 {@code TacInnerCompat.canSee} 里，而 {@code TaskGunAttack.searchRadius}
 * 恒取 LONG(64) 不看枪种——扫描半径与交战半径因此不是一个数，且那个 64 会顺着
 * {@code EntityMaid.searchRadius()} 流进传感器扫描盒与寻路 BFS 半径。统一之后
 * 两个消费者共用本类，这里把映射本身钉死。</p>
 *
 * <p>映射刻意只吃小写枪种字符串而不吃 TaCZ 的类型，因此**不需要 TaCZ 在运行期存在**
 * （它是 modCompileOnly）——否则这份判定逻辑将无法被任何测试覆盖。</p>
 */
class GunRecognitionRangeTest {
    @BeforeAll
    static void initialize() {
        ServerConfig.init();
    }

    @Test
    void sniperUsesTheLongDistance() {
        assertSame(MaidConfig.MAID_GUN_LONG_DISTANCE, GunRecognitionRange.configFor("sniper"));
    }

    @Test
    void closeQuartersWeaponsUseTheNearDistance() {
        assertSame(MaidConfig.MAID_GUN_NEAR_DISTANCE, GunRecognitionRange.configFor("smg"));
        assertSame(MaidConfig.MAID_GUN_NEAR_DISTANCE, GunRecognitionRange.configFor("pistol"));
        assertSame(MaidConfig.MAID_GUN_NEAR_DISTANCE, GunRecognitionRange.configFor("shotgun"));
    }

    @Test
    void everythingElseUsesTheMediumDistance() {
        assertSame(MaidConfig.MAID_GUN_MEDIUM_DISTANCE, GunRecognitionRange.configFor("rifle"));
        assertSame(MaidConfig.MAID_GUN_MEDIUM_DISTANCE, GunRecognitionRange.configFor("rpg"));
        // 枪包作者可以自定义枪种，未知枪种必须落到中档而不是抛异常
        assertSame(MaidConfig.MAID_GUN_MEDIUM_DISTANCE, GunRecognitionRange.configFor("some_modded_type"));
    }

    @Test
    void caseIsNormalized() {
        assertSame(MaidConfig.MAID_GUN_LONG_DISTANCE, GunRecognitionRange.configFor("SNIPER"));
    }

    @Test
    void noGunMeansNoGunDistanceAtAll() {
        // null 表示「手里不是枪 / 枪包索引查不到」，调用方据此回落到自己的默认工作范围，
        // 免得端着锄头的女仆还按枪的识别距离去扫世界
        assertNull(GunRecognitionRange.configFor(null));
    }
}
