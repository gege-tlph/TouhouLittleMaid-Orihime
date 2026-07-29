package com.github.tartaricacid.touhoulittlemaid.event;

import com.google.common.collect.Lists;
import net.minecraft.core.registries.BuiltInRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class MaidMealRegConfigEvent {
    private static final Logger LOGGER = LogManager.getLogger(MaidMealRegConfigEvent.class);

    public static List<String> HEAL_MEAL_REGEX = Lists.newArrayList();
    public static List<String> HOME_MEAL_REGEX = Lists.newArrayList();
    public static List<String> WORK_MEAL_REGEX = Lists.newArrayList();

    public static void handleConfig(List<String> config, List<String> output) {
        output.clear();
        output.addAll(getMatchedList(config));
    }

    private static @NotNull List<String> getMatchedList(List<String> matchList) {
        List<String> list = Lists.newArrayList();
        for (String match : matchList) {
            // 上游缺陷（TartaricAcid/TouhouLittleMaid#1135）：非法正则（如 "["）会让
            // Pattern.compile 抛 PatternSyntaxException。本方法经 refreshDerivedState 挂在
            // ServerRuleConfig 的 loadFromPath / initializeDefaults / activatePendingValues 等
            // 七个调用点上，其中 loadFromPath 失败即世界起不来；而配置本身是合法 TOML，
            // last-good 回滚重读后仍是同一条非法正则，玩家只能手改文件才能进游戏。
            // 逐条隔离：坏的那条记日志跳过，其余照常生效。
            Pattern pattern;
            try {
                pattern = Pattern.compile(match);
            } catch (PatternSyntaxException exception) {
                LOGGER.error("Ignoring invalid maid meal block-list regex {}", match, exception);
                continue;
            }
            BuiltInRegistries.ITEM.getAny().stream().filter(id -> pattern.matcher(id.toString()).matches()).forEach(e -> list.add(e.toString()));
        }
        return list;
    }
}
