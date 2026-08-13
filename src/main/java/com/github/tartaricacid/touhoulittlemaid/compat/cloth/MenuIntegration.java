package com.github.tartaricacid.touhoulittlemaid.compat.cloth;

import com.github.tartaricacid.touhoulittlemaid.api.event.client.AddClothConfigEvent;
import com.github.tartaricacid.touhoulittlemaid.config.subconfig.MaidConfig;
import com.github.tartaricacid.touhoulittlemaid.config.subconfig.MiscConfig;
import com.github.tartaricacid.touhoulittlemaid.config.subconfig.RenderConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.network.chat.Component;

/**
 * 本地个人配置的 Cloth 菜单。
 *
 * <p>⚠️ <b>这里少了 36 个条目，是有意摘掉的，不是漏搬</b>：女仆范围 / 攻击与进食黑名单 / 表情包权重 /
 * 椅子整节 / 妖精与神社灯 / 首次进服赠品 / 稻草人范围——它们已成为**存档级的服务器权威世界规则**
 * （{@code ServerRuleConfig}），值不再存在于本端可写的配置文件里，`set()`/`save()` 在这里无处可落。</p>
 *
 * <p><b>恢复锚点</b>：行为基准 {@code port/1.21.11-fabric} 的本文件把菜单重构成
 * 「个人设置 / 服务器规则 / 服务器维护」三段，服务器那两段用 {@code ServerRulesClientCache.Session}
 * 攒改动、按 {@code canEdit()} 判权限、保存时发包给服务端。那套东西依赖网络层
 * （{@code ServerRulesClientCache} + 保存包 + 权限判定 + {@code ActionButtonListEntry}），
 * 属审计 §3.A 的下一刀。**那一刀落地时必须回来把这 36 项装进「服务器规则」段**，
 * 对应用例是 §9 的 {@code RuleStagingSessionTest} 与 {@code ServerRulesSaveAuthorityContractTest}。
 * 在此之前，世界规则只能改存档的 {@code serverconfig/touhou_little_maid-server.toml}。</p>
 */
public class MenuIntegration {
    public static ConfigBuilder getConfigBuilder() {
        ConfigBuilder root = ConfigBuilder.create().setTitle(Component.literal("Touhou Little Maid"));
        root.setGlobalized(true);
        root.setGlobalizedExpanded(false);
        ConfigEntryBuilder entryBuilder = root.entryBuilder();
        maidConfig(root, entryBuilder);
        miscConfig(root, entryBuilder);
        renderConfig(root, entryBuilder);
        GlobalAIIntegration.aiChat(root, entryBuilder);
        AddClothConfigEvent.CALLBACK.invoker().post(new AddClothConfigEvent(root, entryBuilder));
        return root;
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

        maid.addEntry(entryBuilder.startIntField(Component.translatable("config.touhou_little_maid.maid.maid_gun_long_distance"), MaidConfig.MAID_GUN_LONG_DISTANCE.get())
                .setDefaultValue(64).setMin(0).setMax(512)
                .setTooltip(Component.translatable("config.touhou_little_maid.maid.maid_gun_long_distance.tooltip"))
                .setSaveConsumer(i -> {
                    MaidConfig.MAID_GUN_LONG_DISTANCE.set(i);
                    MaidConfig.MAID_GUN_LONG_DISTANCE.save();
                }).build());

        maid.addEntry(entryBuilder.startIntField(Component.translatable("config.touhou_little_maid.maid.maid_gun_medium_distance"), MaidConfig.MAID_GUN_MEDIUM_DISTANCE.get())
                .setDefaultValue(48).setMin(0).setMax(512)
                .setTooltip(Component.translatable("config.touhou_little_maid.maid.maid_gun_medium_distance.tooltip"))
                .setSaveConsumer(i -> {
                    MaidConfig.MAID_GUN_MEDIUM_DISTANCE.set(i);
                    MaidConfig.MAID_GUN_MEDIUM_DISTANCE.save();
                }).build());

        maid.addEntry(entryBuilder.startIntField(Component.translatable("config.touhou_little_maid.maid.maid_gun_near_distance"), MaidConfig.MAID_GUN_NEAR_DISTANCE.get())
                .setDefaultValue(32).setMin(0).setMax(512)
                .setTooltip(Component.translatable("config.touhou_little_maid.maid.maid_gun_near_distance.tooltip"))
                .setSaveConsumer(i -> {
                    MaidConfig.MAID_GUN_NEAR_DISTANCE.set(i);
                    MaidConfig.MAID_GUN_NEAR_DISTANCE.save();
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
