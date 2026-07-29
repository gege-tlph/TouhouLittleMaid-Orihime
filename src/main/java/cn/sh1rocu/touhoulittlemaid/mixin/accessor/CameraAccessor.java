package cn.sh1rocu.touhoulittlemaid.mixin.accessor;

import net.minecraft.client.Camera;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 1.21.11：{@link Camera} 的 {@code xRot}/{@code yRot} 是 private 字段，公开 getter 已移除
 * （26.1 用 MC26.1.2 的 CameraRenderState.yRot/xRot 字段，1.21.11 的 CameraRenderState 只有 orientation 四元数）。
 * chess/gomoku 的提示文字/箭头需按相机偏航（+俯仰）billboard（gomoku 箭头仅偏航，不能用整 orientation 四元数），
 * 故经此 accessor 取相机 yaw/pitch，忠实还原 HEAD 的 {@code camera.getYRot()/getXRot()} billboard 数学。
 */
@Environment(EnvType.CLIENT)
@Mixin(Camera.class)
public interface CameraAccessor {
    @Accessor("yRot")
    float tlm$getYRot();

    @Accessor("xRot")
    float tlm$getXRot();
}
