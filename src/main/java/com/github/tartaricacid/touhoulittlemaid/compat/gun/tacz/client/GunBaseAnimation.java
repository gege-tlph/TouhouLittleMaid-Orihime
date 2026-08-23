package com.github.tartaricacid.touhoulittlemaid.compat.gun.tacz.client;

import com.github.tartaricacid.simplebedrockmodel.client.bedrock.model.BedrockPart;
import com.tacz.guns.api.item.IGun;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

import static com.github.tartaricacid.touhoulittlemaid.compat.gun.tacz.TacCompat.MINIGUN_ID;

/**
 * 基准（origin/1.21.1）这里吃的是 {@code IMaid} + {@code ModelRendererWrapper}，
 * 但本树的 {@code MaidBaseAnimation} 是渲染状态驱动、手上拿的是 {@link BedrockPart}
 * 且拿不到实体本身。基准也只用实体取了一次主手物品，故这里直接收主手 {@link ItemStack}；
 * 旋转角逐字照抄基准，{@code setRotateAngleX/Y} 对应 {@code xRot/yRot}（与 1.21.11 分支同款适配）。
 */
@Environment(EnvType.CLIENT)
public class GunBaseAnimation {
    public static boolean onHoldGun(ItemStack handItem, @Nullable BedrockPart armLeft, @Nullable BedrockPart armRight) {
        IGun gun = IGun.getIGunOrNull(handItem);
        if (gun == null) {
            return false;
        }
        Identifier gunId = gun.getGunId(handItem);

        // 因为现在还没有 minigun 的专属标签，故只能用特判
        if (gunId.equals(MINIGUN_ID)) {
            if (armLeft != null) {
                armLeft.xRot = -1.45f;
                armLeft.yRot = 1f;
            }
            if (armRight != null) {
                armRight.xRot = 0.75f;
                armRight.yRot = 0;
            }
            return true;
        }

        if (armLeft != null) {
            armLeft.xRot = -1.75f;
            armLeft.yRot = 0.5f;
        }
        if (armRight != null) {
            armRight.xRot = -1.65f;
            armRight.yRot = -0.174f;
        }
        return true;
    }
}
