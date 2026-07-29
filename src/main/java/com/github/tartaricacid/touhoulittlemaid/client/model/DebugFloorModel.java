package com.github.tartaricacid.touhoulittlemaid.client.model;


import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.resources.Identifier;

/**
 * origin/1.21.1 的 renderToBuffer 覆写只渲染 floor 子骨骼；1.21.11 中 renderToBuffer 已 final、
 * 改为渲染 root（root 仅含 floor 一个子骨骼，几何等价），故 floor 字段与两处覆写移除。
 */
public class DebugFloorModel extends AbstractModel<EntityRenderState> {
    public static ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(TouhouLittleMaid.MOD_ID, "main"), "debug_floor");

    public DebugFloorModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition floor = partdefinition.addOrReplaceChild("floor", CubeListBuilder.create().texOffs(0, 0).addBox(-8.0F, 0.0F, -11.0F, 16.0F, 0.0F, 19.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(0.0F, -8.0F, 0.0F, -3.1416F, 0.0F, 3.1416F));

        return LayerDefinition.create(meshdefinition, 64, 32);
    }
}
