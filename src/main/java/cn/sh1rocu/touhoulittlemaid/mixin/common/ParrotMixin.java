package cn.sh1rocu.touhoulittlemaid.mixin.common;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.init.InitSounds;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.entity.animal.parrot.Parrot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;

@Mixin(Parrot.class)
public class ParrotMixin {
    // 1.21.11: Parrot 移至 ...world.entity.animal.parrot 子包（本文件唯一的真实变更）。
    //
    // method_6579 看似是脆弱的 intermediary 硬编码，实际稳定：它是
    //   MOB_SOUND_MAP = Util.make(Maps.newHashMap(), <lambda>)
    // 里那个 lambda 的合成方法。Mojang 官方映射不为合成 lambda 命名，loom 因此回退到
    // intermediary 名。已用 javap -v 的 BootstrapMethods 确认 1.21.11 中它仍是：
    //   REF_invokeStatic .../animal/parrot/Parrot.method_6579:(Ljava/util/HashMap;)V
    // 作用：把女仆加入鹦鹉的模仿音效表。
    @Inject(method = "method_6579", at = @At("HEAD"))
    private static void tlm$addSound(CallbackInfo ci, @Local(argsOnly = true) HashMap<Object, Object> map) {
        map.put(EntityMaid.TYPE, InitSounds.MAID_IDLE);
    }
}
