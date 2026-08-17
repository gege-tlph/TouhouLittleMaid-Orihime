package com.github.tartaricacid.touhoulittlemaid.client.event;

import com.github.tartaricacid.touhoulittlemaid.config.subconfig.MaidConfig;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.SchedulePos;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.OptionalDouble;
import java.util.concurrent.TimeUnit;
import com.github.tartaricacid.touhoulittlemaid.config.ServerRuleConfig;

public class MaidAreaRenderEvent {
    private static final Cache<Integer, SchedulePos> CACHE = CacheBuilder.newBuilder().expireAfterWrite(30, TimeUnit.SECONDS).build();

    //AfterOpaqueFeatures
    public static void onRender(LevelRenderContext context) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        for (int id : CACHE.asMap().keySet()) {
            SchedulePos pos = CACHE.getIfPresent(id);
            if (pos == null) {
                continue;
            }
            Entity entity = mc.level.getEntity(id);
            if (!(entity instanceof EntityMaid maid)) {
                return;
            }
            Identifier dimension = pos.getDimension();
            if (mc.player.level.dimension().identifier().equals(dimension)) {
                renderPos(pos.getWorkPos(), pos.getIdlePos(), pos.getSleepPos(), maid, mc.player);
            }
        }
    }

    private static void renderPos(@Nullable BlockPos workPos, @Nullable BlockPos idlePos, @Nullable BlockPos resetPos, EntityMaid maid, Player player) {
        BlockPos restrictCenter = maid.getHomePosition();
        Vec3 restrictPos = Vec3.atCenterOf(restrictCenter).add(0, 1, 0);
        if (!maid.isHomeModeEnable()) {
            restrictPos = player.position().add(0, 1, 0);
        }
        // home（或未开 home 时的玩家）到女仆的红线：基准一直在算 restrictPos，却没人画它
        Gizmos.line(restrictPos, maid.position().add(0, 1, 0), 0xffff3333);
        // colorFromFloat 的参数序是 (alpha, r, g, b)——基准盒色是 r0.8 g0.8 b0.2 a0.75 的半透明黄，
        // 按位置抄成 (0.8,0.2,0.75,0.8) 会画成半透明青蓝。盒子也不该 move(0,-1,0)，那会低一格。
        Gizmos.cuboid(maid.getBoundingBox(), GizmoStyle.fill(ARGB.colorFromFloat(0.75F, 0.8F, 0.8F, 0.2F)));

        if (workPos != null) {
            double radius = ServerRuleConfig.get(MaidConfig.MAID_WORK_RANGE) + 0.1;
            renderArea(workPos, radius, 0xffff0000);

            Vec3 textPos = new Vec3(workPos.getX() + 0.5, workPos.getY() + 2, workPos.getZ() + 0.5);
            String text = I18n.get("message.touhou_little_maid.kappa_compass.work_area");
            renderLabel(text, textPos, 0xffff1111);
        }

        if (idlePos != null) {
            double radius = ServerRuleConfig.get(MaidConfig.MAID_IDLE_RANGE);
            renderArea(idlePos, radius, 0xff00ff00);
            Vec3 textPos = new Vec3(idlePos.getX() + 0.5, idlePos.getY() + 2, idlePos.getZ() + 0.5);
            if (idlePos.equals(workPos)) {
                textPos = textPos.add(0, 1, 0);
            } else if (workPos != null) {
                Gizmos.line(centerPos(idlePos), centerPos(workPos), 0xffffffff);
            }
            String text = I18n.get("message.touhou_little_maid.kappa_compass.idle_area");
            renderLabel(text, textPos, 0xff11ff11);
        }

        if (resetPos != null) {
            double radius = ServerRuleConfig.get(MaidConfig.MAID_SLEEP_RANGE) - 0.1;
            renderArea(resetPos, radius, 0xff0000ff);
            Vec3 textPos = new Vec3(resetPos.getX() + 0.5, resetPos.getY() + 2, resetPos.getZ() + 0.5);
            if (resetPos.equals(idlePos)) {
                textPos = textPos.add(0, 2, 0);
            } else if (idlePos != null && workPos != null) {
                Gizmos.line(centerPos(resetPos), centerPos(idlePos), 0xffffffff);
                Gizmos.line(centerPos(resetPos), centerPos(workPos), 0xffffffff);
            }
            String text = I18n.get("message.touhou_little_maid.kappa_compass.sleep_area");
            renderLabel(text, textPos, 0xff1111ff);
        }
    }

    private static void renderArea(BlockPos pos, double radius, int color) {
        Gizmos.circle(centerPos(pos), (float) radius, GizmoStyle.stroke(color));
    }

    private static Vec3 centerPos(BlockPos pos) {
        return Vec3.atCenterOf(pos).add(0, 1, 0);
    }

    /**
     * 基准布局：文字锚点在 textPos 上方 1.07 格，标签再高 0.75、▼ 再低 0.75——
     * 标签在上、▼ 在下指向方块。
     */
    private static void renderLabel(String text, Vec3 textPos, int color) {
        renderText(text, textPos.add(0, 1.07 + 0.75, 0), color);
        renderText("▼", textPos.add(0, 1.07 - 0.75, 0), color);
    }

    /** 不开 always-on-top、不用 SEE_THROUGH：基准与 origin/1.21.1 的这段文字都会被墙体遮挡。 */
    private static void renderText(String text, Vec3 pos, int color) {
        Gizmos.billboardText(text, pos, new TextGizmo.Style(color, 1.5f, OptionalDouble.empty()));
    }

    public static void addSchedulePos(int id, SchedulePos pos) {
        CACHE.put(id, pos);
    }
}
