package com.github.tartaricacid.touhoulittlemaid.client.gui.entity.detail;

import com.github.tartaricacid.touhoulittlemaid.client.model.DebugFloorModel;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.state.pip.PictureInPictureRenderState;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import javax.annotation.Nullable;

/**
 * origin/1.21.1 在模型详情屏中与主实体同一 poseStack 直渲 DebugFloorModel；
 * 1.21.11 GUI 只能经 PiP 提交任意模型，故为地板建独立 PiP 状态（变换与主实体的
 * GuiEntityRenderState 完全一致，渲染器内再补 origin 的 translate(0, 0.5, 0)）。
 */
@Environment(EnvType.CLIENT)
public record DebugFloorRenderState(
        DebugFloorModel floorModel,
        Vector3f translation,
        Quaternionf rotation,
        int x0,
        int y0,
        int x1,
        int y1,
        float scale,
        @Nullable ScreenRectangle scissorArea,
        @Nullable ScreenRectangle bounds
) implements PictureInPictureRenderState {
    public DebugFloorRenderState(DebugFloorModel floorModel, Vector3f translation, Quaternionf rotation,
                                 int x0, int y0, int x1, int y1, float scale, @Nullable ScreenRectangle scissorArea) {
        this(floorModel, translation, rotation, x0, y0, x1, y1, scale, scissorArea,
                PictureInPictureRenderState.getBounds(x0, y0, x1, y1, scissorArea));
    }
}
