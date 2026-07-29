package com.github.tartaricacid.touhoulittlemaid.client.event;

import com.github.tartaricacid.touhoulittlemaid.api.event.client.RenderMaidEvent;
import com.github.tartaricacid.touhoulittlemaid.client.model.EasterEggModel;
import com.github.tartaricacid.touhoulittlemaid.client.resource.models.PlayerMaidModels;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.Component;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;

import static com.github.tartaricacid.touhoulittlemaid.client.resource.loader.CustomPackLoader.MAID_MODELS;

@Environment(EnvType.CLIENT)
public final class SpecialMaidRenderEvent {
    public static final String EASTER_EGG_MODEL = "touhou_little_maid:easter_egg_model";
    /**
     * EMCAScript 6 箭头函数表达式风格的前缀，不错吧
     */
    private static final String PLAYER_NAME_PREFIX = "=>";

    //@SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderPlayerNamedMaid(RenderMaidEvent event) {
        Component customName = event.getMaid().asEntity().getCustomName();
        if (customName == null) {
            return;
        }
        String name = customName.getString();
        if (StringUtils.isNotBlank(name) && name.startsWith(PLAYER_NAME_PREFIX)) {
            String playerName = name.substring(2);
            RenderMaidEvent.ModelData data = event.getModelData();
            data.setModel(PlayerMaidModels.model(playerName));
            data.setAnimations(PlayerMaidModels.animations());
            data.setInfo(PlayerMaidModels.info(playerName));
            event.setCanceled(true);
        }
    }

    //@SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onRenderEncryptNamedMaid(RenderMaidEvent event) {
        Component customName = event.getMaid().asEntity().getCustomName();
        if (customName == null) {
            return;
        }
        String name = customName.getString();
        if (StringUtils.isNotBlank(name)) {
            MAID_MODELS.getEasterEggEncryptTagModelId(DigestUtils.sha1Hex(name)).ifPresent(modelId -> modelDataSet(event, modelId));
        }
    }

    //@SubscribeEvent(priority = EventPriority.LOW)
    public static void onRenderNormalNamedMaid(RenderMaidEvent event) {
        Component customName = event.getMaid().asEntity().getCustomName();
        if (customName == null) {
            return;
        }
        String name = customName.getString();
        if (StringUtils.isNotBlank(name)) {
            MAID_MODELS.getEasterEggNormalTagModelId(name).ifPresent(modelId -> modelDataSet(event, modelId));
        }
    }

    //@SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderEasterEggModel(RenderMaidEvent event) {
        String id = event.getMaid().getModelId();
        if (EASTER_EGG_MODEL.equals(id)) {
            RenderMaidEvent.ModelData data = event.getModelData();
            data.setModel(EasterEggModel.model());
            data.setAnimations(Collections.emptyList());
            data.setInfo(EasterEggModel.info());
            event.setCanceled(true);
        }
    }

    /**
     * origin：彩蛋数据在装载期整体存为 MaidModels.ModelData，命中 tag 后用其覆写事件数据。
     * <p>
     * 新装载器（MaidPackLoader.putEasterEggData）只登记 tag → modelId，模型/信息/动画照常入库，
     * 故此处按 modelId 反查后覆写，语义与 origin 一致：
     * model 无条件覆写（Gecko 彩蛋无 bedrock 模型 → 覆写为 null，同 origin data.getModel()==null）、
     * info 覆写、动画仅在非空时覆写。
     */
    private static void modelDataSet(RenderMaidEvent event, String modelId) {
        RenderMaidEvent.ModelData rawData = event.getModelData();
        rawData.setModel(MAID_MODELS.getModel(modelId).orElse(null));
        MAID_MODELS.getInfo(modelId).ifPresent(rawData::setInfo);
        MAID_MODELS.getAnimation(modelId).ifPresent(animations -> {
            if (!animations.isEmpty()) {
                rawData.setAnimations(animations);
            }
        });
        event.setCanceled(true);
    }
}
