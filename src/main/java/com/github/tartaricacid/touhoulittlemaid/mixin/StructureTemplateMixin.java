package com.github.tartaricacid.touhoulittlemaid.mixin;

import com.github.tartaricacid.touhoulittlemaid.config.subconfig.MaidConfig;
import com.github.tartaricacid.touhoulittlemaid.config.ServerRuleConfig;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.init.InitEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import java.util.Random;

@Mixin(StructureTemplate.class)
public abstract class StructureTemplateMixin {
    @Unique
    private static final Random TOUHOU_LITTLE_MAID$RANDOM = new Random();

    // 1.21.11: createEntityIgnoreException 新增首参 ProblemReporter → 描述符与 handler 同步（reporter 不使用，仅对齐签名）
    @Inject(method = "createEntityIgnoreException(Lnet/minecraft/util/ProblemReporter;Lnet/minecraft/world/level/ServerLevelAccessor;Lnet/minecraft/nbt/CompoundTag;)Ljava/util/Optional;", at = @At("RETURN"), cancellable = true)
    private static void createEntityIgnoreException(ProblemReporter reporter, ServerLevelAccessor accessor, CompoundTag tag, CallbackInfoReturnable<Optional<Entity>> ci) {
        ci.getReturnValue().ifPresent(entity -> {
            ServerLevel level = accessor.getLevel();
            if (entity.getType().equals(EntityType.ALLAY) && TOUHOU_LITTLE_MAID$RANDOM.nextDouble() < ServerRuleConfig.get(MaidConfig.REPLACE_ALLAY_PERCENT)) {
                // 1.21.11: EntityType.create(Level) 移除 -> create(Level, EntitySpawnReason)（结构生成 → STRUCTURE，EntityMaid:1728 有对应处理）
                EntityMaid entityMaid = InitEntities.MAID.create(level, EntitySpawnReason.STRUCTURE);
                ci.setReturnValue(Optional.ofNullable(entityMaid));
            }
        });
    }
}
