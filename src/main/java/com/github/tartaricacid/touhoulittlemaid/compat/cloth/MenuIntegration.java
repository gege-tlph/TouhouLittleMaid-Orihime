package com.github.tartaricacid.touhoulittlemaid.compat.cloth;

import com.github.tartaricacid.touhoulittlemaid.api.event.client.AddClothConfigEvent;
import com.github.tartaricacid.touhoulittlemaid.compat.gun.tacz.TacCompat;
import com.github.tartaricacid.touhoulittlemaid.config.ServerConfig;
import com.github.tartaricacid.touhoulittlemaid.config.subconfig.ChairConfig;
import com.github.tartaricacid.touhoulittlemaid.config.subconfig.MaidConfig;
import com.github.tartaricacid.touhoulittlemaid.config.subconfig.MiscConfig;
import com.github.tartaricacid.touhoulittlemaid.config.subconfig.RenderConfig;
import com.github.tartaricacid.touhoulittlemaid.config.subconfig.VanillaConfig;
import com.github.tartaricacid.touhoulittlemaid.init.registry.CompatRegistry;
import com.github.tartaricacid.touhoulittlemaid.network.client.config.ServerRulesClientCache;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.ArrayList;
import java.util.List;

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

        maidConfig(root, entryBuilder);
        vanillaConfig(root, entryBuilder);
        miscConfig(root, entryBuilder);
        renderConfig(root, entryBuilder);
        GlobalAIIntegration.aiChat(root, entryBuilder);
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
     * 少两组：{@code server.experimental}（`SMOOTH_FOLLOW` 未搬，见 `ServerRuleConfig.values()`），
     * 以及 `maid_tamed_item` / `maid_temptation_item` 两条（上游 26.1 已改为物品标签，不再是配置项）。
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

    @SuppressWarnings("all")
    private static void maidConfig(ConfigBuilder root, ConfigEntryBuilder entryBuilder) {
        ConfigCategory maid = root.getOrCreateCategory(Component.translatable("entity.touhou_little_maid.maid"));

        maid.addEntry(entryBuilder.startIntSlider(Component.translatable("config.touhou_little_maid.maid.global_maid_sound_frequency"), MaidConfig.GLOBAL_MAID_SOUND_FREQUENCY.get(), 0, 100)
                .setDefaultValue(100).setTooltip(Component.translatable("config.touhou_little_maid.maid.global_maid_sound_frequency.tooltip"))
                .setSaveConsumer(i -> {
                    MaidConfig.GLOBAL_MAID_SOUND_FREQUENCY.set(i);
                    MaidConfig.GLOBAL_MAID_SOUND_FREQUENCY.save();
                }).build());

        maid.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.touhou_little_maid.maid.global_maid_show_chat_bubble"), MaidConfig.GLOBAL_MAID_SHOW_CHAT_BUBBLE.get())
                .setDefaultValue(true).setTooltip(Component.translatable("config.touhou_little_maid.maid.global_maid_show_chat_bubble.tooltip"))
                .setSaveConsumer(b -> {
                    MaidConfig.GLOBAL_MAID_SHOW_CHAT_BUBBLE.set(b);
                    MaidConfig.GLOBAL_MAID_SHOW_CHAT_BUBBLE.save();
                }).build());

        maid.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.touhou_little_maid.maid.enable_maid_curios"),
                        MaidConfig.ENABLE_MAID_CURIOS.get())
                .setDefaultValue(MaidConfig.ENABLE_MAID_CURIOS.getDefault())
                .setTooltip(Component.translatable("config.touhou_little_maid.maid.enable_maid_curios.tooltip"))
                .setSaveConsumer(s -> {
                    MaidConfig.ENABLE_MAID_CURIOS.set(s);
                    MaidConfig.ENABLE_MAID_CURIOS.save();
                }).build());

        // 枪械三档距离原先以个人配置形式裸读写在此；TACZ 兼容刀把三键定为世界规则
        // （ServerRuleConfig.values() 认领）后，滑条移入上方 server.combat 段走 session 读写
        // 并包 isModLoaded——两处不可并存，否则菜单写 TOML 会绕过服务器权威通道。
    }

    /** 原版替换五开关：实例级个人配置，默认全 false（用户 2026-07-24 定案，上游默认全 true） */
    private static void vanillaConfig(ConfigBuilder root, ConfigEntryBuilder entryBuilder) {
        ConfigCategory vanilla = root.getOrCreateCategory(Component.translatable("config.touhou_little_maid.vanilla"));
        addVanillaToggle(vanilla, entryBuilder, "replace_slime_model", VanillaConfig.REPLACE_SLIME_MODEL);
        addVanillaToggle(vanilla, entryBuilder, "replace_magma_cube_model", VanillaConfig.REPLACE_MAGMA_CUBE_MODEL);
        addVanillaToggle(vanilla, entryBuilder, "replace_xp_texture", VanillaConfig.REPLACE_XP_TEXTURE);
        addVanillaToggle(vanilla, entryBuilder, "replace_totem_texture", VanillaConfig.REPLACE_TOTEM_TEXTURE);
        addVanillaToggle(vanilla, entryBuilder, "replace_xp_bottle_texture", VanillaConfig.REPLACE_XP_BOTTLE_TEXTURE);
    }

    private static void addVanillaToggle(ConfigCategory category, ConfigEntryBuilder entryBuilder,
                                         String key, net.neoforged.neoforge.common.ModConfigSpec.BooleanValue value) {
        category.addEntry(entryBuilder.startBooleanToggle(
                        Component.translatable("config.touhou_little_maid.vanilla." + key), value.get())
                .setDefaultValue(value.getDefault())
                .setTooltip(Component.translatable("config.touhou_little_maid.vanilla." + key + ".tooltip"))
                .setSaveConsumer(b -> {
                    value.set(b);
                    value.save();
                }).build());
    }

    @SuppressWarnings("all")
    private static void miscConfig(ConfigBuilder root, ConfigEntryBuilder entryBuilder) {
        ConfigCategory misc = root.getOrCreateCategory(Component.translatable("config.touhou_little_maid.misc"));
        misc.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.touhou_little_maid.misc.close_optifine_warning"), MiscConfig.CLOSE_OPTIFINE_WARNING.get())
                .setDefaultValue(false).setTooltip(Component.translatable("config.touhou_little_maid.misc.close_optifine_warning.tooltip"))
                .setSaveConsumer(b -> {
                    MiscConfig.CLOSE_OPTIFINE_WARNING.set(b);
                    MiscConfig.CLOSE_OPTIFINE_WARNING.save();
                }).build());

        misc.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.touhou_little_maid.misc.use_new_maid_fairy_model"), MiscConfig.USE_NEW_MAID_FAIRY_MODEL.get())
                .setDefaultValue(true).setTooltip(Component.translatable("config.touhou_little_maid.misc.use_new_maid_fairy_model.tooltip"))
                .setSaveConsumer(b -> {
                    MiscConfig.USE_NEW_MAID_FAIRY_MODEL.set(b);
                    MiscConfig.USE_NEW_MAID_FAIRY_MODEL.save();
                }).build());

        misc.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.touhou_little_maid.misc.model_icon_cache"), MiscConfig.MODEL_ICON_CACHE.get())
                .setDefaultValue(false).setTooltip(Component.translatable("config.touhou_little_maid.misc.model_icon_cache.tooltip"))
                .setSaveConsumer(b -> {
                    MiscConfig.MODEL_ICON_CACHE.set(b);
                    MiscConfig.MODEL_ICON_CACHE.save();
                }).build());

        misc.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.touhou_little_maid.misc.invulnerable_particle_effect"), MiscConfig.INVULNERABLE_PARTICLE_EFFECT.get())
                .setDefaultValue(true).setTooltip(Component.translatable("config.touhou_little_maid.misc.invulnerable_particle_effect.tooltip"))
                .setSaveConsumer(s -> {
                    MiscConfig.INVULNERABLE_PARTICLE_EFFECT.set(s);
                    MiscConfig.INVULNERABLE_PARTICLE_EFFECT.save();
                }).build());
    }

    private static void renderConfig(ConfigBuilder root, ConfigEntryBuilder entryBuilder) {
        ConfigCategory render = root.getOrCreateCategory(Component.translatable("config.touhou_little_maid.render"));

        render.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.touhou_little_maid.render.enable_compass_tip"), RenderConfig.ENABLE_COMPASS_TIP.get())
                .setDefaultValue(true).setSaveConsumer(value -> {
                    RenderConfig.ENABLE_COMPASS_TIP.set(value);
                    RenderConfig.ENABLE_COMPASS_TIP.save();
                }).build());

        render.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.touhou_little_maid.render.enable_golden_apple_tip"), RenderConfig.ENABLE_GOLDEN_APPLE_TIP.get())
                .setDefaultValue(true).setSaveConsumer(value -> {
                    RenderConfig.ENABLE_GOLDEN_APPLE_TIP.set(value);
                    RenderConfig.ENABLE_GOLDEN_APPLE_TIP.save();
                }).build());

        render.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.touhou_little_maid.render.enable_potion_tip"), RenderConfig.ENABLE_POTION_TIP.get())
                .setDefaultValue(true).setSaveConsumer(value -> {
                    RenderConfig.ENABLE_POTION_TIP.set(value);
                    RenderConfig.ENABLE_POTION_TIP.save();
                }).build());

        render.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.touhou_little_maid.render.enable_milk_bucket_tip"), RenderConfig.ENABLE_MILK_BUCKET_TIP.get())
                .setDefaultValue(true).setSaveConsumer(value -> {
                    RenderConfig.ENABLE_MILK_BUCKET_TIP.set(value);
                    RenderConfig.ENABLE_MILK_BUCKET_TIP.save();
                }).build());

        render.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.touhou_little_maid.render.enable_glass_bottle_tip"), RenderConfig.ENABLE_GLASS_BOTTLE_TIP.get())
                .setDefaultValue(true).setSaveConsumer(value -> {
                    RenderConfig.ENABLE_GLASS_BOTTLE_TIP.set(value);
                    RenderConfig.ENABLE_GLASS_BOTTLE_TIP.save();
                }).build());

        render.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.touhou_little_maid.render.enable_name_tag_tip"), RenderConfig.ENABLE_NAME_TAG_TIP.get())
                .setDefaultValue(true).setSaveConsumer(value -> {
                    RenderConfig.ENABLE_NAME_TAG_TIP.set(value);
                    RenderConfig.ENABLE_NAME_TAG_TIP.save();
                }).build());

        render.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.touhou_little_maid.render.enable_lead_tip"), RenderConfig.ENABLE_LEAD_TIP.get())
                .setDefaultValue(true).setSaveConsumer(value -> {
                    RenderConfig.ENABLE_LEAD_TIP.set(value);
                    RenderConfig.ENABLE_LEAD_TIP.save();
                }).build());

        render.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.touhou_little_maid.render.enable_saddle_tip"), RenderConfig.ENABLE_SADDLE_TIP.get())
                .setDefaultValue(true).setSaveConsumer(value -> {
                    RenderConfig.ENABLE_SADDLE_TIP.set(value);
                    RenderConfig.ENABLE_SADDLE_TIP.save();
                }).build());

        render.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.touhou_little_maid.render.enable_shears_tip"), RenderConfig.ENABLE_SHEARS_TIP.get())
                .setDefaultValue(true).setSaveConsumer(value -> {
                    RenderConfig.ENABLE_SHEARS_TIP.set(value);
                    RenderConfig.ENABLE_SHEARS_TIP.save();
                }).build());
    }
}
