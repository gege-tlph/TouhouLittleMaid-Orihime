package com.github.tartaricacid.touhoulittlemaid.client.overlay;

import com.github.tartaricacid.touhoulittlemaid.init.InitItems;
import com.github.tartaricacid.touhoulittlemaid.item.ItemFoxScroll;
import com.github.tartaricacid.touhoulittlemaid.item.ItemServantBell;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3fc;

import java.util.Optional;

/**
 * 狐狸卷轴与仆从铃的追踪标记。
 * <p>
 * <b>这个标记必须画在 HUD 层，不能画在世界里。</b>三种世界内画法都实测失败过：
 * <ul>
 *   <li>{@code Gizmos.billboardText(...).setAlwaysOnTop()}：1.21.11 的「置顶」是在
 *       {@code LevelRenderer.addLateDebugPass} 里 {@code clearDepthTexture} 清掉主渲染目标的深度贴图。
 *       光影包的深度附件被一起抹掉，玩家一切到铃铛地面就整片发白——这正是玩家报的主症状。</li>
 *   <li>不带置顶的 {@code Gizmos}：标记恒被钳到渲染距离边缘，必然埋在地形里。</li>
 *   <li>在 {@code WorldRenderEvents.END_MAIN} 用 {@code SEE_THROUGH} 自己画：不开光影正常，
 *       开光影后被随后的 composite 通道吃掉。同场景下原版 {@code Gizmos} 罗盘叠加层一样看不见。</li>
 * </ul>
 * HUD 层不经过光影管线，因此这是唯一同时满足「不破坏画面」与「一定看得见」的位置。
 * 行为仍与 1.21.1 基准一致：朝女仆方向、显示实际距离、超出渲染距离则钳到边缘、5 格内不显示。
 */
public class TrackerMarkerOverlay {
    public static final TrackerMarkerOverlay INSTANCE = new TrackerMarkerOverlay();

    /**
     * 颜色<b>必须带 Alpha</b>：1.21.1 的 {@code Font} 会把 alpha 为 0 的颜色补成不透明，
     * 1.21.11 删掉了那个兜底，照抄基准的 {@code 0xff8800} 会渲染成全透明。
     */
    private static final int DISTANCE_TEXT_COLOR = 0xffff8800;
    private static final int ARROW_COLOR = 0xffff0000;

    /** 与基准一致：离得太近就不再提示 */
    private static final double MIN_TRACK_DISTANCE = 5.0;

    /** 视线正后方的点投影出来会翻到屏幕另一侧，必须先按深度剔除 */
    private static final double MIN_FORWARD_DEPTH = 0.1;

    public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.level == null || mc.options.hideGui) {
            return;
        }
        Optional<ItemFoxScroll.TrackInfo> trackInfo = getInfo(player, player.getMainHandItem());
        if (trackInfo.isEmpty()) {
            return;
        }
        ItemFoxScroll.TrackInfo info = trackInfo.get();
        if (!info.dimension().equals(player.level.dimension().identifier().toString())) {
            return;
        }
        Camera camera = mc.gameRenderer.getMainCamera();
        if (!camera.isInitialized()) {
            return;
        }

        Vec3 trackVec = new Vec3(info.position().getX(), info.position().getY(), info.position().getZ());
        Vec3 playerVec = player.position();
        double actualDistance = playerVec.distanceTo(trackVec);
        if (actualDistance < MIN_TRACK_DISTANCE) {
            return;
        }
        // 超出渲染距离就把标记拉回渲染距离边缘，方向不变——距离数字仍报真实值
        double maxRenderDistance = mc.options.renderDistance().get() * 16.0;
        if (actualDistance > maxRenderDistance) {
            Vec3 delta = trackVec.subtract(playerVec).normalize();
            trackVec = playerVec.add(delta.x * maxRenderDistance, delta.y * maxRenderDistance, delta.z * maxRenderDistance);
        }

        Vec3 relative = trackVec.subtract(camera.position());
        Vector3fc forward = camera.forwardVector();
        Vector3fc up = camera.upVector();
        Vector3fc left = camera.leftVector();
        double depth = relative.x * forward.x() + relative.y * forward.y() + relative.z * forward.z();
        if (depth <= MIN_FORWARD_DEPTH) {
            return;
        }
        double rightOffset = -(relative.x * left.x() + relative.y * left.y() + relative.z * left.z());
        double upOffset = relative.x * up.x() + relative.y * up.y() + relative.z * up.z();

        // 用真实 FOV 而不是设置里的基础值：疾跑、鬼影效果与望远镜都会改它，
        // 用基础值会让标记在这些状态下偏离女仆的真实方向
        float fov = mc.gameRenderer.getFov(camera, deltaTracker.getGameTimeDeltaPartialTick(false), true);
        double tanHalfFov = Math.tan(Math.toRadians(fov) / 2.0);
        double aspect = (double) mc.getWindow().getWidth() / (double) mc.getWindow().getHeight();

        double ndcX = (rightOffset / depth) / (tanHalfFov * aspect);
        double ndcY = (upOffset / depth) / tanHalfFov;
        if (Math.abs(ndcX) > 1.0 || Math.abs(ndcY) > 1.0) {
            return;
        }

        Font font = mc.font;
        int x = (int) Math.round(guiGraphics.guiWidth() / 2.0 * (1.0 + ndcX));
        int y = (int) Math.round(guiGraphics.guiHeight() / 2.0 * (1.0 - ndcY));
        guiGraphics.drawCenteredString(font, Math.round(actualDistance) + " m", x, y - font.lineHeight, DISTANCE_TEXT_COLOR);
        guiGraphics.drawCenteredString(font, "▼", x, y, ARROW_COLOR);
    }

    private static Optional<ItemFoxScroll.TrackInfo> getInfo(Player player, ItemStack stack) {
        if (stack.getItem() instanceof ItemFoxScroll) {
            return Optional.ofNullable(ItemFoxScroll.getTrackInfo(stack));
        }
        if (stack.is(InitItems.SERVANT_BELL)) {
            return Optional.ofNullable(ItemServantBell.getMaidShow(stack));
        }
        return Optional.empty();
    }
}
