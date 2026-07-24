package cn.sh1rocu.touhoulittlemaid.mixin.client;

import cn.sh1rocu.touhoulittlemaid.api.event.KeyInputCallback;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 来自 Porting_Lib
 */
@Environment(EnvType.CLIENT)
@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerMixin {
    // 如果满足条件，则跳过第一个返回操作码。
    @Inject(
            method = "keyPress",
            slice = @Slice(
                    from = @At(
                            value = "RETURN",
                            ordinal = 0,
                            shift = Shift.AFTER
                    )
            ),
            at = @At(value = "RETURN")
    )
    public void port_lib$onHandleKeyInput(long window, int action, KeyEvent event, CallbackInfo ci) {
        KeyInputCallback.EVENT.invoker().onKeyInput(event.key(), event.scancode(), action, event.modifiers());
    }
}
