package com.github.tartaricacid.touhoulittlemaid.compat.gun.tacz.client;

import com.github.tartaricacid.touhoulittlemaid.compat.gun.tacz.TacCompat;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.IGeoLocatorSource;
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
 * 背部枪械渲染。<b>只在模型作者提供了挂点骨骼时才画</b>——手枪找
 * {@link GeoLocatorType#TAC_PISTOL}、长枪找 {@link GeoLocatorType#TAC_RIFLE}，
 * 没有对应骨骼就什么都不画、不回落。
 *
 * <p>⚠️ 上游（{@code origin/1.21.1}）与行为基准另有一条<b>固定变换的兜底</b>：
 * 不看任何骨骼，直接 {@code ZP180 / XP180 / translate / 背包位移 / ZP-35 / scale(0.6)}
 * 把枪的物品模型拍在背上。它是 bedrock 模型以及「gecko 模型 + 穿着背包」两条路的落点，
 * <b>实机结果是枪甩到女仆身侧、长度接近整个身体、穿进模型里</b>
 * （用户 2026-08-20 实机报出，2026-08-21 截图取证）。三棵树的那条兜底逐字相同，
 * 所以它不是移植回归，是上游一直如此。用户 2026-08-21 裁决<b>砍掉兜底、只保留骨骼那条</b>，
 * 与 2026-08-20 对「没有 TAC_PISTOL/TAC_RIFLE 骨骼就不画」的裁决同一个原则：
 * 挂点归模型作者定，没给挂点就是不想让枪挂在那儿。</p>
 *
 * <p>定位组载体走 {@link IGeoLocatorSource} 接口（与 1.21.11 分支同名同签名：
 * visitLocatorGroup / locatorGroupSize），不是具体的 {@code GeoModelState}——
 * 这样 TLM 自己的 gecko 层（{@code data.modelState}）和 YSM 身体接管的外部定位骨骼源
 * （{@code compat/ysm} 下的桥接）都能喂给这里，本方法不用关心是哪一种。物品提交走标准通道：
 * TaCZ 侧自己实现了原版 ItemModel（TaczDynamicItemModel），{@code ItemModelResolver.updateForLiving}
 * + {@code ItemStackRenderState.submit} 即可画出枪模型，不必碰它的渲染器。</p>
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

    public static void renderBackGun(ItemStack heldItem, IGeoLocatorSource modelState, EntityMaid maid, PoseStack poseStack,
                                     SubmitNodeCollector submitNode, int packedLight) {
        IGun gun = IGun.getIGunOrNull(heldItem);
        if (gun == null) {
            return;
        }
        TimelessAPI.getCommonGunIndex(gun.getGunId(heldItem)).ifPresent(index -> {
            String weaponType = index.getType();
            // 有对应定位骨骼才画，没有就什么都不画、不回落
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
