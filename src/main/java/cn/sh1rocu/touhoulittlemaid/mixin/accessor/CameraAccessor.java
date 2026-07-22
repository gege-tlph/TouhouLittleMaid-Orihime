package cn.sh1rocu.touhoulittlemaid.mixin.accessor;

import net.minecraft.client.Camera;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;


@Environment(EnvType.CLIENT)
@Mixin(Camera.class)
/**
 * 暴露相机的水平与垂直旋转角，供棋盘等始终朝向相机的渲染器分别读取。
 */
public interface CameraAccessor {
    @Accessor("yRot")
    float tlm$getYRot();

    @Accessor("xRot")
    float tlm$getXRot();
}
