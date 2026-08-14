package com.github.tartaricacid.touhoulittlemaid.client.resource.models;

import com.github.tartaricacid.touhoulittlemaid.client.gui.entity.cache.CacheIconManager;
import com.github.tartaricacid.touhoulittlemaid.client.model.bedrock.EntityChairModel;
import com.github.tartaricacid.touhoulittlemaid.client.renderer.entity.state.EntityChairRenderState;
import com.github.tartaricacid.touhoulittlemaid.client.resource.pojo.ChairModelInfo;
import com.github.tartaricacid.touhoulittlemaid.client.resource.pojo.CustomModelPack;
import org.jspecify.annotations.Nullable;

public final class ChairModels extends AbstractClientModels<EntityChairModel, ChairModelInfo, EntityChairRenderState> {
    private static @Nullable ChairModels INSTANCE;

    private ChairModels() {
        super("maid_chair.json", DefaultPackConstant.CHAIR_SORT);
    }

    public static ChairModels getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ChairModels();
        }
        return INSTANCE;
    }

    /** origin/1.21.1 在 ChairModels.addPack 内联登记图标缓存队列；宿主重构出 AbstractClientModels 后由本覆写承接 */
    @Override
    public void addPack(CustomModelPack<ChairModelInfo> pack) {
        super.addPack(pack);
        CacheIconManager.addChairPack(pack);
    }

    public float getModelMountedYOffset(String modelId) {
        if (idInfoMap.containsKey(modelId)) {
            return idInfoMap.get(modelId).getMountedYOffset();
        }
        return 0.0f;
    }

    public boolean getModelTameableCanRide(String modelId) {
        if (idInfoMap.containsKey(modelId)) {
            return idInfoMap.get(modelId).isTameableCanRide();
        }
        return true;
    }

    public boolean getModelNoGravity(String modelId) {
        if (idInfoMap.containsKey(modelId)) {
            return idInfoMap.get(modelId).isNoGravity();
        }
        return false;
    }
}
