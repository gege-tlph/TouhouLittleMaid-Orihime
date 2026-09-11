package com.github.tartaricacid.touhoulittlemaid.geckolib3.geo;

import com.github.tartaricacid.touhoulittlemaid.client.resource.pojo.MaidModelInfo;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.animated.ILocationModel;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;

/**
 * 第三方模型系统（目前是 YSM）对单只女仆渲染态的最小契约——身体接管渲染器
 * （{@link IGeoEntityRenderer}）经它取得当前该渲染这只女仆的哪个模型、哪套骨骼定位。
 * <p>
 * 与 {@code port/1.21.11-fabric} 分支同名接口的差异：{@link #getMaid()} 返回具体的
 * {@link EntityMaid} 而非已被移除的 {@code api.entity.IMaid}——本分支所有宿主调用点
 * 都是 {@code EntityMaid}，无 IMaid 抽象层这条路（同一替换已见于 {@code TacCompat}）。
 */
public interface IGeoEntity {
    EntityMaid getMaid();

    MaidModelInfo getMaidInfo();

    ILocationModel getGeoModel();

    void setMaidInfo(MaidModelInfo info);

    void setYsmModel(String modelId, String texture);

    void updateRoamingVars(Object2FloatOpenHashMap<String> roamingVars);
}
