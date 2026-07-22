package com.github.tartaricacid.touhoulittlemaid.mixin.client;

import com.github.tartaricacid.touhoulittlemaid.client.renderer.entity.state.EntityChairRenderState;
import com.github.tartaricacid.touhoulittlemaid.client.renderer.entity.state.EntityMaidRenderState;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.RenderContextManager;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 原版提取物品栏实体预览时，为渲染器设置对应上下文。
 * 物品栏预览不经过世界实体的即时更新路径，因此需要在此处主动启动 Gecko 更新任务。
 */
@Mixin(InventoryScreen.class)
public class InventoryScreenMixin {
    @WrapOperation(
            method = "extractRenderState",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;createRenderState(Lnet/minecraft/world/entity/Entity;F)Lnet/minecraft/client/renderer/entity/state/EntityRenderState;"
            )
    )
    private static EntityRenderState tlm$extractInventoryRenderState(EntityRenderer<?, ?> instance,
                                                                      Entity entity, float partialTick,
                                                                      Operation<EntityRenderState> original) {
        RenderContextManager.setRenderingInInventory(true);
        try {
            EntityRenderState state = original.call(instance, entity, partialTick);
            if (state instanceof EntityMaidRenderState maidState && maidState.geckoUpdateTask != null) {
                maidState.geckoUpdateTask.start();
            } else if (state instanceof EntityChairRenderState chairState && chairState.geckoUpdateTask != null) {
                chairState.geckoUpdateTask.start();
            }
            return state;
        } finally {
            RenderContextManager.setRenderingInInventory(false);
        }
    }
}
