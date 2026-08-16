package com.github.tartaricacid.touhoulittlemaid.config.subconfig;

import com.google.common.collect.Lists;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

import static com.github.tartaricacid.touhoulittlemaid.util.ItemsUtil.getItemId;

public final class MaidConfig {
    private static final String TRANSLATE_KEY = "config.touhou_little_maid.maid";

    public static ModConfigSpec.IntValue GLOBAL_MAID_SOUND_FREQUENCY;
    public static ModConfigSpec.BooleanValue GLOBAL_MAID_SHOW_CHAT_BUBBLE;

    public static ModConfigSpec.BooleanValue ENABLE_MAID_CURIOS;

    public static ModConfigSpec.IntValue MAID_WORK_RANGE;
    public static ModConfigSpec.IntValue MAID_IDLE_RANGE;
    public static ModConfigSpec.IntValue MAID_SLEEP_RANGE;
    public static ModConfigSpec.IntValue MAID_NON_HOME_RANGE;

    public static ModConfigSpec.IntValue BOW_RANGE;
    public static ModConfigSpec.IntValue CROSS_BOW_RANGE;
    public static ModConfigSpec.IntValue DANMAKU_RANGE;
    public static ModConfigSpec.IntValue TRIDENT_RANGE;

    public static ModConfigSpec.IntValue FEED_ANIMAL_MAX_NUMBER;
    public static ModConfigSpec.BooleanValue MAID_CHANGE_MODEL;
    public static ModConfigSpec.BooleanValue MAID_GOMOKU_OWNER_LIMIT;
    public static ModConfigSpec.IntValue OWNER_MAX_MAID_NUM;
    public static ModConfigSpec.DoubleValue REPLACE_ALLAY_PERCENT;

    // 女仆随机发表情包（颜文字）功能开关
    public static ModConfigSpec.BooleanValue ENABLE_EMOJI;
    public static ModConfigSpec.IntValue EMOJI_CHECK_RATE;
    public static ModConfigSpec.IntValue IMAGE_EMOJI_WEIGHT;
    public static ModConfigSpec.IntValue KAOMOJI_EMOJI_WEIGHT;

    public static ModConfigSpec.ConfigValue<List<String>> MAID_BACKPACK_BLACKLIST;
    public static ModConfigSpec.ConfigValue<List<String>> MAID_ATTACK_IGNORE;
    public static ModConfigSpec.ConfigValue<List<String>> MAID_RANGED_ATTACK_IGNORE;

    public static ModConfigSpec.ConfigValue<List<String>> MAID_WORK_MEALS_BLOCK_LIST;
    public static ModConfigSpec.ConfigValue<List<String>> MAID_HOME_MEALS_BLOCK_LIST;
    public static ModConfigSpec.ConfigValue<List<String>> MAID_HEAL_MEALS_BLOCK_LIST;

    public static ModConfigSpec.ConfigValue<List<String>> MAID_WORK_MEALS_BLOCK_LIST_REGEX;
    public static ModConfigSpec.ConfigValue<List<String>> MAID_HOME_MEALS_BLOCK_LIST_REGEX;
    public static ModConfigSpec.ConfigValue<List<String>> MAID_HEAL_MEALS_BLOCK_LIST_REGEX;
    public static ModConfigSpec.ConfigValue<List<List<String>>> MAID_EATEN_RETURN_CONTAINER_LIST;

    public static ModConfigSpec.IntValue MAID_GUN_LONG_DISTANCE;
    public static ModConfigSpec.IntValue MAID_GUN_MEDIUM_DISTANCE;
    public static ModConfigSpec.IntValue MAID_GUN_NEAR_DISTANCE;

