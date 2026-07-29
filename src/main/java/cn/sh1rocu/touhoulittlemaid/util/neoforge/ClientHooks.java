package cn.sh1rocu.touhoulittlemaid.util.neoforge;

import net.minecraft.world.entity.LivingEntity;

/**
 * NeoForge ClientHooks 的 Fabric 等价 facade。
 */
public class ClientHooks {
    /**
     * SWEEP R12-3：原 stub 恒 true（任意距离渲染气泡/名牌）——还原 NeoForge
     * {@code ClientHooks.isNameplateInRenderDistance} 语义：
     * {@code !(squareDistance > d * d)}，d = NAMETAG_DISTANCE 属性（NeoForge 默认 64.0；
     * Fabric 无此属性 → 取默认值定值，行为 == origin 无 mod 修改属性时）。
     */
    public static boolean isNameplateInRenderDistance(LivingEntity entity, double squareDistance) {
        double value = 64.0D;
        return !(squareDistance > value * value);
    }
}
