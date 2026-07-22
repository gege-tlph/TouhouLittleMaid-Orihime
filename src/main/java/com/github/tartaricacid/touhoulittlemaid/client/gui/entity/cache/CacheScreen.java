package com.github.tartaricacid.touhoulittlemaid.client.gui.entity.cache;

import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.github.tartaricacid.touhoulittlemaid.client.renderer.texture.CacheIconTexture;
import com.github.tartaricacid.touhoulittlemaid.client.resource.pojo.IModelInfo;
import com.github.tartaricacid.touhoulittlemaid.util.EntityCacheUtil;
import com.github.tartaricacid.touhoulittlemaid.util.IconCache;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.time.StopWatch;

import java.util.Queue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;


@Environment(EnvType.CLIENT)
public class CacheScreen<T extends LivingEntity, E extends IModelInfo> extends Screen {
    protected final Screen parent;
    protected final EntityType<T> entityType;
    protected final Queue<E> modelInfos;
    protected final EntityRender<T, E> entityRender;
    protected final int totalCount;
    protected final StopWatch stopWatch;


    private E processingInfo = null;
    private boolean captureScheduled = false;

    public CacheScreen(Screen parent, EntityType<T> entityType, Queue<E> modelInfos, EntityRender<T, E> entityRender) {
        super(Component.literal("Cache Screen"));
        this.parent = parent;
        this.entityType = entityType;
        this.modelInfos = modelInfos;
        this.entityRender = entityRender;
        this.totalCount = modelInfos.size();
        this.stopWatch = StopWatch.createStarted();
    }

    @SuppressWarnings("unchecked")
    private void drawEntity(GuiGraphics graphics, int posX, int posY, E modelInfo, int scaleModified) {
        Level world = Screens.getClient(this).level;
        if (world == null) {
            return;
        }
        T entity;
        try {
            entity = (T) EntityCacheUtil.ENTITY_CACHE.get(entityType, () -> entityType.create(world, EntitySpawnReason.COMMAND));
        } catch (ExecutionException | ClassCastException e) {
            e.fillInStackTrace();
            return;
        }
        entityRender.render(graphics, posX, posY, modelInfo, scaleModified, entity);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        if (modelInfos.isEmpty() && processingInfo == null) {
            stopWatch.stop();
            double timeCost = stopWatch.getTime(TimeUnit.MILLISECONDS) / 1000.0;
            TouhouLittleMaid.LOGGER.info("Cache icon time: {} seconds", timeCost);
            Minecraft.getInstance().setScreen(parent);
            return;
        }


        doCacheIcon(graphics);

        int finishSize = totalCount - modelInfos.size();
        graphics.drawCenteredString(font, Component.translatable("gui.touhou_little_maid.cache_screen.progress", finishSize, totalCount), this.width / 2, this.height - 42, 0xFFFFFFFF);
        graphics.drawCenteredString(font, Component.translatable("gui.touhou_little_maid.cache_screen.desc"), this.width / 2, this.height - 30, 0xFFFFFFFF);
    }

    protected void doCacheIcon(GuiGraphics graphics) {
        if (this.processingInfo == null) {
            this.processingInfo = modelInfos.poll();
            this.captureScheduled = false;
        }
        E modelInfo = this.processingInfo;
        if (modelInfo != null) {
            double guiScale = Minecraft.getInstance().getWindow().getGuiScale();
            int scaleModified = (int) Math.ceil((256 / guiScale));

            // 等待回读期间每帧重复提交同一模型的绘制，保证回读落点帧的画面内容正确
            graphics.fill(0, 0, scaleModified, scaleModified + 2, IconCache.BACKGROUND_COLOR);
            this.drawEntity(graphics, 0, 0, modelInfo, scaleModified);

            if (!this.captureScheduled) {
                this.captureScheduled = true;
                // execute 任务在下一帧 tick 前执行，此时主 RenderTarget 仍持有本帧（含 GUI）完整内容
                Minecraft.getInstance().execute(() ->
                        IconCache.exportImageFromScreenshot(256, IconCache.BACKGROUND_COLOR_SHIFTED, nativeImage -> {
                            Identifier modelId = modelInfo.getModelId();
                            CacheIconTexture cacheIconTexture = new CacheIconTexture(modelId, nativeImage);
                            Minecraft.getInstance().getTextureManager().register(modelInfo.getCacheIconId(), cacheIconTexture);
                            this.processingInfo = null;
                        }));
            }
        }
    }

    public interface EntityRender<T extends LivingEntity, E extends IModelInfo> {
        void render(GuiGraphics graphics, int posX, int posY, E modelInfo, int scaleModified, T entity);
    }
}
