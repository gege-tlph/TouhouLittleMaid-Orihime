package com.github.tartaricacid.touhoulittlemaid.ai.service;

import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;

/**
 * 「这个站点配了密钥没有」——**借用与脱敏同一张字段表**（{@link Site#SECRET_FIELDS}）。
 *
 * <p>不另起一份「密钥叫什么」的清单，是因为两份清单一定会走散：脱敏那份漏了会泄漏明文，
 * 这份漏了会把没配密钥的站点报成配好了。共用一张表意味着**新增密钥字段时只有一处要改**，
 * 而且那一处漏了会被 {@code SiteSecretRedactionTest} 的扫源码断言当场抓住。</p>
 */
public final class SiteSecretPresence {
    private SiteSecretPresence() {
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static boolean hasAnySecret(Site site) {
        SerializableSite serializer = site.serializer();
        if (serializer == null) {
            return true;
        }
        CompoundTag tag = (CompoundTag) serializer.codec()
                .encodeStart(NbtOps.INSTANCE, site)
                .resultOrPartial(TouhouLittleMaid.LOGGER::error)
                .filter(CompoundTag.class::isInstance)
                .orElse(null);
        if (tag == null) {
            return true;
        }
        boolean declaresAny = false;
        for (String field : Site.SECRET_FIELDS) {
            if (tag.getString(field).isEmpty()) {
                continue;
            }
            declaresAny = true;
            if (tag.getString(field).filter(value -> !value.isBlank()).isPresent()) {
                return true;
            }
        }
        // 压根不需要密钥的站点（本地模型、系统 TTS）不该被报成「未配置密钥」
        return !declaresAny;
    }
}
