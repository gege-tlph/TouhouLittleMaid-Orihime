package com.github.tartaricacid.touhoulittlemaid.compat.gun.tacz.client;

import com.github.tartaricacid.touhoulittlemaid.api.backpack.IMaidBackpack;
import com.github.tartaricacid.touhoulittlemaid.api.entity.IMaid;
import com.github.tartaricacid.touhoulittlemaid.compat.gun.tacz.TacCompat;
import com.github.tartaricacid.touhoulittlemaid.entity.backpack.BackpackManager;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.animated.IGeoLocatorSource;
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
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;

/**
 * 1.21.11 渲染管线移植说明（基准 = origin/1.21.1）：
 * <ul>
 *   <li>基准用 {@code ItemRenderer.renderStatic(...)} 直接画物品；1.21.11 已改为
 *       先由 {@code ItemModelResolver} 解析出 {@link ItemStackRenderState}，再 {@code submit} 到
 *       {@link SubmitNodeCollector}。TaCZ 侧本身提供了 {@code TaczDynamicItemModel}（实现原版 ItemModel）
 *       与配套的 SpecialRenderer，所以枪械走这条标准通道就能画出来，不需要我们碰它的渲染器。</li>
 *   <li>基准直接吃 {@code ILocationModel} 的骨骼链（{@code tacPistolBones()} 等）；本树的 gecko 路径
 *       已改为 {@link IGeoLocatorSource} + {@link GeoLocatorType} 定位组，YSM 路径由
 *       {@code LocationModelLocatorSource} 把同一批骨骼链包装成定位组。故此处统一按定位组消费，
 *       两种模型系统都能命中。</li>
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

    public static void renderBackGun(PoseStack poseStack, SubmitNodeCollector submitNode, int packedLight, ItemStack stack, IMaid maid) {
        if (!(stack.getItem() instanceof IGun)) {
            return;
        }
        poseStack.pushPose();
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
        poseStack.translate(0, 0.5, -0.25);
        if (maid instanceof EntityMaid entityMaid && entityMaid.getConfigManager().isShowBackpack()) {
            maid.getMaidBackpackType().offsetBackpackItem(poseStack);
        } else {
            BackpackManager.getEmptyBackpack().offsetBackpackItem(poseStack);
        }
        {
            poseStack.pushPose();
            poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(-35));
            poseStack.scale(0.6f, 0.6f, 0.6f);
            submitGun(poseStack, submitNode, packedLight, stack, maid.asEntity());
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    public static void renderBackGun(ItemStack heldItem, IGeoLocatorSource locators, IMaid maid, PoseStack poseStack,
                                     SubmitNodeCollector submitNode, int packedLight) {
        IGun gun = IGun.getIGunOrNull(heldItem);
        if (gun == null) {
            return;
        }
        Mob entity = maid.asEntity();
        IMaidBackpack maidBackpackType = maid.getMaidBackpackType();
        // 如果女仆穿戴了背包，且配置文件允许显示背包
        // 直接调用背包渲染
        if (entity instanceof EntityMaid entityMaid && entityMaid.getConfigManager().isShowBackpack()
                && maidBackpackType != BackpackManager.getEmptyBackpack()) {
            // 基准：有背包定位骨骼就先 prepMatrixForLocator，没有就直接用当前矩阵——两条路都会继续画
            if (locators.locatorGroupSize(GeoLocatorType.BACKPACK) > 0) {
                locators.visitLocatorGroup(GeoLocatorType.BACKPACK, poseStack,
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
                locators.visitLocatorGroup(GeoLocatorType.TAC_PISTOL, poseStack, locator -> {
                    locator.translate(0, -0.125, 0);
                    locator.scale(0.65f, 0.65f, 0.65f);
                    locator.mulPose(Axis.YP.rotationDegrees(-90.0F));
                    locator.mulPose(Axis.ZP.rotationDegrees(90.0F));
                    submitGun(locator, submitNode, packedLight, heldItem, entity);
                });
            } else {
                locators.visitLocatorGroup(GeoLocatorType.TAC_RIFLE, poseStack, locator -> {
                    locator.scale(0.65f, 0.65f, 0.65f);
                    locator.mulPose(Axis.YP.rotationDegrees(-180.0F));
                    submitGun(locator, submitNode, packedLight, heldItem, entity);
                });
            }
        });
    }

    private static void renderBackpackGun(PoseStack poseStack, SubmitNodeCollector submitNode, int packedLight,
                                          ItemStack heldItem, IMaid maid) {
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
