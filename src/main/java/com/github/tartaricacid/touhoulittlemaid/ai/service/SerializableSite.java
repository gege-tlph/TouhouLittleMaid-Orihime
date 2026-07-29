package com.github.tartaricacid.touhoulittlemaid.ai.service;

import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;

import javax.annotation.Nullable;

/**
 * 站点序列化接口，用于站点配置的读取与保存，还有站点配置的网络通信
 *
 * @param <T>
 */
public interface SerializableSite<T extends Site> {
    /**
     * 站点序列化反序列化的编码器
     */
    Codec<T> codec();

    /**
     * 生成一个默认站点数据，用于配置文件初始化时生成一个默认配置
     */
    T defaultSite();

    /**
     * 将站点配置写入网络数据包
     */
    default void writeToNetwork(T site, FriendlyByteBuf buffer) {
        codec().encodeStart(NbtOps.INSTANCE, site)
                .resultOrPartial(TouhouLittleMaid.LOGGER::error)
                .ifPresent(tag -> {
                    if (tag instanceof CompoundTag compoundTag) {
                        buffer.writeNbt(compoundTag);
                    }
                });
    }

    /**
     * 写入网络数据包，但**把已配置的密钥换成哨兵值**，明文永不下行。
     *
     * <p><b>为什么在 tag 上做，而不是给每个站点类写覆写</b>：密钥字段只有 {@link Site#SECRET_KEY}
     * 与 {@link Site#SECRET_ID} 两种，散在六个站点类里。在编码结果上统一处理，
     * <b>新增站点类型不可能漏掉</b>；靠逐类覆写则一定会漏，而漏掉不会有任何编译或运行错误，
     * 只会安静地把密钥发出去。</p>
     *
     * <p><b>为什么是哨兵而不是删字段</b>：codec 里这些字段是必需的，删掉客户端就解不出来。
     * 而哨兵还顺带承担了第二个职责——见 {@link #restoreKeptSecrets}。</p>
     */
    default void writeRedactedToNetwork(T site, FriendlyByteBuf buffer) {
        codec().encodeStart(NbtOps.INSTANCE, site)
                .resultOrPartial(TouhouLittleMaid.LOGGER::error)
                .ifPresent(tag -> {
                    if (tag instanceof CompoundTag compoundTag) {
                        buffer.writeNbt(Site.redactSecrets(compoundTag));
                    }
                });
    }

    /**
     * 把客户端送回来的站点里的哨兵密钥换回服务端现有的值。
     *
     * <p><b>哨兵的第二个职责</b>：它让「不改密钥」成为**默认行为**。客户端只改了地址、
     * 密钥框根本没碰，回传的就还是哨兵，服务端照原值填回去。反过来，若把「空字符串」当作
     * 「不改」，管理员就永远无法清除一个已配好的密钥；而若把空当作「清除」，
     * 那么任何一次只改地址的保存都会顺手清空密钥——那正是 §8.A3 要防的回归。</p>
     *
     * @param existing 服务端当前那份；为 null 表示新建站点，哨兵一律落为空
     * @return 已填回真实密钥的站点；解析失败时返回 {@code incoming} 原样
     */
    default T restoreKeptSecrets(T incoming, @Nullable T existing) {
        CompoundTag incomingTag = encodeTag(incoming);
        if (incomingTag == null) {
            return incoming;
        }
        CompoundTag existingTag = existing == null ? null : encodeTag(existing);
        if (!Site.restoreKeptSecrets(incomingTag, existingTag)) {
            // 没有任何哨兵，原样返回，避免一次无谓的解码
            return incoming;
        }
        return codec().parse(NbtOps.INSTANCE, incomingTag)
                .resultOrPartial(TouhouLittleMaid.LOGGER::error)
                .orElse(incoming);
    }

    private @Nullable CompoundTag encodeTag(T site) {
        return codec().encodeStart(NbtOps.INSTANCE, site)
                .resultOrPartial(TouhouLittleMaid.LOGGER::error)
                .filter(CompoundTag.class::isInstance)
                .map(CompoundTag.class::cast)
                .orElse(null);
    }

    /**
     * 从网络数据包读取站点配置
     */
    default T fromNetwork(FriendlyByteBuf buffer) {
        return codec().parse(NbtOps.INSTANCE, buffer.readNbt())
                .resultOrPartial(TouhouLittleMaid.LOGGER::error)
                .orElse(null);
    }

    /**
     * 工具方法，通过站点 ID 获取一个默认图标地址
     */
    static Identifier defaultIcon(String id) {
        return Identifier.fromNamespaceAndPath(TouhouLittleMaid.MOD_ID, "textures/gui/ai_chat/%s.png".formatted(id));
    }
}