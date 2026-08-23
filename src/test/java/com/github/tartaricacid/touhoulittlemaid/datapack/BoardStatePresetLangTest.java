package com.github.tartaricacid.touhoulittlemaid.datapack;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 预设棋谱引用的每一个描述 key 都必须有译文。
 *
 * <p>棋谱是数据文件、译文是另一份文件，**两边对不上时没有任何东西会报错**——
 * 玩家看到的是物品提示框里一串 {@code board_state.touhou_little_maid.xxx} 原始 key。
 * 本轮补这批数据时就差点栽在这里：我按记忆列了 4 个 key，实际数据包引用了 7 个。</p>
 */
class BoardStatePresetLangTest {
    /** 测试的 workingDir 是 build/test-working，回两级才是项目根。 */
    private static final Path ROOT = Path.of("..", "..");
    private static final Path PRESETS = ROOT.resolve("src/main/resources/data/touhou_little_maid/board_states");
    private static final Path LANG = ROOT.resolve("src/main/resources/assets/touhou_little_maid/lang");
    private static final Pattern KEY = Pattern.compile("\"(board_state\\.[a-z0-9_.]+)\"");

    private static Set<String> referencedKeys() throws IOException {
        Set<String> keys = new LinkedHashSet<>();
        int files = 0;
        for (String name : new String[]{"chess.json", "gomoku.json", "xiangqi.json"}) {
            Path path = PRESETS.resolve(name);
            assertTrue(Files.isRegularFile(path), "缺预设棋谱文件 " + path);
            files++;
            Matcher matcher = KEY.matcher(Files.readString(path, StandardCharsets.UTF_8));
            while (matcher.find()) {
                keys.add(matcher.group(1));
            }
        }
        assertEquals(3, files);
        assertTrue(keys.size() >= 5, "只扫到 " + keys.size() + " 个描述 key，扫描本身可能失效了");
        return keys;
    }

    @Test
    void everyPresetDescriptionKeyHasEnglishAndChinese() throws IOException {
        Set<String> keys = referencedKeys();
        for (String locale : new String[]{"en_us", "zh_cn"}) {
            JsonObject lang;
            try (Reader reader = Files.newBufferedReader(LANG.resolve(locale + ".json"), StandardCharsets.UTF_8)) {
                lang = JsonParser.parseReader(reader).getAsJsonObject();
            }
            List<String> missing = new ArrayList<>();
            for (String key : keys) {
                JsonElement value = lang.get(key);
                if (value == null || value.getAsString().isBlank()) {
                    missing.add(key);
                }
            }
            assertEquals(List.of(), missing,
                    locale + " 缺这些棋谱描述的译文：玩家会在提示框里看到原始 key");
        }
    }

    /** 棋谱本身的形状：三段必备字段缺一，加载时会得到一个空描述或空数据的残局。 */
    @Test
    void everyPresetRecordCarriesDataAndDisplay() throws IOException {
        for (String name : new String[]{"chess.json", "gomoku.json", "xiangqi.json"}) {
            String raw = Files.readString(PRESETS.resolve(name), StandardCharsets.UTF_8);
            // 数据包里带 // 注释（Gson 宽松模式能读），比对前先剥掉
            String stripped = raw.replaceAll("(?m)^\\s*//.*$", "");
            var array = JsonParser.parseString(stripped).getAsJsonArray();
            assertTrue(array.size() > 0, name + " 里一条棋谱都没有");
            for (JsonElement element : array) {
                JsonObject record = element.getAsJsonObject();
                assertTrue(record.has("data") && !record.get("data").getAsString().isBlank(),
                        name + " 里有棋谱没有 data 段");
                assertTrue(record.has("display"), name + " 里有棋谱没有 display 段");
                assertTrue(record.getAsJsonObject("display").has("description"),
                        name + " 里有棋谱的 display 缺 description");
            }
        }
    }
}
