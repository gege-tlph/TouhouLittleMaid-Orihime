package com.github.tartaricacid.touhoulittlemaid.client.init;

import com.github.tartaricacid.touhoulittlemaid.client.model.DebugFloorModel;
// import com.github.tartaricacid.touhoulittlemaid.client.model.NewEntityFairyModel;
import com.github.tartaricacid.touhoulittlemaid.client.renderer.entity.*;
import com.github.tartaricacid.touhoulittlemaid.client.renderer.blockentity.GarageKitRenderer;
import com.github.tartaricacid.touhoulittlemaid.client.renderer.blockentity.StatueRenderer;
import com.github.tartaricacid.touhoulittlemaid.client.renderer.tileentity.*;
import com.github.tartaricacid.touhoulittlemaid.entity.item.*;
import com.github.tartaricacid.touhoulittlemaid.entity.monster.EntityFairy;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.entity.projectile.EntityDanmaku;
import com.github.tartaricacid.touhoulittlemaid.entity.projectile.EntityThrowPowerPoint;
import com.github.tartaricacid.touhoulittlemaid.entity.projectile.MaidFishingHook;
import com.github.tartaricacid.touhoulittlemaid.tileentity.*;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.world.entity.EntityType;

/**
 * 实体与方块实体渲染器的注册入口。
 */
@Environment(EnvType.CLIENT)
public final class InitEntitiesRender {
    public static void onEntityRenderers() {
        EntityRendererRegistry.register(EntityMaid.TYPE, EntityMaidRenderer::new);
        EntityRendererRegistry.register(EntityThrowPowerPoint.TYPE, ThrownItemRenderer::new);

        EntityRendererRegistry.register(EntityFairy.TYPE, EntityFairyRenderer::new);
        EntityRendererRegistry.register(EntityDanmaku.TYPE, EntityDanmakuRenderer::new);
        EntityRendererRegistry.register(EntityPowerPoint.TYPE, EntityPowerPointRenderer::new);
        EntityRendererRegistry.register(EntityExtinguishingAgent.TYPE, EntityExtinguishingAgentRenderer::new);
        EntityRendererRegistry.register(EntityBox.TYPE, EntityBoxRender::new);
        EntityRendererRegistry.register(EntityTombstone.TYPE, EntityTombstoneRenderer::new);
        EntityRendererRegistry.register(EntitySit.TYPE, EntitySitRenderer::new);
        EntityRendererRegistry.register(EntityBroom.TYPE, EntityBroomRender::new);
        // 未注册渲染器会令 EntityRenderDispatcher 在读取已有鱼钩的存档时解引用 null。
        EntityRendererRegistry.register(MaidFishingHook.TYPE, MaidFishingHookRenderer::new);

        EntityRendererRegistry.register(EntityChair.TYPE, EntityChairRenderer::new);
        // 注册进 vanilla EntityRenderers.PROVIDERS（map put），mod init 晚于原版静态注册 → 覆盖生效；
        // 岩浆怪已拆分为独立的 REPLACE_MAGMA_CUBE_MODEL 开关（上游与史莱姆共用一个开关）。
        EntityRendererRegistry.register(EntityType.SLIME, EntityYukkuriSlimeRender::new);
        EntityRendererRegistry.register(EntityType.MAGMA_CUBE, EntityMarisaYukkuriSlimeRender::new);
        EntityRendererRegistry.register(EntityType.EXPERIENCE_ORB, ReplaceExperienceOrbRenderer::new);

        BlockEntityRenderers.register(TileEntityAltar.TYPE, TileEntityAltarRenderer::new);
        // （本树 util 缺，26.1 有）+ entity-in-BE 渲染 + IEntityRenderStatePartialTick cast
        BlockEntityRenderers.register(TileEntityStatue.TYPE, StatueRenderer::new);
        BlockEntityRenderers.register(TileEntityGarageKit.TYPE, GarageKitRenderer::new);
        BlockEntityRenderers.register(TileEntityGomoku.TYPE, TileEntityGomokuRenderer::new);
        BlockEntityRenderers.register(TileEntityCChess.TYPE, TileEntityCChessRenderer::new);
        BlockEntityRenderers.register(TileEntityWChess.TYPE, TileEntityWChessRenderer::new);
        BlockEntityRenderers.register(TileEntityKeyboard.TYPE, TileEntityKeyboardRenderer::new);
        BlockEntityRenderers.register(TileEntityBookshelf.TYPE, TileEntityBookshelfRenderer::new);
        BlockEntityRenderers.register(TileEntityComputer.TYPE, TileEntityComputerRenderer::new);
        BlockEntityRenderers.register(TileEntityShrine.TYPE, TileEntityShrineRenderer::new);
        BlockEntityRenderers.register(TileEntityPicnicMat.TYPE, PicnicMatRender::new);
        BlockEntityRenderers.register(TileEntityMaidBed.TYPE, TileEntityMaidBedRenderer::new);
        BlockEntityRenderers.register(TileEntitySnackCabinet.TYPE, TileEntitySnackCabinetRenderer::new);
    }

    public static void onRegisterLayers() {
        // origin/1.21.1 InitEntitiesRender:60 registers this unconditionally, and its details
        // screen bakes the same layer. AbstractModelDetailsGui's constructor calls
        // bakeLayer(DebugFloorModel.LAYER), which throws IllegalArgumentException when the layer
        // is absent, so Shift + clicking a model in the selection GUI crashed the client. The
        // stated reason for deferring ("DebugFloorModel 仍延后") was already false: the class is
        // present and its exclusion was lifted long ago.
        EntityModelLayerRegistry.registerModelLayer(DebugFloorModel.LAYER, DebugFloorModel::createBodyLayer);
        // 被 bedrock 模型 new_maid_fairy 取代）→ 不再注册
    }
}
