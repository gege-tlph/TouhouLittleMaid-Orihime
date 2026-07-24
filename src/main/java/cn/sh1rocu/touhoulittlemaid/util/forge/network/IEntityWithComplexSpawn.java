package cn.sh1rocu.touhoulittlemaid.util.forge.network;

import net.minecraft.network.RegistryFriendlyByteBuf;

/**
 * 实体的接口，在生成时需要在服务器和客户端之间传递额外信息。
 */
public interface IEntityWithComplexSpawn {
    /**
     * 在构建生成数据包时由服务器调用。数据应添加到提供的流中。
     *
     * @param buffer 数据包数据流
     */
    void writeSpawnData(RegistryFriendlyByteBuf buffer);

    /**
     * 当客户端收到实体生成数据包时调用。数据应该以与写入相同的方式从流中读出。
     *
     * @param additionalData 数据包数据流
     */
    void readSpawnData(RegistryFriendlyByteBuf additionalData);
}
