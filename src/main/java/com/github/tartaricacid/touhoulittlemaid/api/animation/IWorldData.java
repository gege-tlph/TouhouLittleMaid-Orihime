package com.github.tartaricacid.touhoulittlemaid.api.animation;

public interface IWorldData {
    /**
     * 获取 Minecraft 世界时间，范围为 0～24000。
     *
     * @return 世界时间
     */
    long getWorldTime();

    /**
     * 判断当前世界是否为白天。
     *
     * @return 布尔值
     */
    boolean isDay();

    /**
     * 判断当前世界是否为夜晚。
     *
     * @return 布尔值
     */
    boolean isNight();

    /**
     * 判断当前世界是否正在下雨。
     *
     * @return 布尔值
     */
    boolean isRaining();

    /**
     * 判断当前世界是否正在打雷。
     *
     * @return 布尔值
     */
    boolean isThundering();
}
