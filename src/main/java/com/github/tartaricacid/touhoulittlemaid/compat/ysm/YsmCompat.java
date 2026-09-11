package com.github.tartaricacid.touhoulittlemaid.compat.ysm;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.inventory.tooltip.YsmMaidInfo;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;
import net.fabricmc.loader.api.metadata.version.VersionPredicate;
import net.minecraft.nbt.CompoundTag;

/**
 * OpenYSM（第三方模型系统）安装检测，与其它 {@code compat/} 子包同一套软链接约定
 * （见 CLAUDE.md「Optional Compatibility Boundary」）：只经 {@link FabricLoader} 查询模组容器与版本，
 * 不 import 任何 {@code rip.ysm.*}/{@code com.elfmcys.yesstevemodel.*} 类——那些类可能压根不在
 * classpath 上，import 会直接编译失败。
 * <p>
 * 与 {@code port/1.21.11-fabric} 分支同名类的差异：那边 {@link #getYsmMaidInfo} 读到的
 * {@code YSM_MODEL_NAME_TAG} 是 JSON 序列化的 {@code Component}，需要解析；<b>本树存的是纯字符串</b>
 * （来自 {@code YsmMaidModelPackage} 的 {@code displayName}），故本树的读取方一律直接当文本用。
 * 除此之外行为一致。
 */
public final class YsmCompat {
    private static final String MOD_ID = "yes_steve_model";
    private static final VersionPredicate VERSION_RANGE;
    private static boolean INSTALLED = false;

    static {
        try {
            // 下限对齐本轮联调的 openysm port/26.1.2 版本线；随双方一起演进，不必与
            // 1.21.11 分支的下限保持一致——那是两条不同的产品身份/兼容承诺。
            VERSION_RANGE = VersionPredicate.parse(">=2.7.0.0");
        } catch (VersionParsingException e) {
            throw new RuntimeException(e);
        }
    }

    private YsmCompat() {
    }

    public static void init() {
        FabricLoader.getInstance().getModContainer(MOD_ID).ifPresent(modContainer -> {
            Version version = modContainer.getMetadata().getVersion();
            if (VERSION_RANGE.test(version)) {
                INSTALLED = true;
            } else {
                // 开发环境下 version 可能是空的，需要额外判断
                INSTALLED = FabricLoader.getInstance().isDevelopmentEnvironment();
            }
        });
    }

    public static boolean isInstalled() {
        return INSTALLED;
    }

    /**
     * 从一份**女仆 NBT**里取 YSM 身份，供物品提示框显示「这只女仆用的是哪个 YSM 模型」。
     *
     * <p>读的是 {@code CompoundTag} 而不是 {@link EntityMaid} 实例：照片 / 手办里存的就是一份
     * 标签，没有实体可问。</p>
     *
     * <p>未安装 YSM 时一律返回 {@link YsmMaidInfo#EMPTY}——存档里可能留着上次装着 YSM 时写下的
     * 模型名，这时候拿它当标题会显示一个当前根本渲染不出来的模型。</p>
     */
    public static YsmMaidInfo getYsmMaidInfo(CompoundTag maidData) {
        if (!isInstalled()) {
            return YsmMaidInfo.EMPTY;
        }
        return new YsmMaidInfo(
                maidData.getBooleanOr(EntityMaid.IS_YSM_MODEL_TAG, false),
                maidData.getStringOr(EntityMaid.YSM_MODEL_ID_TAG, ""),
                maidData.getStringOr(EntityMaid.YSM_MODEL_TEXTURE_TAG, ""),
                maidData.getStringOr(EntityMaid.YSM_MODEL_NAME_TAG, ""));
    }
}
