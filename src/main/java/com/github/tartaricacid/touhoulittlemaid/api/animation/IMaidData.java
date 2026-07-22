package com.github.tartaricacid.touhoulittlemaid.api.animation;

import net.minecraft.world.level.biome.Biome;

public interface IMaidData extends IEntityData {
    /**
     * 获取女仆工作任务登记名
     *
     * @return 字符串
     */
    String getTask();

    /**
     * 女仆是否戴头盔
     *
     * @return 布尔值
     */
    boolean hasHelmet();

    /**
     * 获取女仆头盔注册名
     *
     * @return 如果女仆没有戴头盔，则返回空字符串
     */
    String getHelmet();

    /**
     * 女仆是否佩戴胸甲
     *
     * @return 布尔值
     */
    boolean hasChestPlate();

    /**
     * 获取女仆胸甲的注册名
     *
     * @return 如果女仆没有佩戴胸甲，则返回空字符串
     */
    String getChestPlate();

    /**
     * 女仆是否穿打底裤
     *
     * @return 布尔值
     */
    boolean hasLeggings();

    /**
     * 获取女仆打底裤注册名
     *
     * @return 如果女仆没有穿打底裤，则返回空字符串
     */
    String getLeggings();

    /**
     * 女仆是否穿靴子
     *
     * @return 布尔值
     */
    boolean hasBoots();

    /**
     * 获取女仆靴注册名
     *
     * @return 如果女仆没有穿靴子，则返回空字符串
     */
    String getBoots();

    /**
     * 女仆主手是否手持物品
     *
     * @return 布尔值
     */
    boolean hasItemMainhand();

    /**
     * 获取女仆主手物品的注册名
     *
     * @return 如果女仆主手没有任何物品，则返回空字符串
     */
    String getItemMainhand();

    /**
     * 女仆是否手拿物品
     *
     * @return 布尔值
     */
    boolean hasItemOffhand();

    /**
     * 获取女仆副手物品的注册名称
     *
     * @return 如果副手没有任何物品，则返回空字符串
     */
    String getItemOffhand();

    /**
     * 女仆是否正在请求物品
     *
     * @return 布尔值
     */
    boolean isBegging();

    /**
     * 女仆是否摆动手臂
     *
     * @return 布尔值
     */
    boolean isSwingingArms();

    /**
     * 女仆是否正在骑乘
     *
     * @return 布尔值
     */
    boolean isRiding();

    /**
     * 女仆是否在坐
     *
     * @return 布尔值
     */
    boolean isSitting();

    /**
     * 女仆是否装备背包
     *
     * @return 布尔值
     */
    boolean hasBackpack();

    /**
     * 获得女仆背包等级
     *
     * @return 整数
     */
    int getBackpackLevel();

    /**
     * 女仆是否在水中
     *
     * @return 布尔值
     */
    boolean inWater();

    /**
     * 女仆是否处于雨中
     *
     * @return 布尔值
     */
    boolean inRain();

    /**
     * 获取女仆的生物群落登记名
     *
     * @return 字符串
     */
    Biome getAtBiome();

    /**
     * 女仆是否摆动左臂
     *
     * @return 布尔值
     */
    boolean isSwingLeftHand();

    /**
     * 获取女仆当前的手臂摆动进度
     *
     * @return 浮点数
     */
    float getSwingProgress();

    /**
     * 获取女仆当前生命值
     *
     * @return 浮点数
     */
    float getHealth();

    /**
     * 获得女仆的最大生命值
     *
     * @return 浮点数
     */
    float getMaxHealth();

    /**
     * 获取女仆的总护甲值
     *
     * @return 双精度浮点数
     */
    double getArmorValue();

    /**
     * 女仆是否受伤
     *
     * @return 布尔值
     */
    boolean onHurt();

    /**
     * 女仆是否睡觉
     *
     * @return 布尔值
     */
    boolean isSleep();

    /**
     * 获得女仆的好感
     *
     * @return 整数
     */
    int getFavorability();

    /**
     * 女仆是否在地面
     *
     * @return 布尔值
     */
    boolean isOnGround();

    /**
     * 女仆是否有佐物
     *
     * @return 布尔值
     * @deprecated 1.16 no sasimono
     */
    @Deprecated
    boolean hasSasimono();

    /**
     * 女仆是否推车
     *
     * @return 布尔值
     * @deprecated 1.16 no trolley
     */
    @Deprecated
    boolean isHoldTrolley();

    /**
     * 女仆玛丽莎是否骑着扫帚
     *
     * @return 布尔值
     * @deprecated 1.16 no marisa broom
     */
    @Deprecated
    boolean isRidingMarisaBroom();

    /**
     * 女仆是否持有车辆
     *
     * @return 布尔值
     * @deprecated 1.16 no vehicle
     */
    @Deprecated
    boolean isHoldVehicle();

    /**
     * 女仆是否持有便携式音响并播放
     *
     * @return 布尔值
     * @deprecated 1.16 no portable audio
     */
    @Deprecated
    boolean isPortableAudioPlay();

    /**
     * 女仆握住车辆时，左手旋转
     *
     * @return 浮点数[3]{xRot, yRot, zRot}
     * @deprecated 1.16 no vehicle
     */
    @Deprecated
    float[] getLeftHandRotation();

    /**
     * 女仆握住车辆时，右手旋转
     *
     * @return 浮点数[3]{xRot, yRot, zRot}
     * @deprecated 1.16 no vehicle
     */
    @Deprecated
    float[] getRightHandRotation();

    /**
     * 获取女仆的生物群系温度枚举
     *
     * @return 温暖 炎热 海洋 寒冷
     * @deprecated 1.16 no biome temperature enum
     */
    @Deprecated
    String getAtBiomeTemp();

    /**
     * 女仆是否在骑着玩家
     *
     * @return 布尔值
     */
    @Deprecated
    boolean isRidingPlayer();
}
