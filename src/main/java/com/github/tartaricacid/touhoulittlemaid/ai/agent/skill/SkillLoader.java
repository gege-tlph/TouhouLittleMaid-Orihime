package com.github.tartaricacid.touhoulittlemaid.ai.agent.skill;

import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.xml.XmlEscapers;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class SkillLoader {
    /**
     * 用自己的 logger 而不是 {@code TouhouLittleMaid.LOGGER}：后者会触发 TouhouLittleMaid 的
     * 类初始化，而那里的 {@code DEBUG = FabricLoader.getInstance().isDevelopmentEnvironment()}
     * 在纯 JUnit 环境里 NPE——于是本类一碰就 ExceptionInInitializerError，纯逻辑也没法测。
     */
    private static final Logger LOGGER = LogManager.getLogger(SkillLoader.class);
    private static final int MAX_DEPTH = 3;
    private static final String SKILL_FILE_NAME = "skill.md";
    private static final String REFERENCES = "references";

    /**
     * 来自配置文件目录下的 skill
     */
    private static Map<String, SkillInstance> CONFIG_SKILLS = Maps.newLinkedHashMap();
    private static Map<String, SkillInstance> DATA_PACK_SKILLS = Maps.newLinkedHashMap();

    public static void init() {
        Path skillsDir = getSkillsDir();
        createSkillsFolder(skillsDir);
        reloadFromConfig(skillsDir);
    }

    /**
     * 目录作为参数传入而不是常量：原先它是 {@code static final} 且在类加载时调
     * {@code FabricLoader.getInstance()}，于是这个类在纯 JUnit 环境里一碰就炸，
     * 优先级这类纯逻辑也就没法测。
     */
    static void reloadFromConfig(Path skillsDir) {
        Map<String, SkillInstance> loaded = Maps.newLinkedHashMap();
        try (var stream = Files.walk(skillsDir, MAX_DEPTH)) {
            stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().equalsIgnoreCase(SKILL_FILE_NAME))
                    .forEach(path -> loadSkillFromConfig(path, loaded));
        } catch (IOException e) {
            LOGGER.warn("Failed to scan config skills directory {}", skillsDir, e);
        }

        CONFIG_SKILLS = ImmutableMap.copyOf(loaded);
    }

    public static void loadSkillFromDatapack(Map<String, SkillInstance> skills) {
        DATA_PACK_SKILLS = ImmutableMap.copyOf(skills);
    }

    private static void loadSkillFromConfig(Path path, Map<String, SkillInstance> loaded) {
        try {
            SkillInstance skill = parse(path);
            if (skill != null) {
                loaded.put(skill.name(), skill);
                LOGGER.info("Loaded skill {} from file {}", skill.name(), path);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load skill from file {}", path, e);
        }
    }

    /**
     * 读取特定目录下的 skill
     *
     * @param skillFilePath SKILL.md 文件的路径
     * @return 读取后的内容
     */
    private static SkillInstance parse(Path skillFilePath) {
        try (InputStream stream = Files.newInputStream(skillFilePath)) {
            Pair<SkillBean, String> result = SkillParser.parse(stream, skillFilePath.toString());
            if (result == null) {
                return null;
            }

            SkillBean header = result.getLeft();
            String body = result.getRight();
            Map<String, String> references = Maps.newLinkedHashMap();

            // 检查 references 目录是否存在，以及其下有无其他文件
            Path referencesPath = skillFilePath.getParent().resolve(REFERENCES);
            if (!Files.isDirectory(referencesPath)) {
                return new SkillInstance(
                        header.getName().trim(),
                        header.getDescription().trim(),
                        header.getMetadata(),
                        body, Map.of()
                );
            }

            // 读取 referencesPath 下所有的参考文件
            try (var walk = Files.walk(referencesPath, 1)) {
                walk.filter(Files::isRegularFile).forEach(refPath -> {
                    try {
                        String content = Files.readString(refPath);
                        references.put(refPath.getFileName().toString(), content);
                    } catch (IOException e) {
                        LOGGER.warn("Failed to read reference file {} for skill {}, skipping this reference. Error: {}",
                                refPath, header.getName(), e.getMessage());
                    }
                });
            } catch (IOException e) {
                LOGGER.warn("Failed to scan references directory {} for skill {}, skipping all references. Error: {}",
                        referencesPath, header.getName(), e.getMessage());
            }

            return new SkillInstance(
                    header.getName().trim(),
                    header.getDescription().trim(),
                    header.getMetadata(),
                    body, ImmutableMap.copyOf(references)
            );
        } catch (Exception e) {
            LOGGER.error("Failed to read skill file {}", skillFilePath, e);
            return null;
        }
    }

    /**
     * ⚠️ 必须与 {@link #getAllSkills()} 用**同一条**优先级：原实现让数据包胜出，
     * 而 {@code getAllSkills()} 的 {@code putAll} 顺序让配置目录胜出——同一个名字，
     * 列表里显示的和实际取到的是两个技能。让本方法直接走那张合并表，两者不可能再走散。
     */
    public static SkillInstance getSkill(String name) {
        return getAllSkills().get(name);
    }

    public static boolean isEmpty() {
        return CONFIG_SKILLS.isEmpty() && DATA_PACK_SKILLS.isEmpty();
    }

    public static Map<String, SkillInstance> getAllSkills() {
        Map<String, SkillInstance> all = Maps.newLinkedHashMap();
        all.putAll(DATA_PACK_SKILLS);
        all.putAll(CONFIG_SKILLS);
        return all;
    }

    /**
     * 学习 OpenCode，将 Skills 转换为无换行、无缩进的单行 XML 字符串（专为 LLM Prompt 优化）
     */
    public static String getSkillSummary() {
        // 边界处理
        if (isEmpty()) {
            return "No skills are currently available.";
        }

        // 数据合并，config 优先于 data pack
        Map<String, SkillInstance> skills = getAllSkills();

        StringBuilder sb = new StringBuilder("<available_skills>");
        for (var entry : skills.entrySet()) {
            // 使用 Guava 进行安全的 XML 转义
            String safeName = XmlEscapers.xmlContentEscaper().escape(entry.getKey());
            String safeDesc = XmlEscapers.xmlContentEscaper().escape(entry.getValue().description());
            // 紧凑拼接，不包含任何多余的空格或换行
            sb.append("<skill>");
            sb.append("<name>").append(safeName).append("</name>");
            sb.append("<description>").append(safeDesc).append("</description>");
            sb.append("</skill>");
        }
        sb.append("</available_skills>");
        return sb.toString();
    }

    private static Path getSkillsDir() {
        return FabricLoader.getInstance().getConfigDir()
                .resolve(TouhouLittleMaid.MOD_ID)
                .resolve("skills");
    }

    private static void createSkillsFolder(Path skillsDir) {
        try {
            if (Files.isDirectory(skillsDir)) {
                return;
            }
            Files.createDirectories(skillsDir);
        } catch (IOException e) {
            LOGGER.error("Failed to create skills directory {}", skillsDir, e);
        }
    }
}
