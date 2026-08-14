package com.github.tartaricacid.touhoulittlemaid.client.gui.entity.cache;

import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.github.tartaricacid.touhoulittlemaid.client.renderer.texture.CacheIconTexture;
import com.github.tartaricacid.touhoulittlemaid.client.resource.pojo.IModelInfo;
import com.github.tartaricacid.touhoulittlemaid.util.IconCache;
import com.github.tartaricacid.touhoulittlemaid.util.migrate.ScreenUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.time.StopWatch;

import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Environment(EnvType.CLIENT)
public class CacheScreen<T extends LivingEntity, E extends IModelInfo> extends Screen {
    protected final Screen parent;
    protected final Function<Level, T> entityFactory;
    protected final Queue<E> modelInfos;
    protected final EntityRender<T, E> entityRender;
    protected final int totalCount;
    protected final StopWatch stopWatch;

    /**
     * 26.1.2 截图时序（第三次重推导，据 Minecraft.runTick / BlockableEventLoop 反编译源实查）：
     * <p>
     * extract 阶段只把绘制记入渲染状态树，像素要到本帧稍后的 gameRenderer.render 才落入主 RenderTarget；
     * 且渲染线程上 {@code Minecraft.execute} 是<b>内联立即执行</b>（{@code scheduleExecutables()} =
     * {@code runningTask() || !isSameThread()}，帧循环里两者皆否）——1.21.11 版注释设想的
     * 「execute 延迟到下一帧回读」在此并不成立，照搬会把每个图标错位成前一个模型的画面。
     * <p>
     * 故改为显式帧计数：模型 M 的第 1 个 extract 帧只提交绘制；第 2 个 extract 帧<b>内联</b>发起截图——
     * 此刻主 RenderTarget 恰好完整持有上一帧（绿幕 + 模型 M + GUI）画面，且拷贝命令先于本帧渲染命令
     * 进入 GPU 命令流。回读回调经 RenderSystem.executePendingTasks 在渲染线程执行，
     * 满足 registerAndLoad 的渲染线程约束。等待回读期间每帧重复提交同一模型，画面内容保持稳定。
     * 观察行为（全部模型生成绿幕抠像图标）与 origin 一致，仅节奏为每模型约两帧 + fence 延迟。
     */
    private E processingInfo = null;
    private boolean modelSubmittedLastFrame = false;
    private boolean captureIssued = false;

    public CacheScreen(Screen parent, Queue<E> modelInfos, Function<Level, T> entityFactory, EntityRender<T, E> entityRender) {
        super(Component.literal("Cache Screen"));
        this.parent = parent;
        this.modelInfos = modelInfos;
        this.entityFactory = entityFactory;
        this.entityRender = entityRender;
        this.totalCount = modelInfos.size();
        this.stopWatch = StopWatch.createStarted();
    }

    private void drawEntity(GuiGraphicsExtractor graphics, int posX, int posY, E modelInfo, int scaleModified) {
        Level world = Screens.getMinecraft(this).level;
        if (world == null) {
            return;
        }
        T entity = entityFactory.apply(world);
        entityRender.render(graphics, posX, posY, modelInfo, scaleModified, entity);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        if (modelInfos.isEmpty() && processingInfo == null) {
            stopWatch.stop();
            double timeCost = stopWatch.getTime(TimeUnit.MILLISECONDS) / 1000.0;
            TouhouLittleMaid.LOGGER.info("Cache icon time: {} seconds", timeCost);
            ScreenUtil.setScreen(parent);
            return;
        }

        doCacheIcon(graphics);

        int finishSize = totalCount - modelInfos.size();
        graphics.centeredText(font, Component.translatable("gui.touhou_little_maid.cache_screen.progress", finishSize, totalCount), this.width / 2, this.height - 42, 0xFFFFFFFF);
        graphics.centeredText(font, Component.translatable("gui.touhou_little_maid.cache_screen.desc"), this.width / 2, this.height - 30, 0xFFFFFFFF);
    }

    protected void doCacheIcon(GuiGraphicsExtractor graphics) {
        if (this.processingInfo == null) {
            this.processingInfo = modelInfos.poll();
            this.modelSubmittedLastFrame = false;
            this.captureIssued = false;
        }
        E modelInfo = this.processingInfo;
        if (modelInfo == null) {
            return;
        }
        // 26.1.2：Window.getGuiScale() 返回 int（1.21.11 是 double），除法必须走浮点，否则整数截断
        int guiScale = Screens.getMinecraft(this).getWindow().getGuiScale();
        int scaleModified = (int) Math.ceil(256.0 / guiScale);

        // 等待回读期间每帧重复提交同一模型的绘制，保证截图落点帧的画面内容正确
        graphics.fill(0, 0, scaleModified, scaleModified + 2, IconCache.BACKGROUND_COLOR);
        this.drawEntity(graphics, 0, 0, modelInfo, scaleModified);

        if (this.modelSubmittedLastFrame && !this.captureIssued) {
            this.captureIssued = true;
            IconCache.exportImageFromScreenshot(256, IconCache.BACKGROUND_COLOR_SHIFTED, nativeImage -> {
                CacheIconTexture cacheIconTexture = new CacheIconTexture(modelInfo.getModelId(), nativeImage);
                // 26.1.2 的 register 只入表不上传（TextureManager 反编译实查），
                // ReloadableTexture 必须走 registerAndLoad 当场 load + 上传；
                // 本回调在渲染线程（executePendingTasks）执行，满足其渲染线程约束
                Minecraft.getInstance().getTextureManager().registerAndLoad(modelInfo.getCacheIconId(), cacheIconTexture);
                CacheIconManager.markIconCached(modelInfo.getCacheIconId());
                this.processingInfo = null;
            });
        } else {
            this.modelSubmittedLastFrame = true;
        }
    }

    public interface EntityRender<T extends LivingEntity, E extends IModelInfo> {
        void render(GuiGraphicsExtractor graphics, int posX, int posY, E modelInfo, int scaleModified, T entity);
    }
}
