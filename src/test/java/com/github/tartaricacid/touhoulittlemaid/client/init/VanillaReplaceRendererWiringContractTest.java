package com.github.tartaricacid.touhoulittlemaid.client.init;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 史莱姆/岩浆怪/经验球替换渲染必须有真实消费链：渲染器源文件在编译集内、
 * 三个 EntityType 注册处于激活状态、三个配置各自只被自己的渲染器消费、
 * 两个油库里 Bedrock 模型已注册、旧配置继承迁移已接线。
 * 仅“类存在/按钮存在”不构成消费者。
 */
class VanillaReplaceRendererWiringContractTest {
    private static final Path ROOT = Path.of("..", "..");
    private static final Path RENDER_DIR = ROOT.resolve(Path.of("src", "main", "java", "com", "github",
            "tartaricacid", "touhoulittlemaid", "client", "renderer", "entity"));
    private static final Path YUKKURI = RENDER_DIR.resolve("EntityYukkuriSlimeRender.java");
    private static final Path MARISA = RENDER_DIR.resolve("EntityMarisaYukkuriSlimeRender.java");
    private static final Path ORB = RENDER_DIR.resolve("ReplaceExperienceOrbRenderer.java");
    private static final Path INIT_RENDER = ROOT.resolve(Path.of("src", "main", "java", "com", "github",
            "tartaricacid", "touhoulittlemaid", "client", "init", "InitEntitiesRender.java"));
    private static final Path MODEL_REGISTRY = ROOT.resolve(Path.of("src", "main", "java", "com", "github",
            "tartaricacid", "touhoulittlemaid", "client", "resource", "bedrock", "InternalBedrockModelRegistry.java"));
    private static final Path VANILLA_CONFIG = ROOT.resolve(Path.of("src", "main", "java", "com", "github",
            "tartaricacid", "touhoulittlemaid", "config", "subconfig", "VanillaConfig.java"));
    private static final Path GENERAL_CONFIG = ROOT.resolve(Path.of("src", "main", "java", "com", "github",
            "tartaricacid", "touhoulittlemaid", "config", "GeneralConfig.java"));
    private static final Path MENU_INTEGRATION = ROOT.resolve(Path.of("src", "main", "java", "com", "github",
            "tartaricacid", "touhoulittlemaid", "compat", "cloth", "MenuIntegration.java"));
    private static final Path FABRIC_ENTRYPOINT = ROOT.resolve(Path.of("src", "main", "java", "cn", "sh1rocu",
            "touhoulittlemaid", "TouhouLittleMaidFabric.java"));
    private static final Path BUILD_GRADLE = ROOT.resolve("build.gradle");
    private static final Path ASSETS = ROOT.resolve(Path.of("src", "main", "resources", "assets", "touhou_little_maid"));

    @Test
    void rendererSourcesExistAndAreNotExcludedFromCompilation() throws IOException {
        assertTrue(Files.isRegularFile(YUKKURI), "EntityYukkuriSlimeRender.java 必须回到编译树");
        assertTrue(Files.isRegularFile(MARISA), "EntityMarisaYukkuriSlimeRender.java 必须回到编译树");
        assertTrue(Files.isRegularFile(ORB), "ReplaceExperienceOrbRenderer.java 必须回到编译树");

        String activeGradle = activeSource(BUILD_GRADLE);
        assertFalse(activeGradle.contains("EntityYukkuriSlimeRender"),
                "build.gradle 不得排除史莱姆替换渲染器");
        assertFalse(activeGradle.contains("EntityMarisaYukkuriSlimeRender"),
                "build.gradle 不得排除岩浆怪替换渲染器");
        assertFalse(activeGradle.contains("ReplaceExperienceOrbRenderer"),
                "build.gradle 不得排除经验球替换渲染器");
    }

    @Test
    void entityTypeRegistrationsAreActive() throws IOException {
        String active = activeSource(INIT_RENDER);
        assertTrue(active.contains("EntityRendererRegistry.register(EntityType.SLIME, EntityYukkuriSlimeRender::new)"),
                "SLIME 渲染器注册必须处于激活状态（非注释）");
        assertTrue(active.contains("EntityRendererRegistry.register(EntityType.MAGMA_CUBE, EntityMarisaYukkuriSlimeRender::new)"),
                "MAGMA_CUBE 渲染器注册必须处于激活状态（非注释）");
        assertTrue(active.contains("EntityRendererRegistry.register(EntityType.EXPERIENCE_ORB, ReplaceExperienceOrbRenderer::new)"),
                "EXPERIENCE_ORB 渲染器注册必须处于激活状态（非注释）");
    }

