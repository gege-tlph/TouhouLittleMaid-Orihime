package com.github.tartaricacid.touhoulittlemaid.config;

import com.github.tartaricacid.touhoulittlemaid.config.subconfig.MaidConfig;
import com.github.tartaricacid.touhoulittlemaid.config.subconfig.MiscConfig;
import com.github.tartaricacid.touhoulittlemaid.config.subconfig.RenderConfig;
import com.github.tartaricacid.touhoulittlemaid.config.subconfig.VanillaConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

/**
 * 玩家个人偏好（{@code config/touhou_little_maid-global.toml}），以
 * {@code ModConfig.Type.CLIENT} 注册，且**整段只在物理客户端建立与注册**。
 *
 * <p><b>为什么专服上必须一个字都不写</b>：这里装的全是渲染与界面偏好——聊天气泡、图标缓存、
 * 原版模型替换、物品提示。服主打开 {@code config/} 看到它们，只会得到一个错误结论：
 * 「这些能在服务端配」。<b>载体的存在会被读成语义</b>，与 {@code AvailableSites#managesSttSites}
 * 不生成 {@code stt.json} 是同一条判据。</p>
 *
 * <p>本类与行为基准 {@code port/1.21.11-fabric} 的同名类一一对应。代码宿主 {@code origin/26.1}
 * 没有「拆开」这个形态，它把玩法规则、个人偏好与 AI 三样一起塞在 {@link CommonConfig} 里；
 * 我们逐层拆出去时，最后这一层曾被留在 COMMON 上（写法照宿主），于是专服会凭空多出两个
 * 客户端配置文件——那是相对行为基准的一处差异，2026-08-17 补回。</p>
 *
 * <p>⚠️ 加键前先回答一个问题：<b>它的消费点里有没有一处会在专服上执行？</b>有就不能放这里，
 * 放 {@link CommonConfig}（{@code ENABLE_MAID_CURIOS} 就是这么留下的）。判据按**成因**写——
 * 是「这行代码会不会在专服上跑」，不是「它在不在 {@code client/} 包下」。
 * {@code ConfigBootstrapOrderContractTest} 用机械扫描钉着这条。</p>
 */
public final class GeneralConfig {
    public static ModConfigSpec CONFIG;

    public static ModConfigSpec getConfigSpec() {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        MaidConfig.initClient(builder);
        MiscConfig.initClient(builder);
        VanillaConfig.init(builder);
        RenderConfig.init(builder);
        CONFIG = builder.build();
        return CONFIG;
    }

    /**
     * 本 spec 认领的全部键，供 {@link ConfigFileMigration#migrateGlobalFileIfNeeded} 逐键搬旧值。
     *
     * <p>漏登记一个键 = 那个键的旧值在升级时**静默丢失**（新文件按默认值建，旧文件随后被
     * {@code correct()} 剥掉）。故这张表必须与 {@link #getConfigSpec} 建的键集完全一致，
     * 由 {@code ConfigBootstrapOrderContractTest} 机械对账。</p>
     */
    public static List<ModConfigSpec.ConfigValue<?>> values() {
        return List.of(
                MaidConfig.GLOBAL_MAID_SOUND_FREQUENCY,
                MaidConfig.GLOBAL_MAID_SHOW_CHAT_BUBBLE,
                MiscConfig.CLOSE_OPTIFINE_WARNING,
                MiscConfig.USE_NEW_MAID_FAIRY_MODEL,
                MiscConfig.MODEL_ICON_CACHE,
                MiscConfig.INVULNERABLE_PARTICLE_EFFECT,
                VanillaConfig.REPLACE_SLIME_MODEL,
                VanillaConfig.REPLACE_MAGMA_CUBE_MODEL,
                VanillaConfig.REPLACE_XP_TEXTURE,
                VanillaConfig.REPLACE_TOTEM_TEXTURE,
                VanillaConfig.REPLACE_XP_BOTTLE_TEXTURE,
                RenderConfig.ENABLE_COMPASS_TIP,
                RenderConfig.ENABLE_GOLDEN_APPLE_TIP,
                RenderConfig.ENABLE_POTION_TIP,
                RenderConfig.ENABLE_MILK_BUCKET_TIP,
                RenderConfig.ENABLE_SCRIPT_BOOK_TIP,
                RenderConfig.ENABLE_GLASS_BOTTLE_TIP,
                RenderConfig.ENABLE_NAME_TAG_TIP,
                RenderConfig.ENABLE_LEAD_TIP,
                RenderConfig.ENABLE_SADDLE_TIP,
                RenderConfig.ENABLE_SHEARS_TIP
        );
    }
}
