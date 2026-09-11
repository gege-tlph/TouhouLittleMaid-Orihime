package com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.animated;

import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.processor.ILocationBone;

import java.util.Collections;
import java.util.List;

/**
 * 第三方模型系统（目前是 YSM）暴露给 TLM 的定位骨骼组集合。每一组是一条从模型根
 * 到定位点的骨骼链（{@code ILocationBone} 列表，逐级复合），供 {@link LocationModelLocatorSource}
 * 转成 {@link IGeoLocatorSource}，从而让 TLM 的挂件 layer（手持物/头饰/背包/背部物品/旗帜）
 * 在这套模型系统接管女仆本体渲染时仍然可用。
 * <p>
 * 全部方法给默认空实现：模型没有声明某个定位组时，直接不用覆写。
 */
public interface ILocationModel {
    default List<? extends ILocationBone> leftHandBones() {
        return Collections.emptyList();
    }

    default List<List<? extends ILocationBone>> extraLeftHandBones() {
        return Collections.emptyList();
    }

    default List<? extends ILocationBone> rightHandBones() {
        return Collections.emptyList();
    }

    default List<List<? extends ILocationBone>> extraRightHandBones() {
        return Collections.emptyList();
    }

    default List<? extends ILocationBone> leftWaistBones() {
        return Collections.emptyList();
    }

    default List<? extends ILocationBone> rightWaistBones() {
        return Collections.emptyList();
    }

    default List<? extends ILocationBone> backpackBones() {
        return Collections.emptyList();
    }

    default List<? extends ILocationBone> tacPistolBones() {
        return Collections.emptyList();
    }

    default List<? extends ILocationBone> tacRifleBones() {
        return Collections.emptyList();
    }

    default List<? extends ILocationBone> headBones() {
        return Collections.emptyList();
    }
}
