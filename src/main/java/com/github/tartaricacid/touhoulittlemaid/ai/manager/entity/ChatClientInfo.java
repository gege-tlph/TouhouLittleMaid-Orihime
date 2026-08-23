package com.github.tartaricacid.touhoulittlemaid.ai.manager.entity;

import com.github.tartaricacid.touhoulittlemaid.client.resource.loader.CustomPackLoader;
import com.github.tartaricacid.touhoulittlemaid.client.resource.pojo.MaidModelInfo;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.util.ParseI18n;
import com.google.common.collect.Lists;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Optional;

public record ChatClientInfo(String language, String name, List<String> description) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(language);
        buf.writeUtf(name);
        buf.writeVarInt(description.size());
        for (String line : description) {
            buf.writeUtf(line);
        }
    }

    public static ChatClientInfo decode(FriendlyByteBuf buf) {
        String language = buf.readUtf();
        String name = buf.readUtf();
        int size = buf.readVarInt();
        List<String> description = Lists.newArrayListWithExpectedSize(size);
        for (int i = 0; i < size; i++) {
            description.add(buf.readUtf());
        }
        return new ChatClientInfo(language, name, description);
    }

    @Environment(EnvType.CLIENT)
    public static ChatClientInfo fromMaid(EntityMaid maid) {
        String language = getClientLanguage();
        String name = getMaidName(maid);
        List<String> description = getMaidDescription(maid);
        return new ChatClientInfo(language, name, description);
    }

    @Environment(EnvType.CLIENT)
    private static String getClientLanguage() {
        return Minecraft.getInstance().getLanguageManager().getSelected();
    }

    @Environment(EnvType.CLIENT)
    private static String getMaidName(EntityMaid maid) {
        return maid.getName().getString();
    }

    @Environment(EnvType.CLIENT)
    private static List<String> getMaidDescription(EntityMaid maid) {
        List<String> description = Lists.newArrayList();
        // 行为基准这里还有一条「YSM 模型不带描述」的短路。**26.1.2 的 Fabric 上不存在任何 YSM 实现**
        // （本体仅 NeoForge 且闭源，OpenYSM 无 26.x），整块 YSM 兼容按审计 §7.2 在移植范围之外，
        // 故本树没有 YsmCompat。YSM 前置就绪时连同那块兼容一起补回这条短路。
        Optional<MaidModelInfo> info = CustomPackLoader.MAID_MODELS.getInfo(maid.getModelId());
        if (info.isPresent()) {
            MaidModelInfo maidModelInfo = info.get();
            List<Component> parse = ParseI18n.parse(maidModelInfo.getDescription());
            parse.forEach(component -> description.add(component.getString()));
        }
        return description;
    }
}
