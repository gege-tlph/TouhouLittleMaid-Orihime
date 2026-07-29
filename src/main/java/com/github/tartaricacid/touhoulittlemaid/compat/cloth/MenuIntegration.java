package com.github.tartaricacid.touhoulittlemaid.compat.cloth;

import com.github.tartaricacid.touhoulittlemaid.api.event.client.AddClothConfigEvent;
import com.github.tartaricacid.touhoulittlemaid.client.gui.entity.maid.ai.settings.AIChatSettingsHubScreen;
import com.github.tartaricacid.touhoulittlemaid.config.GeneralConfig;
import com.github.tartaricacid.touhoulittlemaid.config.ServerConfig;
import com.github.tartaricacid.touhoulittlemaid.config.subconfig.AIConfig;
import com.github.tartaricacid.touhoulittlemaid.config.subconfig.ChairConfig;
import com.github.tartaricacid.touhoulittlemaid.config.subconfig.ExperimentalConfig;
import com.github.tartaricacid.touhoulittlemaid.config.subconfig.MaidConfig;
import com.github.tartaricacid.touhoulittlemaid.config.subconfig.MiscConfig;
import com.github.tartaricacid.touhoulittlemaid.config.subconfig.RenderConfig;
import com.github.tartaricacid.touhoulittlemaid.config.subconfig.VanillaConfig;
import com.github.tartaricacid.touhoulittlemaid.init.registry.CompatRegistry;
import com.github.tartaricacid.touhoulittlemaid.network.client.config.ServerRulesClientCache;
import com.github.tartaricacid.touhoulittlemaid.network.message.ai.OpenAIConfigPacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder;
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

        // 唯一入口，且**对所有身份可见可点**：personal 这一栏是无条件添加的，
        // 而 server_rules 整栏由 canEdit() 门控——入口留在那边就只有管理员看得见。
        // 直接挂在栏目根上：一个只装着一颗按钮的折叠组是纯噪音。
        category.addEntry(new ActionButtonListEntry(
                Component.translatable(CATEGORY + "ai_settings"),
                Component.translatable(CATEGORY + "open_ai_settings"),
                MenuIntegration::openAiSettings));
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

        SubCategoryBuilder experimental = sub(entries, "server.experimental");
        experimental.add(serverBoolean(entries, "experimental.smooth_follow", ExperimentalConfig.SMOOTH_FOLLOW, session));
        category.addEntry(experimental.build());
    }

    /**
     * AI 那一组已从本菜单撤走，只留「个人设置」栏里的一颗跳转按钮（唯一入口）。
     *
     * <p><b>撤走的理由不是「这里放不下」，是「同一件事被劈成了两个入口」</b>：
     * 总开关、代理、token 上限在这里，站点与密钥在 mod 自己的屏里，
     * 管理员想让一个 LLM 跑起来必须去两个地方，而这两个地方连名字都不像是一回事。
     * 现在全部收拢到「AI 与语音设置」，Cloth 只当入口。</p>
     *
     * <p>在世界里必须走 {@link OpenAIConfigPacket} 这条与 T 屏齿轮相同的链路——
     * 权限判定与站点数据都在服务端，客户端自己开屏就只能开出「语音输入」那一栏
     * （单人档房主也会被错当成无权限，这正是曾经的实况）。
     * 不在世界里（标题屏进的 modmenu）才回落到纯本机的语音输入设置。</p>
     */
    private static void openAiSettings() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null && ClientPlayNetworking.canSend(OpenAIConfigPacket.TYPE)) {
            OpenAIConfigPacket.sendToServer();
            return;
        }
        minecraft.setScreen(AIChatSettingsHubScreen.openSTTConfig(minecraft.screen));
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
