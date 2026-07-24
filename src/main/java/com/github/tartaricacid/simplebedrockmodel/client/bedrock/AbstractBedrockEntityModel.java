package com.github.tartaricacid.simplebedrockmodel.client.bedrock;

import com.github.tartaricacid.simplebedrockmodel.client.bedrock.model.BedrockPart;
import com.github.tartaricacid.simplebedrockmodel.client.bedrock.pojo.BedrockModelPOJO;
import com.github.tartaricacid.simplebedrockmodel.client.bedrock.pojo.BedrockVersion;
import net.fabricmc.loader.api.VersionParsingException;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.phys.AABB;
import org.apache.commons.lang3.tuple.Pair;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public abstract class AbstractBedrockEntityModel<T extends EntityRenderState> extends EntityModel<T> implements BedrockModelProvider {
    protected final HashMap<String, BedrockPart> modelMap = new HashMap<>();
    protected AABB renderBoundingBox;

    private AbstractBedrockEntityModel(BedrockPart root, Pair<HashMap<String, BedrockPart>, AABB> result) {

        super(root, RenderTypes::entityCutoutNoCull);
        if (result != null) {
            modelMap.putAll(result.getLeft());
            renderBoundingBox = result.getRight();
        } else {
            renderBoundingBox = new AABB(-1, 0, -1, 1, 2, 1);
        }
    }


    public AbstractBedrockEntityModel(InputStream stream) {
        this(new BedrockPart(), stream);
    }

    private AbstractBedrockEntityModel(BedrockPart root, InputStream stream) {
        this(root, loadFromStream(stream, root));
    }

    public AbstractBedrockEntityModel(BedrockModelPOJO pojo, BedrockVersion version) {
        this(new BedrockPart(), pojo, version);
    }

    private AbstractBedrockEntityModel(BedrockPart root, BedrockModelPOJO pojo, BedrockVersion version) {
        this(root, loadFromPOJO(pojo, version, root));
    }

    public AbstractBedrockEntityModel(BedrockModelPOJO pojo) throws VersionParsingException {
        this(pojo, BedrockVersion.getVersion(pojo));
    }

    public AbstractBedrockEntityModel() {
        this(new BedrockPart(), (Pair<HashMap<String, BedrockPart>, AABB>) null);
    }

    private static Pair<HashMap<String, BedrockPart>, AABB> loadFromStream(InputStream stream, BedrockPart root) {
        BedrockModelPOJO pojo = BedrockModelUtil.GSON.fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), BedrockModelPOJO.class);
        if (BedrockVersion.isLegacyVersion(pojo)) return LegacyModelReader.load(pojo, root);
        if (BedrockVersion.isNewVersion(pojo)) return NewModelReader.load(pojo, root);
        return null;
    }

    private static Pair<HashMap<String, BedrockPart>, AABB> loadFromPOJO(BedrockModelPOJO pojo, BedrockVersion version, BedrockPart root) {
        if (version == BedrockVersion.LEGACY) return LegacyModelReader.load(pojo, root);
        if (version == BedrockVersion.NEW) return NewModelReader.load(pojo, root);
        return null;
    }

    @Override
    public AABB getRenderBoundingBox() {
        return renderBoundingBox;
    }

    @Override
    public HashMap<String, BedrockPart> getModelMap() {
        return modelMap;
    }
}
