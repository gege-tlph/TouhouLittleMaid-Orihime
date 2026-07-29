package com.github.tartaricacid.touhoulittlemaid.geckolib3.geo;

import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.animated.GeoModelState;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.animated.IGeoLocatorSource;
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
    public GeoModelState modelState;
    /**
     * 挂件 layer 的定位组来源；null = 用自带 gecko 的 {@link #modelState}（TLM 自身渲染路径恒为此）。
     *
     * <p>第三方模型系统（YSM）接管渲染时，模型状态是它那边的，{@link #modelState} 根本不存在，
     * 此时由接管方填一个 {@link com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.animated.LocationModelLocatorSource}
     * 进来，5 个挂件 layer 便无须知道自己正跑在哪套模型系统上。</p>
     */
    @Nullable
    public IGeoLocatorSource locatorSource;
    public EntityModelData modelData;
    public Identifier texture;
    public RenderContext ctx;

    public int color = 0xFFFFFFFF;
    public int overlayUV = OverlayTexture.NO_OVERLAY;

    @Nullable
    public Matrix4f transform;

    public BiConsumer<RenderContext, GeoModelState> returnFunc;

    /** 挂件 layer 问定位组只走这里，不要直接摸 {@link #modelState}——那会把 YSM 路径又写死回去 */
    public IGeoLocatorSource locators() {
        return locatorSource != null ? locatorSource : modelState;
    }

    public boolean isClosed() {
        return returnFunc == null;
    }

    @Override
    public void close() {
        if (returnFunc != null) {
            returnFunc.accept(ctx, modelState);
            modelState = null;
            returnFunc = null;
        }
        // 本对象会被复用，定位来源不清掉会让下一位使用者读到上一帧的模型
        locatorSource = null;
    }
}
