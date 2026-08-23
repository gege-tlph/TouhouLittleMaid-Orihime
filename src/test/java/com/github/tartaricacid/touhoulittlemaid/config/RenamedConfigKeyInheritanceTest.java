package com.github.tartaricacid.touhoulittlemaid.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 换了段名的配置键，旧存档里的值必须搬到新位置。
 *
 * <p><b>这道闸为什么存在</b>：改段名等于换了一个键——旧值就此无人认领，玩家的设置被
 * <b>新默认值静默取代</b>。平滑跟随 2026-08-19 从实验性分组搬进女仆分组时，
 * 新默认值恰好是**开**，所以服主当初特意关掉的会在升级后自己打开，且没有任何提示。
 * 这类失败不会崩、不会报错、不会进日志的 ERROR，只会让人觉得「这版怎么手感变了」。</p>
 *
 * <p>判据按成因写：不断言「平滑跟随」这一个键，而断言**搬运语义**——
 * 新位置空着才搬、搬完可重复执行、两边都没有就什么都不做。</p>
 */
class RenamedConfigKeyInheritanceTest {
    private static final List<String> LEGACY = List.of("experimental", "SmoothFollow");
    private static final List<String> CURRENT = List.of("maid", "SmoothFollow");

    private static final Path MIGRATION_SOURCE = Path.of("..", "..", "src", "main", "java", "com", "github",
            "tartaricacid", "touhoulittlemaid", "config", "ConfigFileMigration.java");

    @Test
    void legacyValueMovesToTheNewSection() {
        CommentedConfig config = newConfig();
        config.set(LEGACY, false);

        assertTrue(ConfigFileMigration.inheritRenamedKey(config, LEGACY, CURRENT),
                "旧段有值、新段空着，必须搬");
        assertEquals(false, config.getRaw(CURRENT),
                "服主当初特意关掉的，升级后不许被新默认值静默打开");
    }

    @Test
    void newSectionWinsWhenBothArePresent() {
        CommentedConfig config = newConfig();
        config.set(LEGACY, false);
        config.set(CURRENT, true);

        assertFalse(ConfigFileMigration.inheritRenamedKey(config, LEGACY, CURRENT),
                "新段已有值说明这份文件已经是新格式，不该再被旧值覆盖");
        assertEquals(true, config.getRaw(CURRENT));
    }

    @Test
    void doesNothingWhenTheLegacyKeyIsAbsent() {
        CommentedConfig config = newConfig();

        assertFalse(ConfigFileMigration.inheritRenamedKey(config, LEGACY, CURRENT),
                "两边都没有就什么都不做，让调用方按 spec 补默认值");
        assertNull(config.getRaw(CURRENT));
    }

    /** 搬运必须可重复执行——同一份文件被处理两次不能出现第二种结果。 */
    @Test
    void isIdempotent() {
        CommentedConfig config = newConfig();
        config.set(LEGACY, false);

        assertTrue(ConfigFileMigration.inheritRenamedKey(config, LEGACY, CURRENT));
        assertFalse(ConfigFileMigration.inheritRenamedKey(config, LEGACY, CURRENT),
                "第二次不该再报「搬了」，否则每次启动都会白回写一次文件");
        assertEquals(false, config.getRaw(CURRENT));
    }

    /**
     * 接线与**次序**：搬运必须排在「补齐缺失键」之前。
     *
     * <p>补缺失那步只填空位。次序反了，新位置会先被 spec 默认值填上，随后搬运看到
     * 新位置已有值就直接返回——旧值被静默吃掉，而两个方法各自都还是对的。
     * 同型的次序坑本仓库已经栽过一次（岩浆怪继承与 global 迁移的先后）。</p>
     */
    @Test
    void renameRunsBeforeFillingMissingKeys() throws IOException {
        String body = methodBody(Files.readString(MIGRATION_SOURCE, StandardCharsets.UTF_8),
                "public static Path prepareWorldFile");
        assertTrue(body != null, "找不到 prepareWorldFile 的方法体，识别依据可能已失效");

        int rename = body.indexOf("inheritRenamedKey");
        int fill = body.indexOf("inheritMissingValues");
        assertTrue(rename >= 0, "prepareWorldFile 根本没搬过改名的键——旧存档的值会被默认值吃掉");
        assertTrue(fill >= 0, "prepareWorldFile 里找不到补齐缺失键那一步，识别依据可能已失效");
        assertTrue(rename < fill,
                "搬运排在了补缺失之后：新位置会先被默认值填上，旧值就再也搬不过来了");
    }

    private static CommentedConfig newConfig() {
        return CommentedConfig.inMemory();
    }

    /** 取 {@code anchor}（方法声明）之后的第一个大括号块。 */
    private static String methodBody(String source, String anchor) {
        int at = source.indexOf(anchor);
        if (at < 0) {
            return null;
        }
        int open = source.indexOf('{', at);
        int depth = 0;
        for (int i = open; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return source.substring(open, i + 1);
                }
            }
        }
        return null;
    }
}
