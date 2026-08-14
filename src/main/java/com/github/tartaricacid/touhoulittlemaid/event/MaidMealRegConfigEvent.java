package com.github.tartaricacid.touhoulittlemaid.event;

import com.google.common.collect.Lists;
import net.minecraft.core.registries.BuiltInRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * 三张「按正则屏蔽的食物」派生表。
 *
 * <p>原先由 {@code ModConfigEvents.loading} 在 COMMON 配置加载时刷新（{@code onEvent}）。
 * 三个来源正则已随世界规则迁进 SERVER spec，成了**存档级**的值，
 * 刷新点也随之改为 {@code ServerRuleConfig.refreshDerivedState()}——它挂在世界规则的
 * 每一处状态变更上（加载 / 激活 / 保存即生效 / reload / 运行期快照下发），
 * 比原来只在配置文件加载时刷新覆盖得更全。行为基准 {@code port/1.21.11-fabric} 同此。</p>
 */
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
            // 上游缺陷（TartaricAcid/TouhouLittleMaid#1135）：非法正则（如一个孤立的左方括号）会让
            // Pattern.compile 抛 PatternSyntaxException。本方法挂在 ServerRuleConfig 的
            // 世界规则全部状态变更点上，其中加载路径失败即世界起不来；而配置本身是合法 TOML，
            // last-good 回滚重读后仍是同一条非法正则，玩家只能手改文件才能进游戏。
            // 逐条隔离：坏的那条记名跳过，其余照常生效。
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