    /**
     * 玩家个人偏好，进 {@link com.github.tartaricacid.touhoulittlemaid.config.GeneralConfig} 的
     * CLIENT spec（{@code config/touhou_little_maid-global.toml}），**只在客户端建立与注册**。
     *
     * <p>与行为基准 {@code port/1.21.11-fabric} 的同名方法一一对应。这两个键专服一行都不读——
     * 消费点是 {@code client/event/MaidSoundFreqEvent} 与 {@code client/renderer/…/EntityMaidRenderState}，
     * 外加 Cloth 菜单（只经 modmenu entrypoint 与客户端 GUI 到达）。</p>
     */
    public static void initClient(ModConfigSpec.Builder builder) {
        builder.translation(TRANSLATE_KEY).push("maid");

        builder.comment("This is a global config that applies to all maids: how often maids speak")
                .translation(translateKey("global_maid_sound_frequency"));
        GLOBAL_MAID_SOUND_FREQUENCY = builder.defineInRange("GlobalMaidSoundFrequency", 100, 0, 100);

        builder.comment("This is a global config that applies to all maids: Whether or not to display chat bubbles")
                .translation(translateKey("global_maid_show_chat_bubble"));
        GLOBAL_MAID_SHOW_CHAT_BUBBLE = builder.define("GlobalMaidShowChatBubble", true);

        builder.pop();
    }

    /**
     * 实例级、**两侧都要读**的配置，进 {@link com.github.tartaricacid.touhoulittlemaid.config.CommonConfig}
     * 的 COMMON spec（{@code config/touhou_little_maid-common.toml}），专服上照样生成。
     *
     * <p>只放真正两侧都读的键。判据是「它的消费点里有没有一处会在专服上执行」——
     * 不是「它看起来像不像玩法设置」。目前唯一的一项：</p>
     *
     * <p>{@code ENABLE_MAID_CURIOS} 是代码宿主 {@code origin/26.1} 自己新增的，1.21.11 上没有对应物。
     * 它经 {@code CuriosCompat.isLoadedOrEnable()} 被 {@code compat/curios/CuriosEvent}、
     * {@code compat/extracontainer/ExtraContainerManager} 与 {@code MaidContainerCache} 读（实查），
     * 这三处都在服务端跑，故**不能**跟着其余个人配置搬进 CLIENT spec——搬了专服每次触碰即 NPE。</p>
     *
     * <p>~~三项 {@code MAID_GUN_*} 同理~~——**此句已过时**：1.21.11 分支 2026-08-14 的 TACZ 批把三键
     * 定为世界规则（键名与默认值与新基相同，TOML 兼容），本树 TACZ 兼容刀随行为基准迁入
     * {@code initServerRule} 并进 {@code ServerRuleConfig.values()} 认领。</p>
     */
    public static void initCommon(ModConfigSpec.Builder builder) {
        builder.translation(TRANSLATE_KEY).push("maid");

        builder.comment("When installed Curios mod, whether to enable maid curios slot support");
        ENABLE_MAID_CURIOS = builder.define("EnableMaidCurios", true);

        builder.pop();
    }

