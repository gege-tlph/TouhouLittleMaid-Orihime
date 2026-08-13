package com.github.tartaricacid.touhoulittlemaid.event;

import com.google.common.collect.Lists;
import net.minecraft.core.registries.BuiltInRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.regex.Pattern;

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
            Pattern pattern = Pattern.compile(match);
            BuiltInRegistries.ITEM.getAny().stream().filter(id -> pattern.matcher(id.toString()).matches()).forEach(e -> list.add(e.toString()));
        }
        return list;
    }
}
