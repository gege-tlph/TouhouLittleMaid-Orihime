package com.github.tartaricacid.touhoulittlemaid.config;

import com.github.tartaricacid.touhoulittlemaid.config.subconfig.ChairConfig;
import com.github.tartaricacid.touhoulittlemaid.config.subconfig.MaidConfig;
import com.github.tartaricacid.touhoulittlemaid.config.subconfig.MiscConfig;
import com.google.common.collect.Lists;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

/**
 * 存档级世界规则的 spec 定义。
 *
 * <p>⚠️ <b>本 spec 有意不向 Forge Config API Port 注册</b>。注册 {@code ModConfig.Type.SERVER} 会让 FCAP
 * 自己去建、读、写 {@code <world>/serverconfig/touhou_little_maid-server.toml}——那正是
 * {@link ServerRuleConfig} 要独占管理的那个文件，两个写者会互相覆盖。本类只提供
 * 「spec」这一个身份：给 {@code correct()} 播种默认值与注释、给 {@code getSpec().test()} 做校验。</p>
 *
 * <p>由此产生一条**机制性保证**：这些值上的 {@code XXX.get()} 会抛
 * {@code IllegalStateException: Cannot get config value before config is loaded}
 * （已对 forgeconfigapiport-fabric 26.1.4 的 {@code ConfigValue.getRaw} 字节码取证）。
 * 也就是说，漏改道的读点会**当场炸**，而不是静默读到实例级的那份旧值——
 * 后者才是真正危险的形态（「两侧各有一份」）。</p>
 */
public class ServerConfig {
    private static final String TRANSLATE_KEY = "config.touhou_little_maid.render";

    public static ModConfigSpec CONFIG;

    // 客户端需要下载的包
    public static ModConfigSpec.ConfigValue<List<String>> CLIENT_PACK_DOWNLOAD_URLS;

    // 开启女仆 AI 耗时检测
    public static ModConfigSpec.BooleanValue MAID_AI_TIME_DEBUG;

    // 女仆备份机制
    public static ModConfigSpec.IntValue MAID_BACKUP_INTERVAL_SECONDS;
    public static ModConfigSpec.IntValue MAID_BACKUP_MAX_COUNT;

    public static ModConfigSpec init() {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        MaidConfig.initServerRule(builder);
        ChairConfig.initServerRule(builder);
        MiscConfig.initServerRule(builder);
        initValues(builder);
        CONFIG = builder.build();
        return CONFIG;
    }

    public static void initValues(ModConfigSpec.Builder builder) {
        builder.comment("The maid pack that the client player needs to download, needs to fill in the URL value of the file");
        builder.comment("Example: [\"https://www.dropbox.com/download/apple.zip\", \"https://www.dropbox.com/download/cat.zip\"]");
        CLIENT_PACK_DOWNLOAD_URLS = builder.define("ClientPackDownloadUrls", Lists.newArrayList());

        builder.comment("When turned on, when the maid AI executes more than 50ms, the entity information of the problem will be logged");
        MAID_AI_TIME_DEBUG = builder.define("MaidAITimeDebug", false);

        builder.comment("The interval time (in seconds) for the maid backup mechanism to back up maid data");
        MAID_BACKUP_INTERVAL_SECONDS = builder.defineInRange("MaidBackupIntervalSeconds", 60 * 3, 5, Integer.MAX_VALUE);

        builder.comment("The maximum number of backups for each player when the maid backup mechanism is enabled");
        MAID_BACKUP_MAX_COUNT = builder.defineInRange("MaidBackupMaxCount", 3, 1, 64);
    }

    private static String translateKey(String key) {
        return TRANSLATE_KEY + "." + key;
    }
}
