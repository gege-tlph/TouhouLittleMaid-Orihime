package cn.sh1rocu.touhoulittlemaid.util.neoforge;

import net.minecraft.world.entity.LivingEntity;

/**
 * NeoForge ClientHooks 在 Fabric 端的等价门面。
 */
public class ClientHooks {

    /**
     * 使用原版 64 格名牌可见距离判断。参数为已经计算好的距离平方。
     */
    public static boolean isNameplateInRenderDistance(LivingEntity entity, double squareDistance) {
        double value = 64.0D;
        return !(squareDistance > value * value);
    }
}
