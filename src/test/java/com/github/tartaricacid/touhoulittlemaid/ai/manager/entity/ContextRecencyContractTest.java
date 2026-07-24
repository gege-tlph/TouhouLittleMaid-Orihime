package com.github.tartaricacid.touhoulittlemaid.ai.manager.entity;

import com.github.tartaricacid.touhoulittlemaid.ai.agent.tool.implement.QueryGameContextTool;
import com.github.tartaricacid.touhoulittlemaid.ai.manager.setting.papi.StringConstant;
import com.github.tartaricacid.touhoulittlemaid.ai.service.llm.LLMMessage;
import com.github.tartaricacid.touhoulittlemaid.ai.service.llm.Role;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContextRecencyContractTest {
    @Test
    void historicalUserContextIsRemovedBeforeSummarization() {
        String message = """
                <context>
                - scheduled_activity: work
                - active_activity: panic
                </context>
                Please come back
                """;
        LLMMessage historical = new LLMMessage(Role.USER, message, 10L);

        assertEquals("Please come back", UserPromptContexts.removeContext(message));
        assertEquals("[USER] Please come back", HistorySummaryPrompts.buildSummaryEntry(historical));
        assertEquals("[USER] Please come back",
                com.github.tartaricacid.touhoulittlemaid.ai.manager.entity.summary.HistorySummaryPrompts
                        .buildSummaryEntry(historical));
    }

    @Test
    void promptAndQueryToolDeclareLatestLiveContextAsAuthority() {
        String prompt = StringConstant.FULL_SETTING;
        String querySummary = new QueryGameContextTool().summary(null);

        assertTrue(prompt.contains("Use ONLY the one in the **latest** user message as the ground truth"));
        assertTrue(prompt.contains("The newest tool result is the authoritative dynamic state"));
        assertTrue(querySummary.contains("authoritative live game context"));
        assertFalse(querySummary.contains("history"));
    }
}
