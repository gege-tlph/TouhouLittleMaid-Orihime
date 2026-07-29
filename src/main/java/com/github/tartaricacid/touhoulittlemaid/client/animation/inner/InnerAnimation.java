package com.github.tartaricacid.touhoulittlemaid.client.animation.inner;

import com.github.tartaricacid.touhoulittlemaid.api.animation.IAnimation;
import com.google.common.collect.Maps;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.resources.Identifier;

import java.util.HashMap;

public final class InnerAnimation {
    static final HashMap<Identifier, IAnimation<?>> INNER_ANIMATION = Maps.newHashMap();

    public static boolean containsKey(Identifier resourceLocation) {
        return INNER_ANIMATION.containsKey(resourceLocation);
    }

    @SuppressWarnings("unchecked")
    public static <T extends EntityRenderState> IAnimation<T> get(Identifier resourceLocation) {
        return (IAnimation<T>) INNER_ANIMATION.get(resourceLocation);
    }

    public static void init() {
        INNER_ANIMATION.clear();
        // 已被 2a 富化消除——9 个类全部编译（合计 81 个注册）。此前只接了空壳 init() → 注册表为空 →
        // 女仆零动画（平移/不转头/呆滞）。现按 origin 全量注册。
        MaidBaseAnimation.init();
        MaidExtraAnimation.init();
        MaidArmorAnimation.init();
        MaidTaskAnimation.init();
        ChairBaseAnimation.init();
        EntityBaseAnimation.init();
        PlayerMaidAnimation.init();
        SpecialAnimation.init();
        FestivalAnimation.init();
    }
}
