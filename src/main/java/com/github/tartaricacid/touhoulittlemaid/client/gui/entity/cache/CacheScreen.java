package com.github.tartaricacid.touhoulittlemaid.client.gui.entity.cache;

import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.github.tartaricacid.touhoulittlemaid.client.renderer.texture.CacheIconTexture;
import com.github.tartaricacid.touhoulittlemaid.client.resource.pojo.IModelInfo;
import com.github.tartaricacid.touhoulittlemaid.util.IconCache;
import com.github.tartaricacid.touhoulittlemaid.util.migrate.ScreenUtil;
import com.mojang.blaze3d.platform.NativeImage;
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
     * 故在帧 k 的 extract 里发起的截图，读到的是<b>帧 k-1</b> 的完整画面。
     * <p>
     * 双背景差分抠像（2026-08-15 用户批准的超基准改进，动机见 {@link IconCache}）采用<b>同帧双幕</b>：
     * 每帧并排绘制绿幕块与品红幕块、同一模型渲染两次——同帧 = 同一动画时间戳，姿态逐位相同。
     * 首版曾跨帧取两张（绿一帧、品红一帧），bedrock 模型近乎静止没暴露问题，gecko 模型的
     * 骨骼动画在帧间位移全部被差分误读成透明度，实测出鬼影——同帧双幕把「时间」这个变量整个消掉。
     * 捕获推迟到第 {@link #CAPTURE_AT_FRAME} 个绘制帧发起（读到的是其前一帧画面），
     * 给 gecko 模型的动画绑定/异步初始化留缓冲。回读经 fence 回调
     * （RenderSystem.executePendingTasks，渲染线程）送达后差分合成、registerAndLoad、进入下一个模型。
     */
    private static final int CAPTURE_AT_FRAME = 3;

    private E processingInfo = null;
    private int framesDrawn = 0;
    /**
     * 屏关闭后仍可能有 fence 回调迟到，据此丢弃并释放，防止 NativeImage 泄漏
     */
    private boolean closed = false;

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
            this.framesDrawn = 0;
        }
        E modelInfo = this.processingInfo;
        if (modelInfo == null) {
            return;
        }
        // 26.1.2：Window.getGuiScale() 返回 int（1.21.11 是 double），除法必须走浮点，否则整数截断
        int guiScale = Screens.getMinecraft(this).getWindow().getGuiScale();
        int scaleModified = (int) Math.ceil(256.0 / guiScale);
        int magentaX = scaleModified + 2;

        // 同帧双幕：左绿右品红，各画一遍同一模型；等待回读期间每帧重复，画面内容保持稳定
        graphics.fill(0, 0, scaleModified, scaleModified + 2, IconCache.BACKGROUND_GREEN);
        this.drawEntity(graphics, 0, 0, modelInfo, scaleModified);
        graphics.fill(magentaX, 0, magentaX + scaleModified, scaleModified + 2, IconCache.BACKGROUND_MAGENTA);
        this.drawEntity(graphics, magentaX, 0, modelInfo, scaleModified);

        if (this.framesDrawn == CAPTURE_AT_FRAME) {
            IconCache.capturePair(256, magentaX * guiScale,
                    (greenShot, magentaShot) -> acceptPair(modelInfo, greenShot, magentaShot));
        }
        if (this.framesDrawn <= CAPTURE_AT_FRAME) {
            this.framesDrawn++;
        }
    }

    private void acceptPair(E modelInfo, NativeImage greenShot, NativeImage magentaShot) {
        // fence 回调在渲染线程执行（executePendingTasks），与 extract 无并发
        if (this.closed) {
            greenShot.close();
            magentaShot.close();
            return;
        }
        NativeImage combined = IconCache.combine(greenShot, magentaShot);
        greenShot.close();
        magentaShot.close();

        CacheIconTexture cacheIconTexture = new CacheIconTexture(modelInfo.getModelId(), combined);
        // 26.1.2 的 register 只入表不上传（TextureManager 反编译实查），
        // ReloadableTexture 必须走 registerAndLoad 当场 load + 上传；本回调已在渲染线程，满足其线程约束
        Minecraft.getInstance().getTextureManager().registerAndLoad(modelInfo.getCacheIconId(), cacheIconTexture);
        CacheIconManager.markIconCached(modelInfo.getCacheIconId());
        this.processingInfo = null;
    }

    @Override
    public void removed() {
        super.removed();
        this.closed = true;
    }

    public interface EntityRender<T extends LivingEntity, E extends IModelInfo> {
        void render(GuiGraphicsExtractor graphics, int posX, int posY, E modelInfo, int scaleModified, T entity);
    }
}
