package com.github.tartaricacid.touhoulittlemaid.inventory.tooltip;

import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import org.jetbrains.annotations.Nullable;

/**
 * @param ysmMaidInfo 提示框标题优先用 YSM 模型名。构造点拿不到时传 {@link YsmMaidInfo#EMPTY}，
 *                    不要传 null——消费方按 {@code isYsmModel()} 分支，EMPTY 天然走本体模型那条。
 */
public record ItemMaidTooltip(String modelId, @Nullable Component customName,
                              YsmMaidInfo ysmMaidInfo) implements TooltipComponent {
}
