package com.github.tartaricacid.touhoulittlemaid.api.animation;

public interface IEntityData {
    /**
     * 获取实体的世界数据
     *
     * @return IWorldData
     */
    IWorldData getWorld();

    /**
     * 获取实体的维度id
     *
     * @return 整数
     * @deprecated In 1.16, dimension no longer uses numbers as ids
     */
    @Deprecated
    int getDim();

    /**
     * 获取固定值，每个实体都不同，类似于实体的UUID
     *
     * @return 实体的 uuid 最低有效位
     */
    long getSeed();
}
