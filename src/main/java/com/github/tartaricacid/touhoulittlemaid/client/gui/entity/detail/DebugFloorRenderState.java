package com.github.tartaricacid.touhoulittlemaid.client.gui.entity.detail;

import com.github.tartaricacid.touhoulittlemaid.client.model.DebugFloorModel;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.state.pip.PictureInPictureRenderState;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import javax.annotation.Nullable;


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
