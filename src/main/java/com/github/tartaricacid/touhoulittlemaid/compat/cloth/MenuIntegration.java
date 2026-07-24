package com.github.tartaricacid.touhoulittlemaid.compat.cloth;

import com.github.tartaricacid.touhoulittlemaid.ai.service.stt.STTSite;
import com.github.tartaricacid.touhoulittlemaid.api.event.client.AddClothConfigEvent;
import com.github.tartaricacid.touhoulittlemaid.client.gui.entity.maid.ai.editor.STTSiteEditorScreen;
import com.github.tartaricacid.touhoulittlemaid.client.gui.entity.maid.ai.settings.AIChatSettingsHubScreen;
import com.github.tartaricacid.touhoulittlemaid.config.GeneralConfig;
import com.github.tartaricacid.touhoulittlemaid.config.ServerConfig;
import com.github.tartaricacid.touhoulittlemaid.config.ServerSTTApiType;
import com.github.tartaricacid.touhoulittlemaid.config.subconfig.AIConfig;
import com.github.tartaricacid.touhoulittlemaid.config.subconfig.ChairConfig;
import com.github.tartaricacid.touhoulittlemaid.config.subconfig.ExperimentalConfig;
import com.github.tartaricacid.touhoulittlemaid.config.subconfig.MaidConfig;
import com.github.tartaricacid.touhoulittlemaid.config.subconfig.MiscConfig;
import com.github.tartaricacid.touhoulittlemaid.config.subconfig.RenderConfig;
import com.github.tartaricacid.touhoulittlemaid.config.subconfig.VanillaConfig;
import com.github.tartaricacid.touhoulittlemaid.init.registry.CompatRegistry;
import com.github.tartaricacid.touhoulittlemaid.network.client.config.ServerRulesClientCache;
import com.github.tartaricacid.touhoulittlemaid.network.message.ai.SaveSTTSitePacket;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class MenuIntegration {
    private static final String CATEGORY = "config.touhou_little_maid.menu.";

    private MenuIntegration() {
    }

    public static ConfigBuilder getConfigBuilder() {
        ConfigBuilder root = ConfigBuilder.create().setTitle(Component.literal("Touhou Little Maid"));
        root.setGlobalized(true);
        root.setGlobalizedExpanded(false);

        ConfigEntryBuilder entries = root.entryBuilder();
        ServerRulesClientCache.Session serverSession = ServerRulesClientCache.createSession();

        addPersonalSettings(root, entries);
        if (ServerRulesClientCache.canEdit()) {
            addServerRules(root, entries, serverSession);
            addServerMaintenance(root, entries, serverSession);
        }

        root.setSavingRunnable(() -> {
            GeneralConfig.CONFIG.save();
            if (ServerRulesClientCache.canEdit()) {
                serverSession.save();
            }
        });
        AddClothConfigEvent.CALLBACK.invoker().post(new AddClothConfigEvent(root, entries));
        return root;
    }

    private static void addPersonalSettings(ConfigBuilder root, ConfigEntryBuilder entries) {
        ConfigCategory category = root.getOrCreateCategory(Component.translatable(CATEGORY + "personal"));

        SubCategoryBuilder sound = sub(entries, "personal.sound_display");
        sound.add(entries.startIntSlider(tr("maid.global_maid_sound_frequency"),
                        MaidConfig.GLOBAL_MAID_SOUND_FREQUENCY.get(), 0, 100)
                .setDefaultValue(MaidConfig.GLOBAL_MAID_SOUND_FREQUENCY.getDefault())
                .setTooltip(tip("maid.global_maid_sound_frequency"))
                .setSaveConsumer(local(MaidConfig.GLOBAL_MAID_SOUND_FREQUENCY)).build());
        sound.add(entries.startBooleanToggle(tr("maid.global_maid_show_chat_bubble"),
                        MaidConfig.GLOBAL_MAID_SHOW_CHAT_BUBBLE.get())
                .setDefaultValue(MaidConfig.GLOBAL_MAID_SHOW_CHAT_BUBBLE.getDefault())
                .setTooltip(tip("maid.global_maid_show_chat_bubble"))
                .setSaveConsumer(local(MaidConfig.GLOBAL_MAID_SHOW_CHAT_BUBBLE)).build());
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

        SubCategoryBuilder voice = sub(entries, "personal.voice_input");
        voice.add(new ActionButtonListEntry(
                Component.translatable(CATEGORY + "personal.voice_input"),
                Component.translatable(CATEGORY + "open_voice_input"),
                MenuIntegration::openVoiceInputSettings));
        category.addEntry(voice.build());
    }

    private static void addServerRules(ConfigBuilder root, ConfigEntryBuilder entries,
                                       ServerRulesClientCache.Session session) {
        ConfigCategory category = root.getOrCreateCategory(Component.translatable(CATEGORY + "server_rules"));
        if (!ServerRulesClientCache.isIntegratedServer()) {
            category.addEntry(entries.startTextDescription(Component.translatable(
                    CATEGORY + "server_rules.dedicated_reload").withStyle(ChatFormatting.YELLOW)).build());
        }

        SubCategoryBuilder basic = sub(entries, "server.maid_basic");
        basic.add(serverString(entries, "maid.maid_tamed_item", MaidConfig.MAID_TAMED_ITEM, session));
        basic.add(serverString(entries, "maid.maid_temptation_item", MaidConfig.MAID_TEMPTATION_ITEM, session));
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

        addAISettings(category, entries, session);

        SubCategoryBuilder experimental = sub(entries, "server.experimental");
        experimental.add(serverBoolean(entries, "experimental.smooth_follow", ExperimentalConfig.SMOOTH_FOLLOW, session));
        category.addEntry(experimental.build());
    }

    private static void addAISettings(ConfigCategory category, ConfigEntryBuilder entries,
                                      ServerRulesClientCache.Session session) {
        SubCategoryBuilder llm = entries.startSubCategory(tr("global_ai.llm")).setExpanded(false);
        llm.add(serverBoolean(entries, "global_ai.llm_enable", AIConfig.LLM_ENABLED, session));
        llm.add(serverBoolean(entries, "global_ai.auto_gen_setting_enabled", AIConfig.AUTO_GEN_SETTING_ENABLED, session));
        llm.add(serverString(entries, "global_ai.llm_proxy_address", AIConfig.LLM_PROXY_ADDRESS, session));
        llm.add(serverInt(entries, "global_ai.maid_history_compress_token_limit",
                AIConfig.MAID_HISTORY_COMPRESS_TOKEN_LIMIT, 8, 1024, session));
        llm.add(serverInt(entries, "global_ai.max_tokens_per_player",
                AIConfig.MAX_TOKENS_PER_PLAYER, 1, Integer.MAX_VALUE, session));
        category.addEntry(llm.build());

        SubCategoryBuilder tts = entries.startSubCategory(tr("global_ai.tts")).setExpanded(false);
        tts.add(serverBoolean(entries, "global_ai.tts_enable", AIConfig.TTS_ENABLED, session));
        tts.add(serverString(entries, "global_ai.tts_language", AIConfig.TTS_LANGUAGE, session));
        tts.add(serverString(entries, "global_ai.tts_proxy_address", AIConfig.TTS_PROXY_ADDRESS, session));
        category.addEntry(tts.build());

        SubCategoryBuilder stt = entries.startSubCategory(
                Component.translatable(CATEGORY + "server_stt.section")).setExpanded(false);
        stt.add(entries.startBooleanToggle(
                        Component.translatable(CATEGORY + "server_stt.provide").withStyle(ChatFormatting.RED),
                        session.getBoolean(ServerConfig.PROVIDE_SERVER_STT))
                .setDefaultValue(ServerConfig.PROVIDE_SERVER_STT.getDefault())
                .setTooltip(Component.translatable(CATEGORY + "server_stt.provide.tooltip"))
                .setSaveConsumer(value -> session.set(ServerConfig.PROVIDE_SERVER_STT, value)).build());
        stt.add(entries.startBooleanToggle(Component.translatable(CATEGORY + "server_stt.proxy_compat"),
                        session.getBoolean(ServerConfig.PROXY_SERVER_COMPATIBILITY))
                .setDefaultValue(ServerConfig.PROXY_SERVER_COMPATIBILITY.getDefault())
                .setTooltip(Component.translatable(CATEGORY + "server_stt.proxy_compat.tooltip"))
                .setSaveConsumer(value -> session.set(ServerConfig.PROXY_SERVER_COMPATIBILITY, value)).build());
        AbstractConfigListEntry<ServerSTTApiType> provider = entries.startEnumSelector(
                        Component.translatable(CATEGORY + "server_stt.provider"),
                        ServerSTTApiType.class, session.getServerSttType())
                .setDefaultValue(ServerConfig.SERVER_STT_TYPE.getDefault())
                .setEnumNameProvider(value -> Component.translatable(
                        "ai.touhou_little_maid.chat.site.%s.name".formatted(((ServerSTTApiType) value).siteId())))
                .setSaveConsumer(value -> session.set(ServerConfig.SERVER_STT_TYPE, value)).build();
        stt.add(provider);
        stt.add(new ActionButtonListEntry(
                Component.translatable(CATEGORY + "server_stt.configuration"),
                Component.translatable(CATEGORY + "server_stt.configure"),
                Component.translatable(ServerRulesClientCache.isIntegratedServer()
                        ? CATEGORY + "server_stt.site_apply.integrated"
                        : CATEGORY + "server_stt.site_apply.dedicated"),
                () -> openServerSTTEditor(provider.getValue())));
        category.addEntry(stt.build());
    }

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

    private static void openVoiceInputSettings() {
        Minecraft minecraft = Minecraft.getInstance();
        Screen parent = minecraft.screen;
        minecraft.setScreen(AIChatSettingsHubScreen.openSTTConfig(parent));
    }

    private static void openServerSTTEditor(ServerSTTApiType type) {
        Minecraft minecraft = Minecraft.getInstance();
        STTSite site = ServerRulesClientCache.serverSttSites().get(type.siteId());
        if (site == null) {
            if (minecraft.player != null) {
                minecraft.player.displayClientMessage(
                        Component.translatable(CATEGORY + "server_stt.site_unavailable").withStyle(ChatFormatting.RED), false);
            }
            return;
        }
        Screen parent = minecraft.screen;
        minecraft.setScreen(new STTSiteEditorScreen(parent, site,
                updated -> ClientPlayNetworking.send(SaveSTTSitePacket.update(updated))));
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

    private static <T> Consumer<T> local(ModConfigSpec.ConfigValue<T> value) {
        return value::set;
    }

    private static AbstractConfigListEntry<Boolean> localBoolean(ConfigEntryBuilder entries, String key,
                                                                 ModConfigSpec.ConfigValue<Boolean> value) {
        return entries.startBooleanToggle(tr(key), value.get())
                .setDefaultValue(value.getDefault()).setTooltip(tip(key)).setSaveConsumer(local(value)).build();
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

    private static AbstractConfigListEntry<String> serverString(ConfigEntryBuilder entries, String key,
                                                                ModConfigSpec.ConfigValue<String> value,
                                                                ServerRulesClientCache.Session session) {
        return entries.startTextField(tr(key), session.getString(value))
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
}
