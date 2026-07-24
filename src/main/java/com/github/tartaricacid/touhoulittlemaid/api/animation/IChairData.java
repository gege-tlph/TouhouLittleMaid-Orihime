package com.github.tartaricacid.touhoulittlemaid.api.animation;

public interface IChairData extends IEntityData {
    /**
     * 玩家是否坐在椅子上
     *
     * @return 布尔值
     */
    boolean isRidingPlayer();

    /**
     * 椅子上是否有骑行实体
     *
     * @return 布尔值
     */
    boolean hasPassenger();

    /**
     * 获取乘客的偏航角
     *
     * @return 浮动
     */
    float getPassengerYaw();

    /**
     * 获取乘客的推介
     *
     * @return 浮动
     */
    float getPassengerPitch();

    /**
     * 获取自身偏航角
     *
     * @return 浮动
     */
    float getYaw();
}
