package com.github.tartaricacid.touhoulittlemaid.ai.manager.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class MaidAIChatSerializable {
    public static final String NO_TTS_SITE = "__none__";

    public String llmSite = "";
    public String llmModel = "";

    public String ttsSite = "";
    public String ttsModel = "";
    public String ttsLanguage = "";
    public String chatLanguage = "";

    public String ownerName = "";
    public String customSetting = "";

    /**
     * 哨兵值，如果为此值，说明此时对当前女仆禁用 TTS 功能
     */
    public static boolean isNoTTSSite(String siteId) {
        return NO_TTS_SITE.equals(siteId);
    }

    public void decode(FriendlyByteBuf buf) {
        llmSite = buf.readUtf();
        llmModel = buf.readUtf();
        ttsSite = buf.readUtf();
        ttsModel = buf.readUtf();
        ttsLanguage = buf.readUtf();
        chatLanguage = buf.readUtf();
        ownerName = buf.readUtf();
        customSetting = buf.readUtf();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(llmSite);
        buf.writeUtf(llmModel);
        buf.writeUtf(ttsSite);
        buf.writeUtf(ttsModel);
        buf.writeUtf(ttsLanguage);
        buf.writeUtf(chatLanguage);
        buf.writeUtf(ownerName);
        buf.writeUtf(customSetting);
    }

    public void copyFrom(MaidAIChatSerializable data) {
        llmSite = data.llmSite;
        llmModel = data.llmModel;
        ttsSite = data.ttsSite;
        ttsModel = data.ttsModel;
        ttsLanguage = data.ttsLanguage;
        chatLanguage = data.chatLanguage;
        ownerName = data.ownerName;
        customSetting = data.customSetting;
    }

    public CompoundTag readFromTag(CompoundTag tag) {
        if (tag.contains("MaidAIChat")) {
            tag.getCompound("MaidAIChat").ifPresent(data -> {
                llmSite = data.getString("LLMSite").orElse("");
                llmModel = data.getString("LLMModel").orElse("");
                ttsSite = data.getString("TTSSiteName").orElse("");
                ttsModel = data.getString("TTSModel").orElse("");
                ttsLanguage = data.getString("TTSLanguage").orElse("");
                chatLanguage = data.getString("ChatLanguage").orElse("");
                ownerName = data.getString("OwnerName").orElse("");
                customSetting = data.getString("CustomSetting").orElse("");
            });
        }
        return tag;
    }

    public CompoundTag writeToTag(CompoundTag tag) {
        CompoundTag data = new CompoundTag();
        {
            data.putString("LLMSite", llmSite);
            data.putString("LLMModel", llmModel);
            data.putString("TTSSiteName", ttsSite);
            data.putString("TTSModel", ttsModel);
            data.putString("TTSLanguage", ttsLanguage);
            data.putString("ChatLanguage", chatLanguage);
            data.putString("OwnerName", ownerName);
            data.putString("CustomSetting", customSetting);
        }
        tag.put("MaidAIChat", data);
        return tag;
    }

    // 1.21.11 entity-save 路径（ValueOutput/ValueInput）。写出与 writeToTag/readFromTag 完全相同的
    // "MaidAIChat" 子 compound（child + 8 个 putString），逐字节同格式。
    // ⚠️ writeToTag/readFromTag(CompoundTag) 仍服务于网络同步（SyncMaidAIDataPacket，payload 是 CompoundTag）；
    //    两条路径必须保持字段一致 —— 新增字段时两处都要改。
    public void save(ValueOutput output) {
        ValueOutput data = output.child("MaidAIChat");
        data.putString("LLMSite", llmSite);
        data.putString("LLMModel", llmModel);
        data.putString("TTSSiteName", ttsSite);
        data.putString("TTSModel", ttsModel);
        data.putString("TTSLanguage", ttsLanguage);
        data.putString("ChatLanguage", chatLanguage);
        data.putString("OwnerName", ownerName);
        data.putString("CustomSetting", customSetting);
    }

    public void read(ValueInput input) {
        input.child("MaidAIChat").ifPresent(data -> {
            llmSite = data.getStringOr("LLMSite", "");
            llmModel = data.getStringOr("LLMModel", "");
            ttsSite = data.getStringOr("TTSSiteName", "");
            ttsModel = data.getStringOr("TTSModel", "");
            ttsLanguage = data.getStringOr("TTSLanguage", "");
            chatLanguage = data.getStringOr("ChatLanguage", "");
            ownerName = data.getStringOr("OwnerName", "");
            customSetting = data.getStringOr("CustomSetting", "");
        });
    }
}