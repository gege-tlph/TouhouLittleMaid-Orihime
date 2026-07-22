package com.github.tartaricacid.simplebedrockmodel.client.bedrock;

import com.github.tartaricacid.simplebedrockmodel.client.bedrock.model.BedrockPart;
import com.github.tartaricacid.simplebedrockmodel.client.bedrock.pojo.BedrockModelPOJO;
import com.github.tartaricacid.simplebedrockmodel.client.bedrock.pojo.BedrockVersion;
import net.fabricmc.loader.api.VersionParsingException;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.phys.AABB;
import org.apache.commons.lang3.tuple.Pair;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public abstract class AbstractBedrockModel<T> extends Model<T> implements BedrockModelProvider {
    protected final HashMap<String, BedrockPart> modelMap = new HashMap<>();
    protected AABB renderBoundingBox;

    private AbstractBedrockModel(BedrockPart root, Pair<HashMap<String, BedrockPart>, AABB> result) {
        super(root, RenderTypes::entityCutout);
        if (result != null) {
            modelMap.putAll(result.getLeft());
            renderBoundingBox = result.getRight();
        } else {
            renderBoundingBox = new AABB(-1, 0, -1, 1, 2, 1);
        }
    }

    // 几何数据必须加载到传给父类的同一个根部件，否则 Model.root() 会指向空模型。
    // 通过中间构造器先完成加载，再把该根部件交给父类。
    public AbstractBedrockModel(InputStream stream) {
        this(new BedrockPart(), stream);
    }

    private AbstractBedrockModel(BedrockPart root, InputStream stream) {
        this(root, loadFromStream(stream, root));
    }

    public AbstractBedrockModel(BedrockModelPOJO pojo, BedrockVersion version) {
        this(new BedrockPart(), pojo, version);
    }

    private AbstractBedrockModel(BedrockPart root, BedrockModelPOJO pojo, BedrockVersion version) {
        this(root, loadFromPOJO(pojo, root, version));
    }

    public AbstractBedrockModel(BedrockModelPOJO pojo) throws VersionParsingException {
        this(pojo, BedrockVersion.getVersion(pojo));
    }

    public AbstractBedrockModel() {
        this(new BedrockPart(), (Pair<HashMap<String, BedrockPart>, AABB>) null);
    }

    private static Pair<HashMap<String, BedrockPart>, AABB> loadFromStream(InputStream stream, BedrockPart root) {
        BedrockModelPOJO pojo = BedrockModelUtil.GSON.fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), BedrockModelPOJO.class);
        return loadFromPOJO(pojo, root);
    }

    private static Pair<HashMap<String, BedrockPart>, AABB> loadFromPOJO(BedrockModelPOJO pojo, BedrockPart root) {
        if (BedrockVersion.isLegacyVersion(pojo)) {
            return LegacyModelReader.load(pojo, root);
        }
        if (BedrockVersion.isNewVersion(pojo)) {
            return NewModelReader.load(pojo, root);
        }
        return null;
    }

    private static Pair<HashMap<String, BedrockPart>, AABB> loadFromPOJO(BedrockModelPOJO pojo, BedrockPart root, BedrockVersion version) {
        if (version == BedrockVersion.LEGACY) {
            return LegacyModelReader.load(pojo, root);
        }
        if (version == BedrockVersion.NEW) {
            return NewModelReader.load(pojo, root);
        }
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