    /**
     * 存档级世界规则，进 {@link com.github.tartaricacid.touhoulittlemaid.config.ServerConfig} 的 SERVER spec，
     * 由 {@link com.github.tartaricacid.touhoulittlemaid.config.ServerRuleConfig} 独占管理。
     *
     * <p>对应行为基准的 {@code initServer}。**这些值一律不得再用 {@code XXX.get()} 读**——SERVER spec
     * 有意不向 Forge Config API Port 注册，`get()` 会抛「Cannot get config value before config is loaded」。
     * 唯一读口是 {@code ServerRuleConfig.get(...)}，`ServerRuleReadRoutingContractTest` 钉着这条。</p>
     */
    public static void initServerRule(ModConfigSpec.Builder builder) {
        builder.translation(TRANSLATE_KEY).push("maid");

        builder.comment("The max range of maid work mode")
                .translation(translateKey("maid_work_range"));
        MAID_WORK_RANGE = builder.defineInRange("MaidWorkRange", 12, 3, 64);

        builder.comment("The max range of maid idle mode")
                .translation(translateKey("maid_idle_range"));
        MAID_IDLE_RANGE = builder.defineInRange("MaidIdleRange", 6, 3, 32);

        builder.comment("The max range of maid sleep mode")
                .translation(translateKey("maid_sleep_range"));
        MAID_SLEEP_RANGE = builder.defineInRange("MaidSleepRange", 6, 3, 32);

        builder.comment("The max range of maid's Non-Home mode")
                .translation(translateKey("maid_non_home_range"));
        MAID_NON_HOME_RANGE = builder.defineInRange("MaidNonHomeRange", 8, 3, 32);

        builder.comment("The max number of animals around when the maid breeds animals")
                .translation(translateKey("bow_range"));
        builder.comment("The max range of maid's bow attack");
        BOW_RANGE = builder.defineInRange("BowRange", 48, 8, 192);

        builder.comment("The max range of maid's crossbow attack")
                .translation(translateKey("cross_bow_range"));
        CROSS_BOW_RANGE = builder.defineInRange("CrossbowRange", 64, 8, 192);

        builder.comment("The max range of maid's danmaku attack")
                .translation(translateKey("danmaku_range"));
        DANMAKU_RANGE = builder.defineInRange("DanmakuRange", 64, 8, 192);

        builder.comment("The max range of maid's trident attack")
                .translation(translateKey("trident_range"));
        TRIDENT_RANGE = builder.defineInRange("TridentRange", 48, 8, 192);

        builder.comment("The max number of animals around when the maid breeds animals")
                .translation(translateKey("feed_animal_max_number"));
        FEED_ANIMAL_MAX_NUMBER = builder.defineInRange("FeedAnimalMaxNumber", 50, 6, 65536);

        builder.comment("Maid can switch models freely")
                .translation(translateKey("maid_change_model"));
        MAID_CHANGE_MODEL = builder.define("MaidChangeModel", true);

        builder.comment("Maid can only play gomoku with her owner")
                .translation(translateKey("maid_gomoku_owner_limit"));
        MAID_GOMOKU_OWNER_LIMIT = builder.define("MaidGomokuOwnerLimit", true);

        builder.comment("The maximum number of maids the player own")
                .translation(translateKey("owner_max_maid_num"));
        OWNER_MAX_MAID_NUM = builder.defineInRange("OwnerMaxMaidNum", Integer.MAX_VALUE, 0, Integer.MAX_VALUE);

        builder.comment("These items cannot be placed in the maid backpack")
                .translation(translateKey("maid_backpack_blacklist"));
        MAID_BACKPACK_BLACKLIST = builder.define("MaidBackpackBlackList", Lists.newArrayList(), MaidConfig::checkItemIds);

        builder.comment("The entity that the maid will not recognize as targets for attack")
                .translation(translateKey("maid_attack_ignore"));
        MAID_ATTACK_IGNORE = builder.define("MaidAttackIgnore", Lists.newArrayList("mekanism:robit"));

        builder.comment("The entity that the maid will not hurt when in ranged attack")
                .translation(translateKey("maid_ranged_attack_ignore"));
        MAID_RANGED_ATTACK_IGNORE = builder.define("MaidRangedAttackIgnore", Lists.newArrayList());

        builder.comment("Percentage chance of replace Allays spawn in pillager outposts with Maids")
                .translation(translateKey("replace_allay_percent"));
        REPLACE_ALLAY_PERCENT = builder.defineInRange("ReplaceAllayPercent", 0.2, 0, 1);

        builder.comment("Enable maid random emoji/kaomoji feature");
        ENABLE_EMOJI = builder.define("EnableEmoji", true);

        builder.comment("The check rate (in ticks) for maid to display random emoji/kaomoji");
        EMOJI_CHECK_RATE = builder.defineInRange("EmojiCheckRate", 60 * 20, 20, 24000);

        builder.comment("The weight for image emoji to be selected");
        IMAGE_EMOJI_WEIGHT = builder.defineInRange("ImageEmojiWeight", 10, 0, 100);

        builder.comment("The weight for kaomoji emoji to be selected");
        KAOMOJI_EMOJI_WEIGHT = builder.defineInRange("KaomojiEmojiWeight", 10, 0, 100);

        builder.comment("These items cannot be used as a maid's work meals")
                .translation(translateKey("maid_work_meals_block_list"));
        MAID_WORK_MEALS_BLOCK_LIST = builder.define("MaidWorkMealsBlockList", Lists.newArrayList(getItemId(Items.PUFFERFISH), getItemId(Items.POISONOUS_POTATO), getItemId(Items.ROTTEN_FLESH), getItemId(Items.SPIDER_EYE), getItemId(Items.CHORUS_FRUIT)), MaidConfig::checkItemIds);

        builder.comment("These items cannot be used as a maid's home meals")
                .translation(translateKey("maid_home_meals_block_list"));
        MAID_HOME_MEALS_BLOCK_LIST = builder.define("MaidHomeMealsBlockList", Lists.newArrayList(getItemId(Items.PUFFERFISH), getItemId(Items.POISONOUS_POTATO), getItemId(Items.ROTTEN_FLESH), getItemId(Items.SPIDER_EYE), getItemId(Items.CHORUS_FRUIT)), MaidConfig::checkItemIds);

        builder.comment("These items cannot be used as a maid's heal meals")
                .translation(translateKey("maid_heal_meals_block_list"));
        MAID_HEAL_MEALS_BLOCK_LIST = builder.define("MaidHealMealsBlockList", Lists.newArrayList(getItemId(Items.PUFFERFISH), getItemId(Items.POISONOUS_POTATO), getItemId(Items.ROTTEN_FLESH), getItemId(Items.SPIDER_EYE)), MaidConfig::checkItemIds);

        builder.comment("These items cannot be used as a maid's work meals which match the regex")
                .translation(translateKey("maid_work_meals_block_list_regex"));
        MAID_WORK_MEALS_BLOCK_LIST_REGEX = builder.define("MaidWorkMealsBlockListRegEx", Lists.newArrayList());

        builder.comment("These items cannot be used as a maid's home meals which match the regex")
                .translation(translateKey("maid_home_meals_block_list_regex"));
        MAID_HOME_MEALS_BLOCK_LIST_REGEX = builder.define("MaidHomeMealsBlockListRegEx", Lists.newArrayList());

        builder.comment("These items cannot be used as a maid's heal meals which match the regex")
                .translation(translateKey("maid_heal_meals_block_list_regex"));
        MAID_HEAL_MEALS_BLOCK_LIST_REGEX = builder.define("MaidHealMealsBlockListRegEx", Lists.newArrayList());

        builder.comment("These entries configure the container returned after a maid has eaten", "Eg: [\"minecraft:beetroot_soup\", \"minecraft:bowl\"]")
                .translation(translateKey("maid_eaten_return_container_list"));
        MAID_EATEN_RETURN_CONTAINER_LIST = builder.define("MaidEatenReturnContainerList", Lists.newArrayList());

        builder.comment("Recognition distance of a maid under the gun task, Suitable for sniper rifles");
        MAID_GUN_LONG_DISTANCE = builder.defineInRange("MaidGunLongDistance", 64, 0, 512);

        builder.comment("Recognition distance of a maid under the gun task, Suitable for most types");
        MAID_GUN_MEDIUM_DISTANCE = builder.defineInRange("MaidGunMediumDistance", 48, 0, 512);

        builder.comment("Recognition distance of a maid under the gun task, Suitable for pistols and shotguns");
        MAID_GUN_NEAR_DISTANCE = builder.defineInRange("MaidGunNearDistance", 32, 0, 512);

        builder.pop();
    }

    private static String translateKey(String key) {
        return TRANSLATE_KEY + "." + key;
    }

    private static boolean checkItemIds(Object obj) {
        if (obj instanceof List<?> list) {
            return list.stream().allMatch(MaidConfig::checkItemId);
        }
        return false;
    }

    private static boolean checkItemId(Object obj) {
        if (obj instanceof String text) {
            return Identifier.tryParse(text) != null;
        }
        return false;
    }
}