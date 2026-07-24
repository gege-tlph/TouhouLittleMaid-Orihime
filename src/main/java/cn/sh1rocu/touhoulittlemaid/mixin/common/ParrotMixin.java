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

    @Inject(method = "method_6579", at = @At("HEAD"))
    private static void tlm$addSound(CallbackInfo ci, @Local(argsOnly = true) HashMap<Object, Object> map) {
        map.put(EntityMaid.TYPE, InitSounds.MAID_IDLE);
    }
}
