package com.github.tartaricacid.touhoulittlemaid.client.renderer.entity.gecko;

import com.github.tartaricacid.touhoulittlemaid.client.renderer.entity.state.EntityMaidRenderState;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.GeoLayerRenderer;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.animated.ILocationModel;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.animated.LocationModelLocatorSource;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.SubmitNodeCollector;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 第三方模型系统（YSM）接管女仆渲染时，用这一个静态入口提交 TLM 的挂件 layer。
 * <p>
 * 移植自 {@code port/1.21.11-fabric} 分支同名类，逻辑不变，只把字段名从那条分支的
 * {@code data.locatorSource} 换成本分支既有的 {@code data.modelState}（类型已在
 * {@code IGeoLocatorSource} 那轮改动里放宽为接口，见 GeckoRenderData）。
 * <p>
 * <b>为什么入口在 TLM 而不是让接管方自己拼</b>：怎么构造 {@link GeckoMaidRenderData}、
 * 定位来源该塞什么、camera 传什么才安全，都是 TLM 的内部知识。放在这里，接管方只需要在
 * <b>模型空间</b>里调一次，日后 TLM 增删 layer、换数据载体都不必惊动对面。
 * <p>
 * <b>调用时机是硬要求</b>：必须在接管方自己的 {@code pushPose()}/{@code popPose()} 之内、
 * 模型的根变换（朝向、缩放、睡姿位移等）都施加完之后调用。挂件靠骨骼链定位，
 * 而骨骼链是相对模型根的；在实体根坐标系里调，挂件会整体错位。
 * <p>
 * camera 取自 {@link EntityMaidRenderState#camera}，由 {@code EntityMaidRenderer.submit} 在
 * 移交给接管方<b>之前</b>填好，接管方自己也在用它。所以这条路上 camera 是真的，
 * 不需要传 null——目前 5 个 layer 都没读它，但将来谁要用，直接用即可，不需要改接口。
 */
@Environment(EnvType.CLIENT)
public final class YsmMaidLayerBridge {
    private YsmMaidLayerBridge() {
    }

    /**
     * @param layers        接管方经 {@code addGeoLayerRenderer} 收到的那批 layer，原样传回
     * @param locationModel 接管方模型的定位组；null（模型还没就绪）时本方法什么都不做
     */
    @SuppressWarnings("unchecked")
    public static void submitMaidLayers(List<GeoLayerRenderer<?, ?>> layers,
                                        SubmitNodeCollector submitNodeCollector,
                                        PoseStack poseStack,
                                        EntityMaidRenderState state,
                                        @Nullable ILocationModel locationModel) {
        if (locationModel == null || layers.isEmpty()) {
            return;
        }
        try (GeckoMaidRenderData data = new GeckoMaidRenderData()) {
            data.modelState = new LocationModelLocatorSource(locationModel);
            for (GeoLayerRenderer<?, ?> layer : layers) {
                ((GeoLayerRenderer<EntityMaidRenderState, GeckoMaidRenderData>) layer)
                        .submit(submitNodeCollector, poseStack, state, data, state.camera);
            }
        }
    }
}
