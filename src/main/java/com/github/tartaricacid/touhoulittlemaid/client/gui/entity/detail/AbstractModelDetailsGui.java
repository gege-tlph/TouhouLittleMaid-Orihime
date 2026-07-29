package com.github.tartaricacid.touhoulittlemaid.client.gui.entity.detail;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import cn.sh1rocu.touhoulittlemaid.mixin.accessor.GuiGraphicsAccessor;
import cn.sh1rocu.touhoulittlemaid.mixin.accessor.ScreenAccessor;
import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.github.tartaricacid.touhoulittlemaid.client.gui.widget.button.TouhouImageButton;
import com.github.tartaricacid.touhoulittlemaid.client.model.DebugFloorModel;
import com.github.tartaricacid.touhoulittlemaid.client.resource.pojo.IModelInfo;
import com.github.tartaricacid.touhoulittlemaid.util.ParseI18n;
import com.github.tartaricacid.touhoulittlemaid.util.Rectangle;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import javax.annotation.Nullable;

public abstract class AbstractModelDetailsGui<T extends LivingEntity, E extends IModelInfo> extends Screen {
    private static final Identifier BUTTON_TEXTURE = Identifier.fromNamespaceAndPath(TouhouLittleMaid.MOD_ID, "textures/gui/skin_detail.png");
    static final Identifier FLOOR_TEXTURE = Identifier.fromNamespaceAndPath(TouhouLittleMaid.MOD_ID, "textures/entity/debug_floor.png");

    private static final int LEFT_MOUSE_BUTTON = 0;
    private static final int RIGHT_MOUSE_BUTTON = 1;

    private static final float SCALE_MAX = 360f;
    private static final float SCALE_MIN = 18f;
    private static final float PITCH_MAX = 90f;
    private static final float PITCH_MIN = -90f;

    private static Rectangle BACKGROUND_SIZE;
    private static Rectangle BOTTOM_STATUS_BAR_SIZE;
    private static Rectangle SIDE_MENU_SIZE;
    private static Rectangle TOP_STATUS_BAR_SIZE;

    protected final DebugFloorModel floorModel;

    protected T sourceEntity;
    protected volatile T guiEntity;
    protected E modelInfo;

    private float posX = 0;
    private float posY = 25;
    private float scale = 80;
    private float yaw = 145;
    private float pitch = 0;
    private boolean showFloor = true;

    // 1.21.11：GUI 实体渲染 PiP 化后，renderEntity 每帧计算的共享变换，供 renderExtraEntity/submitExtraEntity 复用
    // （对应 origin 中主实体与额外实体共用同一 poseStack 与摄像机覆盖）
    private Quaternionf pipRotation;
    private Quaternionf pipCameraOverride;
    private Vector3f pipTranslation;
    private int pipX0;
    private int pipY0;
    private int pipX1;
    private int pipY1;

    public AbstractModelDetailsGui(T sourceEntity, @Nullable T guiEntity, E modelInfo) {
        super(Component.translatable("gui.touhou_little_maid.custom_model_details_gui.title"));
        this.sourceEntity = sourceEntity;
        this.guiEntity = guiEntity;
        this.modelInfo = modelInfo;
        this.floorModel = new DebugFloorModel(Minecraft.getInstance().getEntityModels().bakeLayer(DebugFloorModel.LAYER));
    }

    /**
     * Click return button action
     */
    abstract protected void applyReturnButtonLogic();

    /**
     * Init side button
     */
    abstract protected void initSideButton();

    /**
     * Render extra entity in main window
     * <p>
     * 1.21.11：origin 签名为 (EntityRenderDispatcher, PoseStack, MultiBufferSource.BufferSource)，
     * GUI 实体渲染 PiP 化后直渲路径删除，改为通过 {@link #submitExtraEntity} 提交；
     * 偏移语义与 origin 的 manager.render(entity, x, y, z, 0, partialTicks, ...) 一致
     *
     * @param graphics     GuiGraphics
     * @param partialTicks 部分刻
     */
    abstract protected void renderExtraEntity(GuiGraphics graphics, float partialTicks);

