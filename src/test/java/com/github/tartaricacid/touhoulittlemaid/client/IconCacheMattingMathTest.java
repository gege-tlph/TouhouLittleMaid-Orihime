package com.github.tartaricacid.touhoulittlemaid.client;

import com.github.tartaricacid.touhoulittlemaid.util.IconCache;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 双背景差分抠像的纯数学契约（{@link IconCache#combinePixel(int, int)}）。
 *
 * <p>动机：origin 的单绿幕等值抠像对半透明贴图部件（琪露诺冰翅 α=128）失效，
 * 混合像素逃过等值判定整片变绿——2026-08-15 实机截图证实，用户批准改为双背景差分。
 * 本测试把「半透明恢复」「纯背景全透」「绿色前景零误杀」「读回舍入容差」四条各钉一例；
 * GPU 读回值由 {@code blend()} 按渲染混合方程合成，模拟真实捕获输入。</p>
 */
class IconCacheMattingMathTest {
    private static final int GREEN = 0xFF_00_FF_00;
    private static final int MAGENTA = 0xFF_FF_00_FF;

    /** 模拟 GPU 混合 + 读回：P = round(F·α + B·(1-α))，读回 α 恒置 FF */
    private static int blend(int fr, int fg, int fb, double alpha, int background) {
        int br = (background >> 16) & 0xFF;
        int bg = (background >> 8) & 0xFF;
        int bb = background & 0xFF;
        int r = (int) Math.round(fr * alpha + br * (1 - alpha));
        int g = (int) Math.round(fg * alpha + bg * (1 - alpha));
        int b = (int) Math.round(fb * alpha + bb * (1 - alpha));
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    private static void assertChannelClose(int expected, int actual, int tolerance, String what) {
        assertTrue(Math.abs(expected - actual) <= tolerance,
                what + " 偏差过大：期望 " + expected + " 实得 " + actual);
    }

    @Test
    void opaqueForegroundIsRecoveredExactly() {
        int result = IconCache.combinePixel(blend(181, 213, 237, 1.0, GREEN), blend(181, 213, 237, 1.0, MAGENTA));
        assertEquals(0xFF_B5_D5_ED, result, "不透明前景两张读回全同，必须逐位原样保留");
    }

    @Test
    void pureBackgroundBecomesTransparent() {
        assertEquals(0, IconCache.combinePixel(GREEN, MAGENTA), "纯背景像素差分幅值拉满，必须判全透明");
    }

    @Test
    void semiTransparentIceWingIsRecovered() {
        // 琪露诺冰翅实测值：F=(181,213,237)、α=128/255——正是 origin 等值抠像下整片变绿的那类像素
        double alpha = 128 / 255.0;
        int result = IconCache.combinePixel(
                blend(181, 213, 237, alpha, GREEN), blend(181, 213, 237, alpha, MAGENTA));
        assertChannelClose(128, result >>> 24, 3, "α");
        assertChannelClose(181, (result >> 16) & 0xFF, 3, "R");
        assertChannelClose(213, (result >> 8) & 0xFF, 3, "G");
        assertChannelClose(237, result & 0xFF, 3, "B");
    }

    @Test
    void opaqueGreenForegroundSurvives() {
        // 阈值法必误杀的案例：模型自身的不透明纯绿部件（大妖精绿裙同类）——差分法两张读回全同，原样保留
        int result = IconCache.combinePixel(blend(0, 255, 0, 1.0, GREEN), blend(0, 255, 0, 1.0, MAGENTA));
        assertEquals(0xFF_00_FF_00, result, "不透明纯绿前景必须原样保留，不许被当背景抠掉");
    }

    @Test
    void toleratesReadbackRoundingNoise() {
        // GPU 混合与读回存在 ±1 LSB 噪声：半透明像素扰动后各通道仍须落在小容差内
        double alpha = 128 / 255.0;
        int p1 = blend(181, 213, 237, alpha, GREEN) + 0x00_01_00_01;
        int p2 = blend(181, 213, 237, alpha, MAGENTA) - 0x00_00_01_00;
        int result = IconCache.combinePixel(p1, p2);
        assertChannelClose(128, result >>> 24, 6, "α(噪声)");
        assertChannelClose(181, (result >> 16) & 0xFF, 6, "R(噪声)");
        assertChannelClose(213, (result >> 8) & 0xFF, 6, "G(噪声)");
        assertChannelClose(237, result & 0xFF, 6, "B(噪声)");
    }
}