    @Test
    void eachConfigHasItsOwnRendererConsumer() throws IOException {
        String yukkuri = activeSource(YUKKURI);
        assertTrue(yukkuri.contains("VanillaConfig.REPLACE_SLIME_MODEL.get()"),
                "史莱姆渲染器必须以 REPLACE_SLIME_MODEL 分流");
        assertFalse(yukkuri.contains("REPLACE_MAGMA_CUBE_MODEL"),
                "史莱姆渲染器不得读取岩浆怪开关");
        assertTrue(yukkuri.contains("SlimeRenderer"),
                "史莱姆渲染器关闭时必须委托原版 SlimeRenderer");

        String marisa = activeSource(MARISA);
        assertTrue(marisa.contains("VanillaConfig.REPLACE_MAGMA_CUBE_MODEL.get()"),
                "岩浆怪渲染器必须以独立的 REPLACE_MAGMA_CUBE_MODEL 分流");
        assertFalse(marisa.contains("REPLACE_SLIME_MODEL"),
                "岩浆怪渲染器不得再共享史莱姆开关");
        assertTrue(marisa.contains("MagmaCubeRenderer"),
                "岩浆怪渲染器关闭时必须委托原版 MagmaCubeRenderer");

        String orb = activeSource(ORB);
        assertTrue(orb.contains("VanillaConfig.REPLACE_XP_TEXTURE.get()"),
                "经验球渲染器必须以 REPLACE_XP_TEXTURE 分流");
        assertTrue(orb.contains("ExperienceOrbRenderer"),
                "经验球渲染器关闭时必须委托原版 ExperienceOrbRenderer");
    }

    @Test
    void yukkuriBedrockModelsAreRegistered() throws IOException {
        String registry = activeSource(MODEL_REGISTRY);
        assertTrue(registry.contains("addEntityModel(\"reimu_yukkuri\")"),
                "InternalBedrockModelRegistry 必须注册 reimu_yukkuri 实体模型");
        assertTrue(registry.contains("addEntityModel(\"marisa_yukkuri\")"),
                "InternalBedrockModelRegistry 必须注册 marisa_yukkuri 实体模型");

        assertTrue(Files.isRegularFile(ASSETS.resolve(Path.of("models", "bedrock", "entity", "reimu_yukkuri.json"))));
        assertTrue(Files.isRegularFile(ASSETS.resolve(Path.of("models", "bedrock", "entity", "marisa_yukkuri.json"))));
        assertTrue(Files.isRegularFile(ASSETS.resolve(Path.of("textures", "bedrock", "entity", "reimu_yukkuri.png"))));
        assertTrue(Files.isRegularFile(ASSETS.resolve(Path.of("textures", "bedrock", "entity", "marisa_yukkuri.png"))));
        assertTrue(Files.isRegularFile(ASSETS.resolve(Path.of("textures", "entity", "point_item.png"))));
    }

    @Test
    void magmaCubeConfigIsDefinedRegisteredAndDisplayed() throws IOException {
        String vanillaConfig = activeSource(VANILLA_CONFIG);
        assertTrue(vanillaConfig.contains("define(\"ReplaceMagmaCubeModel\", false)"),
                "VanillaConfig 必须定义独立的 ReplaceMagmaCubeModel（默认按用户定案为 false）");
        assertTrue(vanillaConfig.contains("REPLACE_MAGMA_CUBE_MODEL"),
                "VanillaConfig 必须暴露 REPLACE_MAGMA_CUBE_MODEL 字段");

        assertTrue(activeSource(GENERAL_CONFIG).contains("VanillaConfig.REPLACE_MAGMA_CUBE_MODEL"),
                "GeneralConfig.values() 必须包含岩浆怪开关，否则迁移/持久化链漏项");

        String menu = activeSource(MENU_INTEGRATION);
        int slime = menu.indexOf("vanilla.replace_slime_model");
        int magma = menu.indexOf("vanilla.replace_magma_cube_model");
        int xp = menu.indexOf("vanilla.replace_xp_texture");
        assertTrue(magma > 0, "配置菜单必须包含岩浆怪开关");
        assertTrue(slime < magma && magma < xp, "岩浆怪开关必须位于史莱姆之后、经验球之前");

        for (String lang : new String[]{"en_us", "zh_cn"}) {
            String json = Files.readString(ASSETS.resolve(Path.of("lang", lang + ".json")));
            assertTrue(json.contains("config.touhou_little_maid.vanilla.replace_magma_cube_model"),
                    lang + " 必须包含岩浆怪开关翻译");
            assertTrue(json.contains("config.touhou_little_maid.vanilla.replace_magma_cube_model.tooltip"),
                    lang + " 必须包含岩浆怪开关 tooltip");
        }
    }

    @Test
    void legacyMagmaCubeInheritanceIsWiredBeforeConfigLoad() throws IOException {
        String migration = activeSource(ROOT.resolve(Path.of("src", "main", "java", "com", "github",
                "tartaricacid", "touhoulittlemaid", "config", "ConfigFileMigration.java")));
        assertTrue(migration.contains("ReplaceMagmaCubeModel"),
                "ConfigFileMigration 必须处理旧文件缺失 ReplaceMagmaCubeModel 的继承");

        String entrypoint = activeSource(FABRIC_ENTRYPOINT);
        int inherit = entrypoint.indexOf("inheritMagmaCubeFromSlime");
        int register = entrypoint.indexOf("ConfigRegistry.INSTANCE.register");
        assertTrue(inherit > 0, "客户端入口必须调用岩浆怪开关继承迁移");
        assertTrue(register > 0 && inherit < register,
                "继承迁移必须发生在 ConfigRegistry 加载 global TOML 之前，否则缺键被默认 true 补掉");
    }

    private static String activeSource(Path path) throws IOException {
        StringBuilder active = new StringBuilder();
        for (String line : Files.readAllLines(path)) {
            String trimmed = line.trim();
            if (trimmed.startsWith("//")) {
                continue;
            }
            active.append(line).append('\n');
        }
        return active.toString();
    }
}