    @Override
    protected void init() {
        this.clearWidgets();

        BACKGROUND_SIZE = new Rectangle(0, 0, width, height);
        BOTTOM_STATUS_BAR_SIZE = new Rectangle(0, height - 16, width, height);
        SIDE_MENU_SIZE = new Rectangle(0, 0, 132, height);
        TOP_STATUS_BAR_SIZE = new Rectangle(0, 0, width, 15);

        TouhouImageButton closeButton = new TouhouImageButton(width - 15, 0, 15, 15,
                0, 24, 15, BUTTON_TEXTURE, b -> Minecraft.getInstance().setScreen(null));
        TouhouImageButton floorButton = new TouhouImageButton(width - 30, 0, 15, 15,
                30, 24, 15, BUTTON_TEXTURE, b -> showFloor = !showFloor);
        TouhouImageButton returnButton = new TouhouImageButton(width - 45, 0, 15, 15,
                15, 24, 15, BUTTON_TEXTURE, b -> applyReturnButtonLogic());
        addRenderableWidget(closeButton);
        addRenderableWidget(floorButton);
        addRenderableWidget(returnButton);

        this.initSideButton();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        if (minecraft == null) {
            return;
        }
        this.renderViewBg(graphics);
        this.renderEntity((width + 132) / 2, height / 2 + 50, graphics);
        this.renderBottomStatueBar(graphics);
        this.fillGradient(graphics, SIDE_MENU_SIZE, 0xfe21252b);
        this.fillGradient(graphics, TOP_STATUS_BAR_SIZE, 0xfe282c34);
        graphics.drawString(font, getTitle(), 6, 4, 0xffaaaaaa);
        for (Renderable renderable : ((ScreenAccessor) this).tlm$getRenderables()) {
            renderable.render(graphics, mouseX, mouseY, partialTicks);
        }
    }

    private void renderViewBg(GuiGraphics graphics) {
        // 1.21.11：GUI 2D 化，z 层级由提交顺序决定；origin 的 z=-999 背景填充与 z=-900 pose 平移
        // （均仅调 z）按迁移规范丢弃，绘制顺序（背景→帮助文字→实体→状态栏）与 origin 视觉层级一致
        this.fillGradient(graphics, BACKGROUND_SIZE, 0xfe17191d);
        graphics.drawString(font, Component.translatable("gui.touhou_little_maid.skin_details.left_mouse"), (int) SIDE_MENU_SIZE.w + 4, (int) TOP_STATUS_BAR_SIZE.h + 4, 0xffaaaaaa, false);
        graphics.drawString(font, Component.translatable("gui.touhou_little_maid.skin_details.right_mouse"), (int) SIDE_MENU_SIZE.w + 4, (int) TOP_STATUS_BAR_SIZE.h + 14, 0xffaaaaaa, false);
        graphics.drawString(font, Component.translatable("gui.touhou_little_maid.skin_details.mouse_wheel"), (int) SIDE_MENU_SIZE.w + 4, (int) TOP_STATUS_BAR_SIZE.h + 24, 0xffaaaaaa, false);
    }

