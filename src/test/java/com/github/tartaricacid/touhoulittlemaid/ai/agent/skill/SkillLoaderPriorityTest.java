package com.github.tartaricacid.touhoulittlemaid.ai.agent.skill;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillLoaderPriorityTest {
    private static final String SKILL_NAME = "same-name-skill";

    @TempDir
    Path temporaryDirectory;

    @AfterEach
    void resetSkillSnapshots() throws Exception {
        SkillLoader.loadSkillFromDatapack(Map.of());
        Path emptyDirectory = Files.createDirectories(temporaryDirectory.resolve("empty"));
        SkillLoader.reloadFromConfig(emptyDirectory);
    }

    @Test
    void configSkillWinsForSummarySelectionAndReload() throws Exception {
        SkillInstance datapackSkill = new SkillInstance(
                SKILL_NAME,
                "datapack description",
                Map.of(),
                "datapack body",
                Map.of("en_us.md", "datapack reference")
        );
        SkillLoader.loadSkillFromDatapack(Map.of(SKILL_NAME, datapackSkill));

        Path skillDirectory = Files.createDirectories(temporaryDirectory.resolve(SKILL_NAME));
        Path skillFile = skillDirectory.resolve("skill.md");
        Path referenceFile = Files.createDirectories(skillDirectory.resolve("references")).resolve("en_us.md");
        writeSkill(skillFile, "config description v1", "config body v1");
        Files.writeString(referenceFile, "config reference v1", StandardCharsets.UTF_8);

        SkillLoader.reloadFromConfig(temporaryDirectory);
        assertSelectedConfigSkill("config description v1", "config body v1", "config reference v1");
        assertTrue(SkillLoader.getSkillSummary().contains("config description v1"));
        assertFalse(SkillLoader.getSkillSummary().contains("datapack description"));

        writeSkill(skillFile, "config description v2", "config body v2");
        Files.writeString(referenceFile, "config reference v2", StandardCharsets.UTF_8);
        SkillLoader.reloadFromConfig(temporaryDirectory);

        assertSelectedConfigSkill("config description v2", "config body v2", "config reference v2");
        assertTrue(SkillLoader.getSkillSummary().contains("config description v2"));
        assertFalse(SkillLoader.getSkillSummary().contains("config description v1"));

        Files.delete(skillFile);
        SkillLoader.reloadFromConfig(temporaryDirectory);

        assertSame(datapackSkill, SkillLoader.getSkill(SKILL_NAME));
        assertSame(datapackSkill, SkillLoader.getAllSkills().get(SKILL_NAME));
        assertTrue(SkillLoader.getSkillSummary().contains("datapack description"));
        assertFalse(SkillLoader.getSkillSummary().contains("config description v2"));
    }

    private static void writeSkill(Path path, String description, String body) throws Exception {
        String content = """
                ---
                name: %s
                description: %s
                ---

                %s
                """.formatted(SKILL_NAME, description, body);
        Files.writeString(path, content, StandardCharsets.UTF_8);
    }

    private static void assertSelectedConfigSkill(String description, String body, String reference) {
        SkillInstance selected = SkillLoader.getSkill(SKILL_NAME);
        assertSame(selected, SkillLoader.getAllSkills().get(SKILL_NAME));
        assertEquals(description, selected.description());
        assertEquals(body, selected.body());
        assertEquals(reference, selected.references().get("en_us.md"));
    }
}
