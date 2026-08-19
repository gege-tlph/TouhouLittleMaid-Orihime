package com.github.tartaricacid.touhoulittlemaid.compat.cloth;

import com.github.tartaricacid.touhoulittlemaid.api.event.client.AddClothConfigEvent;
import com.github.tartaricacid.touhoulittlemaid.client.gui.entity.maid.ai.settings.AIChatSettingsHubScreen;
import com.github.tartaricacid.touhoulittlemaid.compat.gun.tacz.TacCompat;
import com.github.tartaricacid.touhoulittlemaid.config.ServerConfig;
import com.github.tartaricacid.touhoulittlemaid.config.subconfig.ChairConfig;
import com.github.tartaricacid.touhoulittlemaid.config.subconfig.ExperimentalConfig;
import com.github.tartaricacid.touhoulittlemaid.config.subconfig.MaidConfig;
import com.github.tartaricacid.touhoulittlemaid.config.subconfig.MiscConfig;
import com.github.tartaricacid.touhoulittlemaid.config.subconfig.RenderConfig;
import com.github.tartaricacid.touhoulittlemaid.config.subconfig.VanillaConfig;
import com.github.tartaricacid.touhoulittlemaid.init.registry.CompatRegistry;
import com.github.tartaricacid.touhoulittlemaid.network.client.config.ServerRulesClientCache;
import com.github.tartaricacid.touhoulittlemaid.network.message.ai.OpenAIConfigPacket;
import com.github.tartaricacid.touhoulittlemaid.util.migrate.ScreenUtil;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Cloth 配置菜单：本机个人配置 + 服务器权威的世界规则。
 *
 * <p>两半的**保存去向完全不同**，别混：个人配置直接写本机 TOML（{@code value.set()/save()}）；
 * 世界规则一个字节也不在本端落地，全部走 {@link ServerRulesClientCache.Session} 攒着，
 * 保存时打成一个只含改动键的包发给服务端，由服务端校验后写存档文件。</p>
 *
 * <p>世界规则那两栏由 {@link ServerRulesClientCache#canEdit()} 门控——无权限的玩家根本看不到，
 * 而不是看得到点不动。权限在服务端求值后随快照下发，客户端不自行判断。</p>
 */
public class MenuIntegration {
    private static final String CATEGORY = "config.touhou_little_maid.menu.";

    public static ConfigBuilder getConfigBuilder() {
        ConfigBuilder root = ConfigBuilder.create().setTitle(Component.literal("Touhou Little Maid"));
        root.setGlobalized(true);
        root.setGlobalizedExpanded(false);
        ConfigEntryBuilder entryBuilder = root.entryBuilder();
        ServerRulesClientCache.Session session = ServerRulesClientCache.createSession();

        addPersonalSettings(root, entryBuilder);
        if (ServerRulesClientCache.canEdit()) {
            addServerRules(root, entryBuilder, session);
            addServerMaintenance(root, entryBuilder, session);
        }

        // Cloth 的保存回调是整个菜单一次性的：个人配置那半由各自的 setSaveConsumer 写完，
        // 这里补上世界规则那半的提交。漏了这一行，世界规则改了会**静默丢失**且界面照样报已保存。
        root.setSavingRunnable(session::save);
        AddClothConfigEvent.CALLBACK.invoker().post(new AddClothConfigEvent(root, entryBuilder));
        return root;
    }

    /**
     * 存档级玩法规则。分组沿用行为基准 {@code port/1.21.11-fabric} 的六组划分，
     * 少 `maid_tamed_item` / `maid_temptation_item` 两条（上游 26.1 已改为物品标签，不再是配置项）。
     */
    private static void addServerRules(ConfigBuilder root, ConfigEntryBuilder entries,
                                       ServerRulesClientCache.Session session) {
        ConfigCategory category = root.getOrCreateCategory(Component.translatable(CATEGORY + "server_rules"));
        if (!ServerRulesClientCache.isIntegratedServer()) {
            category.addEntry(entries.startTextDescription(Component.translatable(
                    CATEGORY + "server_rules.dedicated_reload").withStyle(ChatFormatting.YELLOW)).build());
        }

        SubCategoryBuilder basic = sub(entries, "server.maid_basic");
        basic.add(serverBoolean(entries, "maid.maid_change_model", MaidConfig.MAID_CHANGE_MODEL, session));
        basic.add(serverBoolean(entries, "maid.maid_gomoku_owner_limit", MaidConfig.MAID_GOMOKU_OWNER_LIMIT, session));
        basic.add(serverInt(entries, "maid.owner_max_maid_num", MaidConfig.OWNER_MAX_MAID_NUM, 0, Integer.MAX_VALUE, session));
        basic.add(serverDouble(entries, "maid.replace_allay_percent", MaidConfig.REPLACE_ALLAY_PERCENT, 0, 1, session));
        basic.add(serverBoolean(entries, "maid.enable_emoji", MaidConfig.ENABLE_EMOJI, session));
        basic.add(serverInt(entries, "maid.emoji_check_rate", MaidConfig.EMOJI_CHECK_RATE, 20, 24000, session));
        basic.add(serverInt(entries, "maid.image_emoji_weight", MaidConfig.IMAGE_EMOJI_WEIGHT, 0, 100, session));
        basic.add(serverInt(entries, "maid.kaomoji_emoji_weight", MaidConfig.KAOMOJI_EMOJI_WEIGHT, 0, 100, session));
        category.addEntry(basic.build());

        SubCategoryBuilder ranges = sub(entries, "server.work_ranges");
        ranges.add(serverSlider(entries, "maid.maid_work_range", MaidConfig.MAID_WORK_RANGE, 3, 64, session));
        ranges.add(serverSlider(entries, "maid.maid_idle_range", MaidConfig.MAID_IDLE_RANGE, 3, 32, session));
        ranges.add(serverSlider(entries, "maid.maid_sleep_range", MaidConfig.MAID_SLEEP_RANGE, 3, 32, session));
        ranges.add(serverSlider(entries, "maid.maid_non_home_range", MaidConfig.MAID_NON_HOME_RANGE, 3, 32, session));
        ranges.add(serverInt(entries, "maid.feed_animal_max_number", MaidConfig.FEED_ANIMAL_MAX_NUMBER, 6, 65536, session));
        category.addEntry(ranges.build());

        SubCategoryBuilder combat = sub(entries, "server.combat");
        combat.add(serverSlider(entries, "maid.bow_range", MaidConfig.BOW_RANGE, 8, 192, session));
        combat.add(serverSlider(entries, "maid.cross_bow_range", MaidConfig.CROSS_BOW_RANGE, 8, 192, session));
        combat.add(serverSlider(entries, "maid.danmaku_range", MaidConfig.DANMAKU_RANGE, 8, 192, session));
        combat.add(serverSlider(entries, "maid.trident_range", MaidConfig.TRIDENT_RANGE, 8, 192, session));
        // 枪械三档距离只服务 TaCZ，模组专属选项按 isModLoaded 动态显示——
        // 只在装了 TaCZ 时露面，免得没装的人看到一组永远不起作用的滑条。
        // 三键是世界规则（ServerRuleConfig.values() 认领），读写走 session，与上面的射程滑条同款
        if (FabricLoader.getInstance().isModLoaded(TacCompat.TACZ_ID)) {
            combat.add(serverSlider(entries, "maid.maid_gun_long_distance", MaidConfig.MAID_GUN_LONG_DISTANCE, 0, 512, session));
            combat.add(serverSlider(entries, "maid.maid_gun_medium_distance", MaidConfig.MAID_GUN_MEDIUM_DISTANCE, 0, 512, session));
            combat.add(serverSlider(entries, "maid.maid_gun_near_distance", MaidConfig.MAID_GUN_NEAR_DISTANCE, 0, 512, session));
        }
        combat.add(serverList(entries, "maid.maid_attack_ignore", MaidConfig.MAID_ATTACK_IGNORE, session));
        combat.add(serverList(entries, "maid.maid_ranged_attack_ignore", MaidConfig.MAID_RANGED_ATTACK_IGNORE, session));
        category.addEntry(combat.build());

        SubCategoryBuilder food = sub(entries, "server.food_backpack");
        food.add(serverList(entries, "maid.maid_backpack_blacklist", MaidConfig.MAID_BACKPACK_BLACKLIST, session));
        food.add(serverList(entries, "maid.maid_work_meals_block_list", MaidConfig.MAID_WORK_MEALS_BLOCK_LIST, session));
        food.add(serverList(entries, "maid.maid_home_meals_block_list", MaidConfig.MAID_HOME_MEALS_BLOCK_LIST, session));
        food.add(serverList(entries, "maid.maid_heal_meals_block_list", MaidConfig.MAID_HEAL_MEALS_BLOCK_LIST, session));
        food.add(serverList(entries, "maid.maid_work_meals_block_list_regex", MaidConfig.MAID_WORK_MEALS_BLOCK_LIST_REGEX, session));
        food.add(serverList(entries, "maid.maid_home_meals_block_list_regex", MaidConfig.MAID_HOME_MEALS_BLOCK_LIST_REGEX, session));
        food.add(serverList(entries, "maid.maid_heal_meals_block_list_regex", MaidConfig.MAID_HEAL_MEALS_BLOCK_LIST_REGEX, session));
        food.add(entries.startStrList(tr("maid.maid_eaten_return_container_list"),
                        session.getContainerPairs(MaidConfig.MAID_EATEN_RETURN_CONTAINER_LIST))
                .setDefaultValue(containerDefaults())
                .setTooltip(tip("maid.maid_eaten_return_container_list"))
                .setSaveConsumer(values -> session.setContainerPairs(MaidConfig.MAID_EATEN_RETURN_CONTAINER_LIST, values))
                .build());
        category.addEntry(food.build());

        SubCategoryBuilder world = sub(entries, "server.world_economy");
        world.add(serverDouble(entries, "misc.maid_fairy_power_point", MiscConfig.MAID_FAIRY_POWER_POINT, 0, 5, session));
        world.add(serverInt(entries, "misc.maid_fairy_spawn_probability", MiscConfig.MAID_FAIRY_SPAWN_PROBABILITY, 0, Integer.MAX_VALUE, session));
        world.add(serverList(entries, "misc.maid_fairy_blacklist_dimension", MiscConfig.MAID_FAIRY_BLACKLIST_DIMENSION, session));
        world.add(serverDouble(entries, "misc.player_death_loss_power_point", MiscConfig.PLAYER_DEATH_LOSS_POWER_POINT, 0, 5, session));
        world.add(serverBoolean(entries, "misc.give_smart_slab", MiscConfig.GIVE_SMART_SLAB, session));
        if (FabricLoader.getInstance().isModLoaded(CompatRegistry.PATCHOULI)) {
            world.add(serverBoolean(entries, "misc.give_patchouli_book", MiscConfig.GIVE_PATCHOULI_BOOK, session));
        }
        world.add(serverDouble(entries, "misc.shrine_lamp_effect_cost", MiscConfig.SHRINE_LAMP_EFFECT_COST, 0, Double.MAX_VALUE, session));
        world.add(serverDouble(entries, "misc.shrine_lamp_max_storage", MiscConfig.SHRINE_LAMP_MAX_STORAGE, 0, Double.MAX_VALUE, session));
        world.add(serverInt(entries, "misc.shrine_lamp_max_range", MiscConfig.SHRINE_LAMP_MAX_RANGE, 0, Integer.MAX_VALUE, session));
        world.add(serverInt(entries, "misc.scarecrow_range", MiscConfig.SCARECROW_RANGE, 0, Integer.MAX_VALUE, session));
        category.addEntry(world.build());

        SubCategoryBuilder chair = sub(entries, "server.chair");
        chair.add(serverBoolean(entries, "chair.chair_change_model", ChairConfig.CHAIR_CHANGE_MODEL, session));
        chair.add(serverBoolean(entries, "chair.chair_can_destroyed_by_anyone", ChairConfig.CHAIR_CAN_DESTROYED_BY_ANYONE, session));
        category.addEntry(chair.build());

        SubCategoryBuilder experimental = sub(entries, "server.experimental");
        experimental.add(serverBoolean(entries, "experimental.smooth_follow", ExperimentalConfig.SMOOTH_FOLLOW, session));
        experimental.add(serverBoolean(entries, "experimental.snowball_knockback", ExperimentalConfig.SNOWBALL_KNOCKBACK, session));
        category.addEntry(experimental.build());
    }

    /** 运维参数：同属世界规则文件，但不进公开运行期快照，只有编辑者看得到。 */
    private static void addServerMaintenance(ConfigBuilder root, ConfigEntryBuilder entries,
                                             ServerRulesClientCache.Session session) {
        ConfigCategory category = root.getOrCreateCategory(Component.translatable(CATEGORY + "server_maintenance"));
        category.addEntry(serverList(entries, "menu.server.client_pack_download_urls", ServerConfig.CLIENT_PACK_DOWNLOAD_URLS, session));
        category.addEntry(serverBoolean(entries, "menu.server.maid_ai_time_debug", ServerConfig.MAID_AI_TIME_DEBUG, session));
        category.addEntry(serverInt(entries, "menu.server.maid_backup_interval_seconds",
                ServerConfig.MAID_BACKUP_INTERVAL_SECONDS, 5, Integer.MAX_VALUE, session));
        category.addEntry(serverInt(entries, "menu.server.maid_backup_max_count",
                ServerConfig.MAID_BACKUP_MAX_COUNT, 1, 64, session));
    }

    private static SubCategoryBuilder sub(ConfigEntryBuilder entries, String key) {
        return entries.startSubCategory(Component.translatable(CATEGORY + key)).setExpanded(false);
    }

    private static Component tr(String suffix) {
        return Component.translatable("config.touhou_little_maid." + suffix);
    }

    private static Component tip(String suffix) {
        return Component.translatable("config.touhou_little_maid." + suffix + ".tooltip");
    }

    private static AbstractConfigListEntry<Boolean> serverBoolean(ConfigEntryBuilder entries, String key,
                                                                  ModConfigSpec.ConfigValue<Boolean> value,
                                                                  ServerRulesClientCache.Session session) {
        return entries.startBooleanToggle(tr(key), session.getBoolean(value))
                .setDefaultValue(value.getDefault()).setTooltip(tip(key))
                .setSaveConsumer(newValue -> session.set(value, newValue)).build();
    }

    private static AbstractConfigListEntry<Integer> serverInt(ConfigEntryBuilder entries, String key,
                                                              ModConfigSpec.ConfigValue<Integer> value,
                                                              int min, int max,
                                                              ServerRulesClientCache.Session session) {
        return entries.startIntField(tr(key), session.getInt(value)).setMin(min).setMax(max)
                .setDefaultValue(value.getDefault()).setTooltip(tip(key))
                .setSaveConsumer(newValue -> session.set(value, newValue)).build();
    }

    private static AbstractConfigListEntry<Integer> serverSlider(ConfigEntryBuilder entries, String key,
                                                                 ModConfigSpec.ConfigValue<Integer> value,
                                                                 int min, int max,
                                                                 ServerRulesClientCache.Session session) {
        return entries.startIntSlider(tr(key), session.getInt(value), min, max)
                .setDefaultValue(value.getDefault()).setTooltip(tip(key))
                .setSaveConsumer(newValue -> session.set(value, newValue)).build();
    }

    private static AbstractConfigListEntry<Double> serverDouble(ConfigEntryBuilder entries, String key,
                                                                ModConfigSpec.ConfigValue<Double> value,
                                                                double min, double max,
                                                                ServerRulesClientCache.Session session) {
        return entries.startDoubleField(tr(key), session.getDouble(value)).setMin(min).setMax(max)
                .setDefaultValue(value.getDefault()).setTooltip(tip(key))
                .setSaveConsumer(newValue -> session.set(value, newValue)).build();
    }

    private static AbstractConfigListEntry<List<String>> serverList(ConfigEntryBuilder entries, String key,
                                                                    ModConfigSpec.ConfigValue<? extends List<? extends String>> value,
                                                                    ServerRulesClientCache.Session session) {
        List<String> defaults = new ArrayList<>();
        value.getDefault().forEach(defaults::add);
        return entries.startStrList(tr(key), session.getStringList(value))
                .setDefaultValue(defaults).setTooltip(tip(key))
                .setSaveConsumer(newValue -> session.set(value, newValue)).build();
    }

    private static List<String> containerDefaults() {
        return MaidConfig.MAID_EATEN_RETURN_CONTAINER_LIST.getDefault().stream()
                .filter(pair -> pair.size() == 2)
                .map(pair -> pair.get(0) + "," + pair.get(1))
                .toList();
    }

    /**
     * 个人设置：只写本机 TOML 的那一半——三个分组 + 一颗跳转按钮。
     *
     * <p>分组沿用行为基准 {@code port/1.21.11-fabric} 的三组划分（声音与显示 / 外观与性能 /
     * 交互提示）。代码宿主原本把它们摊成四个平铺栏目（女仆 / 原版设置 / 杂项 / 渲染设置），
     * 那是按**配置文件**分的，不是按**玩家想改什么**分的——「关闭 Optifine 警告」与
     * 「缓存模型图标」被分在两栏，只因为它们住在不同的 spec 里。</p>
     */
    private static void addPersonalSettings(ConfigBuilder root, ConfigEntryBuilder entries) {
        ConfigCategory category = root.getOrCreateCategory(Component.translatable(CATEGORY + "personal"));

        SubCategoryBuilder sound = sub(entries, "personal.sound_display");
        sound.add(entries.startIntSlider(tr("maid.global_maid_sound_frequency"),
                        MaidConfig.GLOBAL_MAID_SOUND_FREQUENCY.get(), 0, 100)
                .setDefaultValue(MaidConfig.GLOBAL_MAID_SOUND_FREQUENCY.getDefault())
                .setTooltip(tip("maid.global_maid_sound_frequency"))
                .setSaveConsumer(local(MaidConfig.GLOBAL_MAID_SOUND_FREQUENCY)).build());
        sound.add(localBoolean(entries, "maid.global_maid_show_chat_bubble", MaidConfig.GLOBAL_MAID_SHOW_CHAT_BUBBLE));
        category.addEntry(sound.build());

        SubCategoryBuilder appearance = sub(entries, "personal.appearance_performance");
        appearance.add(localBoolean(entries, "misc.close_optifine_warning", MiscConfig.CLOSE_OPTIFINE_WARNING));
        appearance.add(localBoolean(entries, "misc.use_new_maid_fairy_model", MiscConfig.USE_NEW_MAID_FAIRY_MODEL));
        appearance.add(localBoolean(entries, "misc.model_icon_cache", MiscConfig.MODEL_ICON_CACHE));
        appearance.add(localBoolean(entries, "misc.invulnerable_particle_effect", MiscConfig.INVULNERABLE_PARTICLE_EFFECT));
        appearance.add(localBoolean(entries, "vanilla.replace_slime_model", VanillaConfig.REPLACE_SLIME_MODEL));
        appearance.add(localBoolean(entries, "vanilla.replace_magma_cube_model", VanillaConfig.REPLACE_MAGMA_CUBE_MODEL));
        appearance.add(localBoolean(entries, "vanilla.replace_xp_texture", VanillaConfig.REPLACE_XP_TEXTURE));
        appearance.add(localBoolean(entries, "vanilla.replace_totem_texture", VanillaConfig.REPLACE_TOTEM_TEXTURE));
        appearance.add(localBoolean(entries, "vanilla.replace_xp_bottle_texture", VanillaConfig.REPLACE_XP_BOTTLE_TEXTURE));
        // 饰品栏兼容（本树走 Trinkets）是模组专属选项，与上方 TaCZ / Patchouli 同一惯例按
        // isModLoaded 动态显示：它的消费点是「模组已加载 && 本键」，模组不在时这个开关
        // 按定义什么都改变不了，摆出来就是一个能设置却不起作用的选项。
        if (FabricLoader.getInstance().isModLoaded(CompatRegistry.TRINKETS)) {
            appearance.add(localBoolean(entries, "maid.enable_maid_curios", MaidConfig.ENABLE_MAID_CURIOS));
        }
        category.addEntry(appearance.build());

        SubCategoryBuilder tips = sub(entries, "personal.interaction_tips");
        tips.add(localBoolean(entries, "render.enable_compass_tip", RenderConfig.ENABLE_COMPASS_TIP));
        tips.add(localBoolean(entries, "render.enable_golden_apple_tip", RenderConfig.ENABLE_GOLDEN_APPLE_TIP));
        tips.add(localBoolean(entries, "render.enable_potion_tip", RenderConfig.ENABLE_POTION_TIP));
        tips.add(localBoolean(entries, "render.enable_milk_bucket_tip", RenderConfig.ENABLE_MILK_BUCKET_TIP));
        tips.add(localBoolean(entries, "render.enable_glass_bottle_tip", RenderConfig.ENABLE_GLASS_BOTTLE_TIP));
        tips.add(localBoolean(entries, "render.enable_name_tag_tip", RenderConfig.ENABLE_NAME_TAG_TIP));
        tips.add(localBoolean(entries, "render.enable_lead_tip", RenderConfig.ENABLE_LEAD_TIP));
        tips.add(localBoolean(entries, "render.enable_saddle_tip", RenderConfig.ENABLE_SADDLE_TIP));
        tips.add(localBoolean(entries, "render.enable_shears_tip", RenderConfig.ENABLE_SHEARS_TIP));
        category.addEntry(tips.build());

        // AI 与语音设置的**唯一入口**，且对所有身份可见可点：personal 这一栏是无条件添加的，
        // 而世界规则那两栏由 canEdit() 门控——入口放到那边就只有管理员看得见。
        // 直接挂在栏目根上：一个只装着一颗按钮的折叠组是纯噪音。
        category.addEntry(new ActionButtonListEntry(
                Component.translatable(CATEGORY + "ai_settings"),
                Component.translatable(CATEGORY + "open_ai_settings"),
                MenuIntegration::openAiSettings));
    }

    /**
     * AI 那一组**不在本菜单里摆开关**，只留这一颗跳转按钮。
     *
     * <p><b>理由不是「这里放不下」，是「同一件事被劈成了两个入口」</b>：总开关与代理曾经在这里，
     * 而站点、密钥、模型、语音全在 mod 自己的五页设置屏里，管理员想让一个 LLM 跑起来得去两个
     * 地方，两个地方连名字都不像是一回事，且两边改的是同一批键。现在全部收拢到「AI 与语音设置」。</p>
     *
     * <p>在世界里必须走 {@link OpenAIConfigPacket} 这条与聊天屏齿轮相同的链路——
     * 权限判定与站点数据都在**服务端**，客户端自己开屏只能开出不需要服务端数据的那一栏。
     * 不在世界里（标题屏进的 modmenu）才回落到纯本机的语音输入设置。</p>
     */
    private static void openAiSettings() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null && ClientPlayNetworking.canSend(OpenAIConfigPacket.TYPE)) {
            OpenAIConfigPacket.sendToServer();
            return;
        }
        ScreenUtil.setScreen(AIChatSettingsHubScreen.openSTTConfig(minecraft.screen));
    }

    /** 个人配置的落盘：写本机 TOML，与世界规则那半的 session 攒改动完全不同。 */
    private static <T> Consumer<T> local(ModConfigSpec.ConfigValue<T> value) {
        return newValue -> {
            value.set(newValue);
            value.save();
        };
    }

    private static AbstractConfigListEntry<Boolean> localBoolean(ConfigEntryBuilder entries, String key,
                                                                 ModConfigSpec.ConfigValue<Boolean> value) {
        return entries.startBooleanToggle(tr(key), value.get())
                .setDefaultValue(value.getDefault()).setTooltip(tip(key))
                .setSaveConsumer(local(value)).build();
    }
}
