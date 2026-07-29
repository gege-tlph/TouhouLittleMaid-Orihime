package com.github.tartaricacid.touhoulittlemaid.client.gui.entity.maid.ai;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public interface Translations {
    MutableComponent SITE_ID_NAME = Component.translatable("ai.touhou_little_maid.chat.settings.hub.site_id");
    MutableComponent URL_NAME = Component.translatable("ai.touhou_little_maid.chat.settings.hub.url");
    MutableComponent SECRET_ID_NAME = Component.translatable("ai.touhou_little_maid.chat.settings.hub.secret_id");
    MutableComponent SECRET_KEY_NAME = Component.translatable("ai.touhou_little_maid.chat.settings.hub.secret_key");
    MutableComponent ADD_MODEL_NAME = Component.translatable("ai.touhou_little_maid.chat.settings.hub.add_model");
    MutableComponent MODEL_NAME = Component.translatable("ai.touhou_little_maid.chat.settings.hub.model");
    MutableComponent MODELS_NAME = Component.translatable("ai.touhou_little_maid.chat.settings.hub.models");
    MutableComponent VOICES_NAME = Component.translatable("ai.touhou_little_maid.chat.settings.hub.voices");
    MutableComponent SAVE_NAME = Component.translatable("selectWorld.edit.save");
    MutableComponent ENG_SER_VICE_TYPE_NAME = Component.translatable("ai.touhou_little_maid.chat.settings.hub.eng_ser_vice_type");
    MutableComponent HOT_WORD_NAME = Component.translatable("ai.touhou_little_maid.chat.settings.hub.hot_word");

    MutableComponent SITE_ID_IS_EMPTY = Component.translatable("ai.touhou_little_maid.chat.settings.hub.site_id_is_empty");
    MutableComponent SITE_ID_ALREADY_EXISTS = Component.translatable("ai.touhou_little_maid.chat.settings.hub.site_id_already_exists");
    MutableComponent URL_IS_EMPTY = Component.translatable("ai.touhou_little_maid.chat.settings.hub.url_is_empty");
    MutableComponent MODEL_IS_EMPTY = Component.translatable("ai.touhou_little_maid.chat.settings.hub.model_is_empty");
    MutableComponent SECRET_ID_IS_EMPTY = Component.translatable("ai.touhou_little_maid.chat.settings.hub.secret_id_is_empty");
    MutableComponent SECRET_KEY_IS_EMPTY = Component.translatable("ai.touhou_little_maid.chat.settings.hub.secret_key_is_empty");
    MutableComponent APP_KEY_IS_EMPTY = Component.translatable("ai.touhou_little_maid.chat.settings.hub.app_key_is_empty");
    MutableComponent VOICE_IS_EMPTY = Component.translatable("ai.touhou_little_maid.chat.settings.hub.voice_is_empty");

    MutableComponent SITE_NAME = Component.translatable("ai.touhou_little_maid.chat.settings.hub.site").withStyle(ChatFormatting.UNDERLINE);
    MutableComponent SITE_LLM_NAME = Component.translatable("ai.touhou_little_maid.chat.settings.hub.site.llm");
    MutableComponent SITE_TTS_NAME = Component.translatable("ai.touhou_little_maid.chat.settings.hub.site.tts");

    MutableComponent VOICE_INPUT_NAME = Component.translatable("ai.touhou_little_maid.chat.settings.hub.voice_input").withStyle(ChatFormatting.UNDERLINE);
    MutableComponent SERVICE_NAME = Component.translatable("ai.touhou_little_maid.chat.settings.hub.service").withStyle(ChatFormatting.UNDERLINE);
    MutableComponent USAGE_NAME = Component.translatable("ai.touhou_little_maid.chat.settings.hub.usage").withStyle(ChatFormatting.UNDERLINE);
    MutableComponent USAGE_QUOTA_NAME = Component.translatable("ai.touhou_little_maid.chat.settings.hub.usage.quota");
    MutableComponent STT_CONFIG_NAME = Component.translatable("ai.touhou_little_maid.chat.settings.hub.stt.config");

    static MutableComponent llmEditorTitle(Object... args) {
        return Component.translatable("ai.touhou_little_maid.chat.settings.llm_editor.title", args);
    }

    static MutableComponent ttsEditorTitle(Object... args) {
        return Component.translatable("ai.touhou_little_maid.chat.settings.tts_editor.title", args);
    }

    static MutableComponent sttEditorTitle(Object... args) {
        return Component.translatable("ai.touhou_little_maid.chat.settings.stt_editor.title", args);
    }
}
