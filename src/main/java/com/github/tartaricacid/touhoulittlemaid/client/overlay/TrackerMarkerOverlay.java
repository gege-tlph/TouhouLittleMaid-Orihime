package com.github.tartaricacid.touhoulittlemaid.client.overlay;

import com.github.tartaricacid.touhoulittlemaid.init.InitItems;
import com.github.tartaricacid.touhoulittlemaid.item.ItemFoxScroll;
import com.github.tartaricacid.touhoulittlemaid.item.ItemServantBell;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3fc;

import java.util.Optional;

/**
 * 狐狸卷轴与仆从铃的追踪标记。
 *
 * <p><b>这个标记必须画在 HUD 层，不能画在世界里。</b>「置顶」在 26.1.2 依旧是
 * {@code LevelRenderer.addLateDebugPass} 里对主渲染目标 {@code clearDepthTexture}
 * （反编译源实查：{@code alwaysOnTopPrimitives()} 非空即清）。光影包的深度附件被一并抹掉，
 * 玩家一切到铃铛地面就整片发白。行为基准在 1.21.11 上把三种世界内画法都实测过：</p>
 * <ul>
 *   <li>置顶的 gizmo：清深度贴图，地面发白——这正是玩家报的主症状；</li>
 *   <li>不置顶的 gizmo：标记恒被钳到渲染距离边缘，必然埋进地形；</li>
 *   <li>在世界渲染末尾自己画 SEE_THROUGH 文本：不开光影正常，开光影后被 composite 通道吃掉
 *       （同场景下原版罗盘叠加层一样看不见，所以不是我们这一笔画法的问题）。</li>
 * </ul>
 * <p>HUD 层不经过光影管线，是唯一同时满足「不破坏画面」与「一定看得见」的位置。</p>
 *
 * <p><b>与行为基准的两处写法差异（行为不变）</b>：① 真实 FOV 在 26.1.2 是
 * {@code Camera#getFov()} 且**公开**，基准当年为 {@code GameRenderer.getFov} 加过一条
 * access widener，本树不需要；② 目标点沿用宿主既有的「方块中心上方一格」，
 * 与 {@code origin/1.21.1} 一致（基准那边取的是方块角点，差半格，是移植期的副产物）。</p>
 */
public class TrackerMarkerOverlay implements HudElement {
    public static final TrackerMarkerOverlay INSTANCE = new TrackerMarkerOverlay();

    /**
     * 颜色<b>必须带 Alpha</b>：1.21.1 的 {@code Font} 会把 alpha 为 0 的颜色补成不透明，
     * 之后的版本删掉了那个兜底，照抄 {@code 0xff8800} 会渲染成全透明。
     */
    private static final int DISTANCE_TEXT_COLOR = 0xffff8800;
    private static final int ARROW_COLOR = 0xffff0000;

    /** 与基准一致：离得太近就不再提示 */
    private static final double MIN_TRACK_DISTANCE = 5.0;

    /** 视线正后方的点投影出来会翻到屏幕另一侧，必须先按深度剔除 */
    private static final double MIN_FORWARD_DEPTH = 0.1;

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor guiGraphics, @NotNull DeltaTracker deltaTracker) {
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

        Vec3 trackVec = Vec3.upFromBottomCenterOf(info.position(), 1);
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
        // 用基础值会让标记在这些状态下偏离女仆的真实方向。
        // Camera#getFov 返回的是**垂直** FOV、单位为度（Projection.setupPerspective 实查：
        // setPerspective(fov * PI/180, width/height, ...)），故 X 方向要再乘宽高比。
        double tanHalfFov = Math.tan(Math.toRadians(camera.getFov()) / 2.0);
        double aspect = (double) mc.getWindow().getWidth() / (double) mc.getWindow().getHeight();

        double ndcX = (rightOffset / depth) / (tanHalfFov * aspect);
        double ndcY = (upOffset / depth) / tanHalfFov;
        if (Math.abs(ndcX) > 1.0 || Math.abs(ndcY) > 1.0) {
            return;
        }

        Font font = mc.font;
        int x = (int) Math.round(guiGraphics.guiWidth() / 2.0 * (1.0 + ndcX));
        int y = (int) Math.round(guiGraphics.guiHeight() / 2.0 * (1.0 - ndcY));
        guiGraphics.centeredText(font, Math.round(actualDistance) + " m", x, y - font.lineHeight, DISTANCE_TEXT_COLOR);
        guiGraphics.centeredText(font, "▼", x, y, ARROW_COLOR);
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
