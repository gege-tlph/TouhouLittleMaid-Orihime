package com.github.tartaricacid.touhoulittlemaid.compat.gun.tacz.client;

import com.github.tartaricacid.touhoulittlemaid.compat.gun.tacz.TacCompat;
import com.github.tartaricacid.touhoulittlemaid.entity.backpack.BackpackManager;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.animated.GeoModelState;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.render.built.GeoLocatorType;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.GunTabType;
import com.tacz.guns.api.item.IGun;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;

import static com.github.tartaricacid.touhoulittlemaid.entity.backpack.BackpackManager.RENDER_DATA_CACHE;

/**
 * 背部枪械渲染（行为基准 = origin/1.21.1，写法照宿主）。与 1.21.11 分支的差异：
 * <ul>
 *   <li>定位组载体：那边抽象成 {@code IGeoLocatorSource}，宿主直接用 {@link GeoModelState}
 *       （gecko 层的 {@code data.modelState}），API 同名（visitLocatorGroup / locatorGroupSize）。</li>
 *   <li>背包位移：宿主把渲染数据从 {@code IMaidBackpack} 分离成
 *       {@code MaidBackpackRenderData}（{@code RENDER_DATA_CACHE.apply(id)}），此处随宿主。</li>
 *   <li>物品提交走标准通道：TaCZ 侧自己实现了原版 ItemModel（TaczDynamicItemModel），
 *       {@code ItemModelResolver.updateForLiving} + {@code ItemStackRenderState.submit}
 *       即可画出枪模型，不必碰它的渲染器（与宿主 AltarRenderer 同款五参 submit）。</li>
 * </ul>
 */
@Environment(EnvType.CLIENT)
public class GunMaidRender {
    public static void addItemTranslate(PoseStack matrixStack, ItemStack itemStack, boolean isLeft) {
        if (!isLeft && itemStack.getItem() instanceof IGun gun) {
            matrixStack.translate(0, -0.125, 0);
            if (TacCompat.MINIGUN_ID.equals(gun.getGunId(itemStack))) {
                matrixStack.mulPose(Axis.ZP.rotationDegrees(20));
                matrixStack.mulPose(Axis.XP.rotationDegrees(50));
            }
        }
    }

    public static void renderBackGun(PoseStack poseStack, SubmitNodeCollector submitNode, int packedLight, ItemStack stack, EntityMaid maid) {
        if (!(stack.getItem() instanceof IGun)) {
            return;
        }
        poseStack.pushPose();
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
        poseStack.translate(0, 0.5, -0.25);
        // 基准：显示背包时按女仆背包型位移，否则按空背包位移——两条路都会继续画
        Identifier backpackId = maid.getConfigManager().isShowBackpack()
                ? maid.getMaidBackpackType().getId()
                : BackpackManager.getEmptyBackpack().getId();
        RENDER_DATA_CACHE.apply(backpackId).offsetBackpackItem(poseStack);
        {
            poseStack.pushPose();
            poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(-35));
            poseStack.scale(0.6f, 0.6f, 0.6f);
            submitGun(poseStack, submitNode, packedLight, stack, maid);
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    public static void renderBackGun(ItemStack heldItem, GeoModelState modelState, EntityMaid maid, PoseStack poseStack,
                                     SubmitNodeCollector submitNode, int packedLight) {
        IGun gun = IGun.getIGunOrNull(heldItem);
        if (gun == null) {
            return;
        }
        // 如果女仆穿戴了背包，且配置文件允许显示背包
        // 直接调用背包渲染
        if (maid.getConfigManager().isShowBackpack()
                && maid.getMaidBackpackType() != BackpackManager.getEmptyBackpack()) {
            // 基准：有背包定位骨骼就先 prepMatrixForLocator，没有就直接用当前矩阵——两条路都会继续画
            if (modelState.locatorGroupSize(GeoLocatorType.BACKPACK) > 0) {
                modelState.visitLocatorGroup(GeoLocatorType.BACKPACK, poseStack,
                        locator -> renderBackpackGun(locator, submitNode, packedLight, heldItem, maid));
            } else {
                renderBackpackGun(poseStack, submitNode, packedLight, heldItem, maid);
            }
            return;
        }
        TimelessAPI.getCommonGunIndex(gun.getGunId(heldItem)).ifPresent(index -> {
            String weaponType = index.getType();
            // 基准这两支是「有对应定位骨骼才画」，没有就什么都不画，不回落
            if (isPistol(weaponType)) {
                modelState.visitLocatorGroup(GeoLocatorType.TAC_PISTOL, poseStack, locator -> {
                    locator.translate(0, -0.125, 0);
                    locator.scale(0.65f, 0.65f, 0.65f);
                    locator.mulPose(Axis.YP.rotationDegrees(-90.0F));
                    locator.mulPose(Axis.ZP.rotationDegrees(90.0F));
                    submitGun(locator, submitNode, packedLight, heldItem, maid);
                });
            } else {
                modelState.visitLocatorGroup(GeoLocatorType.TAC_RIFLE, poseStack, locator -> {
                    locator.scale(0.65f, 0.65f, 0.65f);
                    locator.mulPose(Axis.YP.rotationDegrees(-180.0F));
                    submitGun(locator, submitNode, packedLight, heldItem, maid);
                });
            }
        });
    }

    private static void renderBackpackGun(PoseStack poseStack, SubmitNodeCollector submitNode, int packedLight,
                                          ItemStack heldItem, EntityMaid maid) {
        poseStack.mulPose(Axis.ZP.rotationDegrees(180));
        poseStack.translate(0, -1, 0.25);
        renderBackGun(poseStack, submitNode, packedLight, heldItem, maid);
    }

    private static void submitGun(PoseStack poseStack, SubmitNodeCollector submitNode, int packedLight,
                                  ItemStack stack, Mob mob) {
        ItemStackRenderState renderState = new ItemStackRenderState();
        Minecraft.getInstance().getItemModelResolver().updateForLiving(renderState, stack, ItemDisplayContext.FIXED, mob);
        renderState.submit(poseStack, submitNode, packedLight, OverlayTexture.NO_OVERLAY, 0);
    }

    private static boolean isPistol(String type) {
        return type.equals(GunTabType.PISTOL.name().toLowerCase(Locale.ENGLISH));
    }
}
