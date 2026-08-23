package com.github.tartaricacid.touhoulittlemaid.client.gui.entity.cache;

import com.github.tartaricacid.touhoulittlemaid.api.client.render.MaidRenderState;
import com.github.tartaricacid.touhoulittlemaid.blockentity.BlockEntityModelSwitcher;
import com.github.tartaricacid.touhoulittlemaid.client.gui.block.ModelSwitcherGui;
import com.github.tartaricacid.touhoulittlemaid.client.gui.block.ModelSwitcherModelGui;
import com.github.tartaricacid.touhoulittlemaid.client.gui.entity.model.AbstractModelGui;
import com.github.tartaricacid.touhoulittlemaid.client.gui.entity.model.ChairModelGui;
import com.github.tartaricacid.touhoulittlemaid.client.gui.entity.model.MaidModelGui;
import com.github.tartaricacid.touhoulittlemaid.client.resource.pojo.ChairModelInfo;
import com.github.tartaricacid.touhoulittlemaid.client.resource.pojo.CustomModelPack;
import com.github.tartaricacid.touhoulittlemaid.client.resource.pojo.MaidModelInfo;
import com.github.tartaricacid.touhoulittlemaid.config.subconfig.MiscConfig;
import com.github.tartaricacid.touhoulittlemaid.entity.item.EntityChair;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.util.EntityCacheUtil;
import com.github.tartaricacid.touhoulittlemaid.util.migrate.ScreenUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntitySpawnReason;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import static com.github.tartaricacid.touhoulittlemaid.client.resource.models.SpecialMaidModelResolver.EASTER_EGG_MODEL;
import static com.github.tartaricacid.touhoulittlemaid.util.EntityCacheUtil.clearMaidDataResidue;

@Environment(EnvType.CLIENT)
public final class CacheIconManager {
    private static final LinkedList<MaidModelInfo> MAID_CACHE_QUEUE = new LinkedList<>();
    private static final LinkedList<ChairModelInfo> CHAIR_CACHE_QUEUE = new LinkedList<>();
    /**
     * 本会话已注册的缓存图标。26.1.2 的 TextureManager.byPath 已私有化，且 getTexture 对未注册 id
     * 会自动 registerAndLoad 一个 SimpleTexture 并尝试从资源包加载（反编译实查）——不能当存在性探针。
     * 缓存图标只会由 CacheScreen 注册，故由本类自持注册表，替代基准直接读 byPath 的写法
     */
    private static final Set<Identifier> CACHED_ICONS = new HashSet<>();

    public static void clearCache() {
        MAID_CACHE_QUEUE.clear();
        CHAIR_CACHE_QUEUE.clear();
    }

    public static void addMaidPack(CustomModelPack<MaidModelInfo> customModelPack) {
        MAID_CACHE_QUEUE.addAll(customModelPack.getModelList());
    }

    public static void addChairPack(CustomModelPack<ChairModelInfo> customModelPack) {
        CHAIR_CACHE_QUEUE.addAll(customModelPack.getModelList());
    }

    static void markIconCached(Identifier cacheIconId) {
        CACHED_ICONS.add(cacheIconId);
    }

    public static boolean isIconCached(Identifier cacheIconId) {
        return CACHED_ICONS.contains(cacheIconId);
    }

    public static void openMaidModelGui(EntityMaid maid) {
        MaidModelGui maidModelGui = new MaidModelGui(maid);
        if (MiscConfig.MODEL_ICON_CACHE.get() && !MAID_CACHE_QUEUE.isEmpty()) {
            ScreenUtil.setScreen(getMaidCacheScreen(maidModelGui));
        } else {
            ScreenUtil.setScreen(maidModelGui);
        }
    }

    public static void openChairModelGui(EntityChair chair) {
        ChairModelGui chairModelGui = new ChairModelGui(chair);
        if (MiscConfig.MODEL_ICON_CACHE.get() && !CHAIR_CACHE_QUEUE.isEmpty()) {
            ScreenUtil.setScreen(getChairCacheScreen(chairModelGui));
        } else {
            ScreenUtil.setScreen(chairModelGui);
        }
    }

    public static void openModelSwitcherModelGui(EntityMaid maid, BlockEntityModelSwitcher.ModeInfo info, ModelSwitcherGui modelSwitcherGui) {
        ModelSwitcherModelGui switcherModelGui = new ModelSwitcherModelGui(maid, info, modelSwitcherGui);
        if (MiscConfig.MODEL_ICON_CACHE.get() && !MAID_CACHE_QUEUE.isEmpty()) {
            ScreenUtil.setScreen(getMaidCacheScreen(switcherModelGui));
        } else {
            ScreenUtil.setScreen(switcherModelGui);
        }
    }

    private static CacheScreen<EntityMaid, MaidModelInfo> getMaidCacheScreen(AbstractModelGui<EntityMaid, MaidModelInfo> maidModelGui) {
        return new CacheScreen<>(maidModelGui, MAID_CACHE_QUEUE,
                world -> EntityCacheUtil.getMaid(world, EntitySpawnReason.COMMAND),
                (graphics, posX, posY, modelInfo, scaleModified, maid) -> {
                    // 与 MaidModelGui.drawEntity 同一套 GUI 预览 idiom；
                    // 基准的 setIsYsmModel(false) 随 YSM 一起不存在于 26.1.2 Fabric，删去
                    maid.renderState = MaidRenderState.GUI;
                    clearMaidDataResidue(maid, false);
                    if (modelInfo.getEasterEgg() != null) {
                        maid.setModelId(EASTER_EGG_MODEL);
                    } else {
                        maid.setModelId(modelInfo.getModelId().toString());
                    }
                    int half = scaleModified / 2;
                    int yOffset = scaleModified * 6 / 5;
                    InventoryScreen.extractEntityInInventoryFollowsMouse(
                            graphics,
                            posX,
                            posY - 3,
                            posX + scaleModified,
                            posY + yOffset - 3,
                            (int) (half * modelInfo.getRenderItemScale()),
                            0.1F,
                            posX + half + 25,
                            posY + yOffset / 2f + 15,
                            maid);
                });
    }

    private static CacheScreen<EntityChair, ChairModelInfo> getChairCacheScreen(ChairModelGui chairModelGui) {
        return new CacheScreen<>(chairModelGui, CHAIR_CACHE_QUEUE,
                world -> EntityCacheUtil.getChair(world, EntitySpawnReason.COMMAND),
                (graphics, posX, posY, modelInfo, scaleModified, chair) -> {
                    chair.setModelId(modelInfo.getModelId().toString());

                    int half = scaleModified / 2;
                    int yOffset = scaleModified * 3 / 2;
                    InventoryScreen.extractEntityInInventoryFollowsMouse(
                            graphics,
                            posX,
                            posY,
                            posX + scaleModified,
                            posY + yOffset,
                            (int) (half * modelInfo.getRenderItemScale() * 0.9),
                            0.1F,
                            posX + half + 20,
                            posY + yOffset / 2f + 10,
                            chair);
                });
    }
}