    private void renderBottomStatueBar(GuiGraphics graphics) {
        this.fillGradient(graphics, BOTTOM_STATUS_BAR_SIZE, 0xfe282c34);
        String name = String.format("%s %s", "✔", I18n.get(ParseI18n.getI18nKey(modelInfo.getName())));
        String info = String.format("%d FPS %.2f%%", Minecraft.fps, scale * 100 / 80);
        graphics.drawString(font, name, 136, this.height - 12, 0xffcacad4, false);
        graphics.drawString(font, info, this.width - font.width(info) - 4, this.height - 12, 0xffcacad4, false);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        boolean isInWidthRange = 132 < mouseX && mouseX < width - 1;
        boolean isInHeightRange = 15 < mouseY && mouseY < height - 16;
        boolean isInRange = isInWidthRange && isInHeightRange;
        if (minecraft == null || !isInRange) {
            return false;
        }
        if (button == LEFT_MOUSE_BUTTON) {
            yaw += dragX;
            changePitchValue((float) dragY);
        }
        if (button == RIGHT_MOUSE_BUTTON) {
            posX += dragX;
            posY += dragY;
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        boolean isInWidthRange = 132 < mouseX && mouseX < width - 1;
        boolean isInHeightRange = 15 < mouseY && mouseY < height - 16;
        boolean isInRange = isInWidthRange && isInHeightRange;
        if (minecraft == null || !isInRange) {
            return false;
        }
        if (deltaY != 0) {
            changeScaleValue((float) deltaY * 0.07f);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }

    private void changePitchValue(float amount) {
        if (pitch - amount > PITCH_MAX) {
            pitch = 90;
        } else if (pitch - amount < PITCH_MIN) {
            pitch = -90;
        } else {
            pitch = pitch - amount;
        }
    }

    private void changeScaleValue(float amount) {
        float tmp = scale + amount * scale;
        scale = Mth.clamp(tmp, SCALE_MIN, SCALE_MAX);
    }


    private void renderEntity(int middleWidth, int middleHeight, GuiGraphics graphics) {
        graphics.enableScissor(132, 15, this.width, this.height - 16);

        // 1.21.11：GUI 全面 2D 化，origin 的 PoseStack + EntityRenderDispatcher.render + runAsFancy
        // 直渲路径已删除，实体渲染改为 PiP 提交（GuiGraphics.submitEntityRenderState，内部
        // GuiEntityRenderer 自带 ENTITY_IN_UI 光照，等价 origin 的 Lighting.setupForEntityInInventory）。
        // origin 变换链：translate(posX+middleWidth, posY+middleHeight, 1000) → scale(scale, scale, -scale) → mulPose(zp*yp*xp)
        Quaternionf zp = Axis.ZP.rotationDegrees(-180.0F);
        Quaternionf yp = Axis.YP.rotationDegrees(yaw);
        Quaternionf xp = Axis.XP.rotationDegrees(-pitch);
        yp.mul(xp);
        zp.mul(yp);

        this.pipX0 = 132;
        this.pipY0 = 15;
        this.pipX1 = this.width;
        this.pipY1 = this.height - 16;
        this.pipRotation = zp;
        // origin 先 xp.conjugate() 再 overrideCameraOrientation(xp)；1.21.11 的 GuiEntityRenderer
        // 会对 overrideCameraAngle 自行 conjugate（对照 InventoryScreen 1.21.1→1.21.11 同位迁移），故传未共轭的 xp
        this.pipCameraOverride = xp;
        // PiP 把实体原点置于视口 (x0,y0,x1,y1) 中心；origin 原点 = (middleWidth+posX, middleHeight+posY)
        // （GUI 像素）；PiP 的 translation 在 scale 之后应用（实体单位），故除以 scale 折算
        float centerX = (this.pipX0 + this.pipX1) / 2.0F;
        float centerY = (this.pipY0 + this.pipY1) / 2.0F;
        this.pipTranslation = new Vector3f((middleWidth + posX - centerX) / scale, (middleHeight + posY - centerY) / scale, 0);

        float partialTicks = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
        EntityRenderState renderState = extractRenderState(guiEntity, partialTicks);
        graphics.submitEntityRenderState(renderState, scale, this.pipTranslation, this.pipRotation, this.pipCameraOverride,
                this.pipX0, this.pipY0, this.pipX1, this.pipY1);

        if (showFloor) {
            // origin 在主实体后同一 poseStack 直渲地板；1.21.11 经自定义 PiP（DebugFloorPiPRenderer，
            // SpecialGuiElementRegistry 注册）提交，变换与主实体一致，0.5 偏移在渲染器内补
            ScreenRectangle scissor = new ScreenRectangle(this.pipX0, this.pipY0, this.pipX1 - this.pipX0, this.pipY1 - this.pipY0);
            ((GuiGraphicsAccessor) graphics).tlm$getGuiRenderState().submitPicturesInPictureState(
                    new DebugFloorRenderState(this.floorModel, this.pipTranslation, this.pipRotation,
                            this.pipX0, this.pipY0, this.pipX1, this.pipY1, this.scale, scissor));
        }
        this.renderExtraEntity(graphics, partialTicks);

        graphics.disableScissor();
    }

    /**
     * 1.21.11：以与主实体相同的变换提交额外实体，等价 origin renderExtraEntity 中
     * manager.render(entity, x, y, z, 0, partialTicks, poseStack, buffer, 0xf000f0)
     * （该 poseStack 已在主实体渲染后 translate(0, 0.5, 0)）。
     * PiP 的 translation 在 mulPose 之前应用，故把 origin 的旋转后偏移折算回未旋转空间：v' = v + R·(x, 0.5+y, z)
     */
    protected void submitExtraEntity(GuiGraphics graphics, LivingEntity entity, float x, float y, float z, float partialTicks) {
        Vector3f offset = this.pipRotation.transform(new Vector3f(x, 0.5F + y, z));
        Vector3f translation = new Vector3f(this.pipTranslation).add(offset);
        EntityRenderState renderState = extractRenderState(entity, partialTicks);
        graphics.submitEntityRenderState(renderState, scale, translation, this.pipRotation, this.pipCameraOverride,
                this.pipX0, this.pipY0, this.pipX1, this.pipY1);
    }

    /**
     * 1.21.11：从实体提取渲染状态（对照 InventoryScreen 同位实现）；
     * lightCoords=0xf000f0 与 shadowPieces 清空分别等价 origin 的固定光照与 setRenderShadow(false)
     */
    private static EntityRenderState extractRenderState(LivingEntity entity, float partialTicks) {
        EntityRenderDispatcher manager = Minecraft.getInstance().getEntityRenderDispatcher();
        EntityRenderer<? super LivingEntity, ?> renderer = manager.getRenderer(entity);
        EntityRenderState renderState = renderer.createRenderState(entity, partialTicks);
        renderState.lightCoords = 0xf000f0;
        renderState.shadowPieces.clear();
        renderState.outlineColor = 0;
        return renderState;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void fillGradient(GuiGraphics graphics, Rectangle vec4d, int color) {
        graphics.fillGradient((int) vec4d.x, (int) vec4d.y, (int) vec4d.w, (int) vec4d.h, color, color);
    }

    // 1.21.11：带 zLevel 的 fillGradient 重载已删除（z 由提交顺序决定），origin 的 (graphics, rect, color, zLevel)
    // 私有重载并入上方 4 参版本，唯一调用点（BACKGROUND_SIZE, z=-999）已改为按顺序先绘制
}
