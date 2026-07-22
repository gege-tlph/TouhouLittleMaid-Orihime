package com.github.tartaricacid.touhoulittlemaid.client.event;

import com.github.tartaricacid.touhoulittlemaid.client.renderer.item.ChairItemRenderer;
import com.github.tartaricacid.touhoulittlemaid.client.renderer.item.EntityPlaceholderItemRenderer;
import com.github.tartaricacid.touhoulittlemaid.client.renderer.item.GarageKitItemRenderer;
import com.github.tartaricacid.touhoulittlemaid.client.renderer.item.PicnicBasketItemRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderers;

public class RegisterSpecialModelEvent {
    public static void registerSpecialModelRenderers() {
        var mapper = SpecialModelRenderers.ID_MAPPER;
        mapper.put(ChairItemRenderer.ID, ChairItemRenderer.Unbaked.MAP_CODEC);
        mapper.put(GarageKitItemRenderer.ID, GarageKitItemRenderer.Unbaked.MAP_CODEC);
        mapper.put(EntityPlaceholderItemRenderer.ID, EntityPlaceholderItemRenderer.Unbaked.MAP_CODEC);
        mapper.put(PicnicBasketItemRenderer.ID, PicnicBasketItemRenderer.Unbaked.MAP_CODEC);
    }
}
