package com.github.tartaricacid.touhoulittlemaid.client.init;

import com.github.tartaricacid.touhoulittlemaid.client.renderer.item.ReplaceableBakedModel;
import com.github.tartaricacid.touhoulittlemaid.config.subconfig.VanillaConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier;
import net.fabricmc.fabric.api.client.model.loading.v1.ExtraModelKey;
import net.fabricmc.fabric.api.client.model.loading.v1.SimpleUnbakedExtraModel;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.item.BlockModelWrapper;
import net.minecraft.resources.Identifier;

import java.util.List;

@Environment(EnvType.CLIENT)
public final class InitSpecialItemRender implements ModelLoadingPlugin {
    private static final Identifier TOTEM = Identifier.withDefaultNamespace("totem_of_undying");
    private static final Identifier EXPERIENCE_BOTTLE = Identifier.withDefaultNamespace("experience_bottle");
    private static final Identifier LIFE_POINT = Identifier.fromNamespaceAndPath("touhou_little_maid", "item/life_point");
    private static final Identifier POINT_ITEM = Identifier.fromNamespaceAndPath("touhou_little_maid", "item/point_item");
    private static final ExtraModelKey<BlockStateModel> LIFE_POINT_KEY = ExtraModelKey.create(LIFE_POINT::toString);
    private static final ExtraModelKey<BlockStateModel> POINT_ITEM_KEY = ExtraModelKey.create(POINT_ITEM::toString);

    @Override
    public void initialize(Context context) {
        // Before-bake modifiers run after vanilla's normal dependency walk.
        // Extra models make the two replacement geometries discoverable early.
        context.addModel(LIFE_POINT_KEY, SimpleUnbakedExtraModel.blockStateModel(LIFE_POINT));
        context.addModel(POINT_ITEM_KEY, SimpleUnbakedExtraModel.blockStateModel(POINT_ITEM));
        context.modifyItemModelBeforeBake().register(ModelModifier.WRAP_PHASE, (model, bakeContext) -> {
            Identifier itemId = bakeContext.itemId();
            if (TOTEM.equals(itemId)) {
                return new ReplaceableBakedModel.Unbaked(model,
                        new BlockModelWrapper.Unbaked(LIFE_POINT, List.of()),
                        VanillaConfig.REPLACE_TOTEM_TEXTURE::get);
            }
            if (EXPERIENCE_BOTTLE.equals(itemId)) {
                return new ReplaceableBakedModel.Unbaked(model,
                        new BlockModelWrapper.Unbaked(POINT_ITEM, List.of()),
                        VanillaConfig.REPLACE_XP_BOTTLE_TEXTURE::get);
            }
            return model;
        });
    }
}
