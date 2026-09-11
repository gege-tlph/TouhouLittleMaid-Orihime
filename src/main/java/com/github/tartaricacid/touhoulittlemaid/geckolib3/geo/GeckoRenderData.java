package com.github.tartaricacid.touhoulittlemaid.geckolib3.geo;

import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.animated.GeoModelState;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.model.provider.data.EntityModelData;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.function.BiConsumer;

/**
 * 如果要添加与特定实体渲染相关的字段，不要加在这里，应该：
 * 1. 另起一个类 class GeckoXXXRenderData extends GeckoRenderData
 * 2. 实现 GeckoXXXEntity 的 createRenderData 和 extractRenderData 函数
 **/
public class GeckoRenderData implements AutoCloseable {
    /**
     * 声明为接口而非具体的 {@link GeoModelState}，使挂件 layer（只经由
     * {@link IGeoLocatorSource} 读取骨骼定位）也能在外部渲染器（如 YSM 身体接管）
     * 接管本体渲染时复用——见 {@code YsmMaidLayerBridge}。
     * <p>
     * 池化归还路径（{@link #returnFunc}/{@link #close()}）仍然只认 TLM 自己的
     * {@link GeoModelState} 实例：只有 {@link com.github.tartaricacid.touhoulittlemaid.geckolib3.core.AnimatableEntity}
     * 的 gecko 取模路径会设置 {@link #returnFunc}，且该路径下 {@link #modelState}
     * 恒为 {@code GeoModelState}；外部渲染器路径永远不设置 {@link #returnFunc}，
     * 也永远不调用 {@link #close()}，所以 {@link #close()} 里的强转是安全的。
     */
    public IGeoLocatorSource modelState;
    public EntityModelData modelData;
    public Identifier texture;
    public RenderContext ctx;

    public int color = 0xFFFFFFFF;
    public int overlayUV = OverlayTexture.NO_OVERLAY;

    @Nullable
    public Matrix4f transform;

    public BiConsumer<RenderContext, GeoModelState> returnFunc;

    public boolean isClosed() {
        return returnFunc == null;
    }

    @Override
    public void close() {
        if (returnFunc != null) {
            // 安全性见 modelState 字段上的说明：能走到这里说明 returnFunc 是
            // AnimatableEntity 设的，modelState 必然还是它自己塞进来的 GeoModelState。
            returnFunc.accept(ctx, (GeoModelState) modelState);
            modelState = null;
            returnFunc = null;
        }
    }
}
