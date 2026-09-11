package com.github.tartaricacid.touhoulittlemaid.inventory.tooltip;

import org.apache.commons.lang3.StringUtils;

/**
 * 提示框需要的 YSM 身份快照，来自**死女仆的 NBT**（照片 / 手办里那份 CompoundTag）。
 *
 * <p>{@code name} 在本树是<b>纯展示字符串</b>，不是 JSON 序列化的 Component——
 * 见 {@link com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid#YSM_MODEL_NAME_TAG}。</p>
 */
public record YsmMaidInfo(boolean isYsmModel, String modelId, String textureId, String name) {
    public static final YsmMaidInfo EMPTY =
            new YsmMaidInfo(false, StringUtils.EMPTY, StringUtils.EMPTY, StringUtils.EMPTY);
}
