package com.github.tartaricacid.touhoulittlemaid.config;

import com.electronwill.nightconfig.core.EnumGetMethod;
import com.google.common.collect.Lists;
import com.github.tartaricacid.touhoulittlemaid.config.subconfig.AIConfig;
import com.github.tartaricacid.touhoulittlemaid.config.subconfig.ChairConfig;
import com.github.tartaricacid.touhoulittlemaid.config.subconfig.ExperimentalConfig;
import com.github.tartaricacid.touhoulittlemaid.config.subconfig.MaidConfig;
import com.github.tartaricacid.touhoulittlemaid.config.subconfig.MiscConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public class ServerConfig {
    public static ModConfigSpec CONFIG;

    // 客户端需要下载的包
    public static ModConfigSpec.ConfigValue<List<String>> CLIENT_PACK_DOWNLOAD_URLS;

    // 开启女仆 AI 耗时检测
    public static ModConfigSpec.BooleanValue MAID_AI_TIME_DEBUG;

    // 女仆备份机制
    public static ModConfigSpec.IntValue MAID_BACKUP_INTERVAL_SECONDS;
    public static ModConfigSpec.IntValue MAID_BACKUP_MAX_COUNT;

    // 服务器向玩家发布 STT 配置（凭据会下发到受信任客户端）
    public static ModConfigSpec.BooleanValue PROVIDE_SERVER_STT;
    public static ModConfigSpec.BooleanValue PROXY_SERVER_COMPATIBILITY;
    public static ModConfigSpec.EnumValue<ServerSTTApiType> SERVER_STT_TYPE;

    public static ModConfigSpec init() {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        MaidConfig.initServer(builder);
        ExperimentalConfig.init(builder);
        ChairConfig.init(builder);
        MiscConfig.initServer(builder);
        AIConfig.initServer(builder);
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

        builder.comment("Whether the server publishes its selected STT site and credentials to connected players. " +
                "Clients can extract and reuse these credentials; enable only for trusted players or limited credentials.");
        PROVIDE_SERVER_STT = builder.define("ProvideServerSTT", false);

        builder.comment("Compatibility for proxy networks such as Velocity that switch backend servers without disconnecting the client. " +
                "Related backends in the same proxy network must enable this consistently.");
        PROXY_SERVER_COMPATIBILITY = builder.define("ProxyServerCompatibility", false);

        builder.comment("The STT provider published by the server. Player2 is client-local and is intentionally unavailable here.");
        SERVER_STT_TYPE = builder.defineEnum("ServerSTTType", ServerSTTApiType.ALIYUN, EnumGetMethod.NAME_IGNORECASE);

    }
}
