package com.github.tartaricacid.touhoulittlemaid.ai.agent.tool.implement;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class UseSkillToolContractTest {
    @Test
    void knowledgeResultUsesItsActualSourceInsteadOfClaimingWiki() {
        String result = UseSkillTool.formatKnowledgeResult("maid fact");

        assertEquals("Source: loaded gameplay knowledge skill\nSummary: maid fact", result);
        assertFalse(result.contains("Minecraft Wiki"));
    }
}
