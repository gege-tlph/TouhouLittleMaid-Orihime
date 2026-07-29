package com.github.tartaricacid.touhoulittlemaid.client.tooltip;

import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.github.tartaricacid.touhoulittlemaid.client.resource.loader.CustomPackLoader;
import com.github.tartaricacid.touhoulittlemaid.client.resource.pojo.MaidModelInfo;
import com.github.tartaricacid.touhoulittlemaid.compat.ysm.YsmCompat;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.inventory.tooltip.ItemMaidTooltip;
import com.github.tartaricacid.touhoulittlemaid.inventory.tooltip.YsmMaidInfo;
import com.github.tartaricacid.touhoulittlemaid.util.EntityCacheUtil;
import com.github.tartaricacid.touhoulittlemaid.util.ParseI18n;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.StringUtils;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

import static com.github.tartaricacid.touhoulittlemaid.util.EntityCacheUtil.clearMaidDataResidue;

public class ClientMaidTooltip implements ClientTooltipComponent {
    private static final String EASTER_EGG_MODEL = "touhou_little_maid:easter_egg_model";
    private final @Nullable MaidModelInfo info;
    private final YsmMaidInfo ysmMaidInfo;
    private final MutableComponent name;
    private final String customName;

    public ClientMaidTooltip(ItemMaidTooltip tooltip) {
        this.info = CustomPackLoader.MAID_MODELS.getInfo(tooltip.modelId()).orElse(null);
        this.ysmMaidInfo = Objects.requireNonNullElse(tooltip.ysmMaidInfo(), YsmMaidInfo.EMPTY);
        this.name = getName(this.info, this.ysmMaidInfo);
        this.customName = tooltip.customName();
    }

    public MutableComponent getName(MaidModelInfo info, YsmMaidInfo ysmMaidInfo) {
        // 优先使用 YSM 模型名称
        if (YsmCompat.isInstalled() && ysmMaidInfo.isYsmModel()) {
            ClientLevel level = Minecraft.getInstance().level;
            if (level == null) {
                return Component.empty();
            }
            MutableComponent name = parseComponentJson(ysmMaidInfo.name(), level.registryAccess());
            if (name == null || name.equals(Component.empty())) {
                return Component.literal(ysmMaidInfo.modelId());
            }
            return name;
        }

        // 然后才是默认模型名
        if (info == null) {
            return Component.empty();
        }
        return Component.translatable(ParseI18n.getI18nKey(info.getName()));
    }

    // 1.21.11: ComponentSerialization.fromJson 移除 → CODEC + createSerializationContext(JsonOps)（同 EntityMaid YSM 名解析）
    @Nullable
    private static MutableComponent parseComponentJson(String json, RegistryAccess access) {
        if (StringUtils.isBlank(json)) {
            return null;
        }
        return ComponentSerialization.CODEC
                .parse(access.createSerializationContext(JsonOps.INSTANCE), JsonParser.parseString(json))
                .result()
                .map(c -> c instanceof MutableComponent mc ? mc : c.copy())
                .orElse(null);
    }

    @Override
    // 1.21.11: getHeight() 现需 Font 参数
    public int getHeight(Font font) {
        return 70;
    }

    @Override
    public int getWidth(Font font) {
        return Math.max(font.width(this.name), 50);
    }

    @Override
    // 1.21.11: renderImage 新增 w/h 两参
    public void renderImage(Font font, int pX, int pY, int width, int height, GuiGraphics guiGraphics) {
        if (info == null) {
            return;
        }
        Level world = Minecraft.getInstance().level;
        if (world == null) {
            return;
        }

        RegistryAccess access = Minecraft.getInstance().level.registryAccess();

        MutableComponent customNameComponent = null;
        if (StringUtils.isNotBlank(customName)) {
            customNameComponent = parseComponentJson(customName, access);
            if (customNameComponent != null) {
                guiGraphics.drawString(font, customNameComponent.withStyle(ChatFormatting.GRAY), pX, pY + 2, 0xFFFFFFFF);
            }
        } else {
            guiGraphics.drawString(font, name.withStyle(ChatFormatting.GRAY), pX, pY + 2, 0xFFFFFFFF);
        }

        // 注意：形参 width 是 1.21.11 renderImage 新增的调用方给定宽度；
        // 此处沿用 HEAD 的行为，仍按自身 getWidth(font) 计算，故用独立变量名避免遮蔽。
        int selfWidth = this.getWidth(font);
        int posY = pY + 64;
        EntityMaid maid;
        try {
            maid = (EntityMaid) EntityCacheUtil.ENTITY_CACHE.get(EntityMaid.TYPE, () -> {
                // 1.21.11: EntityType.create(Level) → create(Level, EntitySpawnReason)
                Entity e = EntityMaid.TYPE.create(world, EntitySpawnReason.LOAD);
                return Objects.requireNonNullElseGet(e, () -> new EntityMaid(world));
            });
        } catch (ExecutionException | ClassCastException e) {
            TouhouLittleMaid.LOGGER.error("Failed to render maid tooltip preview", e);
            return;
        }
        clearMaidDataResidue(maid, false);
        if (StringUtils.isNotBlank(customName)) {
            maid.setCustomName(customNameComponent);
        }
        if (info.getEasterEgg() != null) {
            maid.setModelId(EASTER_EGG_MODEL);
        } else {
            maid.setModelId(info.getModelId().toString());
        }

        // YSM 渲染运用
        if (YsmCompat.isInstalled() && ysmMaidInfo.isYsmModel()) {
            maid.setIsYsmModel(true);
            maid.setYsmModel(ysmMaidInfo.modelId(), ysmMaidInfo.textureId(), this.name);
        } else {
            maid.setIsYsmModel(false);
        }

        int x1 = pX, y1 = posY - 50, x2 = pX + selfWidth, y2 = posY;
        int scale = (int) (25 * info.getRenderItemScale());
        float rotation = (float) Math.toRadians((System.currentTimeMillis() / 25.0) % 360);
        Quaternionf pose = new Quaternionf().rotateZ((float) Math.PI).rotateY(rotation);
        Vector3f translation = new Vector3f(0, (y2 - y1) / (2.0F * scale), 0);
        float partialTick = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
        EntityRenderState renderState = extractRenderState(maid, partialTick);
        guiGraphics.enableScissor(x1, y1, x2, y2);
        guiGraphics.submitEntityRenderState(renderState, scale, translation, pose, null, x1, y1, x2, y2);
        guiGraphics.disableScissor();
    }

    private static EntityRenderState extractRenderState(EntityMaid maid, float partialTick) {
        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        EntityRenderer<? super EntityMaid, ?> renderer = dispatcher.getRenderer(maid);
        EntityRenderState state = renderer.createRenderState(maid, partialTick);
        state.lightCoords = 0xf000f0;
        state.shadowPieces.clear();
        state.outlineColor = 0;
        return state;
    }
}
