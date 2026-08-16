package com.github.tartaricacid.touhoulittlemaid.ai.service;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.util.Util;
import net.minecraft.resources.Identifier;

import java.util.Comparator;
import java.util.Map;
import java.util.function.Function;
import java.util.function.ToIntFunction;

public interface Site {
    /**
     * 与序列化反序列化相关的字段
     */
    String ID = "id";
    String API_TYPE = "api_type";
    String ENABLED = "enabled";
    String ICON = "icon";
    String URL = "url";
    String SECRET_ID = "secret_id";
    String SECRET_KEY = "secret_key";
    String HEADERS = "headers";
    String MODELS = "models";
    String HAS_THINKING_FIELD = "has_thinking_field";
    // 仅用于 MiniMax TTS，表示语音合成的模型，和 MODELS 里列出的模型不同，MODELS 里列出的是 voice_id，而 siteModel 是一个独立的参数
    String SITE_MODEL = "site_model";
    // 腾讯云 asr 语言类型
    String ENG_SER_VICE_TYPE = "eng_ser_vice_type";
    // 腾讯云 asr 热词
    String HOT_WORD = "hot_word";

    /**
     * 全部密钥字段。**新增带密钥的站点类型时必须把新字段名加进来**，
     * 脱敏与「保持原密钥」都以这张表为准；漏加不会报错，只会安静地把明文发出去。
     */
    String[] SECRET_FIELDS = {SECRET_KEY, SECRET_ID};

    /**
     * 哨兵值：下行时代替真实密钥，上行时表示「这一项别动」。
     *
     * <p>取一个真实密钥不可能取到的值——首字符是 NUL，任何服务商的密钥都不会长这样。</p>
     *
     * <p>⚠️ 这里用 Java 转义 {@code \0} 而不是把裸 NUL 字节写进源文件。行为基准
     * {@code port/1.21.11-fabric} 的同名常量嵌的是真字节，代价是 <b>git 把整个
     * {@code Site.java} 判成 binary</b>——不出 diff、不能合并，任何按文本读它的工具都可能改坏它。
     * 运行期值逐字节相同。</p>
     */
    String SECRET_KEPT = "\0tlm:secret-kept";

    /**
     * 把 tag 里所有**非空**的密钥字段换成哨兵；空的保持为空（那是「未配置」，客户端要能区分）。
     */
    static net.minecraft.nbt.CompoundTag redactSecrets(net.minecraft.nbt.CompoundTag tag) {
        for (String field : SECRET_FIELDS) {
            tag.getString(field)
                    .filter(value -> !value.isBlank())
                    .ifPresent(value -> tag.putString(field, SECRET_KEPT));
        }
        return tag;
    }

    /**
     * 把 tag 里的哨兵密钥换回 {@code existing} 里的真实值；{@code existing} 为 null 时落为空串。
     *
     * @return 是否存在过哨兵（无哨兵时调用方可以跳过一次解码）
     */
    static boolean restoreKeptSecrets(net.minecraft.nbt.CompoundTag tag,
                                      @javax.annotation.Nullable net.minecraft.nbt.CompoundTag existing) {
        boolean found = false;
        for (String field : SECRET_FIELDS) {
            if (!SECRET_KEPT.equals(tag.getString(field).orElse(null))) {
                continue;
            }
            found = true;
            String previous = existing == null ? "" : existing.getString(field).orElse("");
            tag.putString(field, previous);
        }
        return found;
    }

    /**
     * 用于控制 JSON 序列化的字段顺序
     */
    ToIntFunction<String> FIXED_ORDER_FIELDS = Util.make(new Object2IntOpenHashMap<>(), map -> {
        map.put(ID, 0);
        map.put(API_TYPE, 1);
        map.put(ENABLED, 2);
        map.put(ICON, 3);
        map.put(URL, 4);
        map.put(SECRET_KEY, 5);
        map.put(HEADERS, 6);
        map.defaultReturnValue(100);
        map.put(MODELS, Integer.MAX_VALUE);
    });

    /**
     * 用于控制 JSON 序列化的字段顺序
     */
    Comparator<String> KEY_COMPARATOR = Comparator.comparingInt(FIXED_ORDER_FIELDS).thenComparing(Function.identity());

    /**
     * 该站点的 ID，唯一标识一个站点
     * 该 ID 不建议包含空格或者其他非英文字符
     */
    String id();

    /**
     * 该站点是否启用
     * 启用后才会在选择界面显示
     */
    boolean enabled();

    /**
     * 设置站点是否启用
     */
    void setEnabled(boolean enabled);

    /**
     * 该站点的图标
     * 用于游戏内配置站点功能的显示
     */
    Identifier icon();

    /**
     * 该站点的 URL
     * 用于请求数据
     */
    String url();

    /**
     * HTTP 头部信息，特殊头部信息需要在这里添加
     */
    Map<String, String> headers();

    /**
     * 服务类型，指的是 LLM STT 还是 TTS
     */
    ServiceType getServiceType();

    /**
     * API 类型，指的是该站点的 API 类型
     * 不同 API 类型的站点拥有不同的解析和通信方式
     */
    String getApiType();

    /**
     * 该站点的客户端
     * 用于请求数据
     */
    Client client();

    /**
     * 该站点的序列化器，用于读取和写入 JSON 配置数据或者网络通信
     */
    default SerializableSite<? extends Site> serializer() {
        return SerializerRegister.getSerializer(getServiceType(), getApiType());
    }

    /**
     * 站点名称语言文件 key
     */
    default String getNameKey() {
        return "ai.touhou_little_maid.chat.site.%s.name".formatted(id());
    }
}
