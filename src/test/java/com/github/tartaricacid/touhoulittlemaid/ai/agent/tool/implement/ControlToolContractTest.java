package com.github.tartaricacid.touhoulittlemaid.ai.agent.tool.implement;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ControlToolContractTest {
    @Test
    void toolDescriptionsDistinguishPersistentCommandsFromEmergencyState() {
        String work = new SwitchWorkTaskTool().summary(null).toLowerCase();
        String follow = new SwitchFollowStateTool().summary(null).toLowerCase();
        String sit = new SwitchSitTool().summary(null).toLowerCase();
        String schedule = new SwitchScheduleTool().summary(null).toLowerCase();

        assertAll(
                () -> assertTrue(work.contains("permanent") && work.contains("emergency")),
                () -> assertTrue(work.contains("latest nearby-entity context")),
                () -> assertTrue(work.contains("rejected target leaves the work task unchanged")),
                () -> assertTrue(follow.contains("persistent") && follow.contains("emergency")),
                () -> assertTrue(sit.contains("persistent") && sit.contains("emergency")),
                () -> assertTrue(schedule.contains("persistent") && schedule.contains("temporary emergency"))
        );
    }

    @Test
    void invalidScheduleCannotCrashThePlayerVisibleInvocationSummary() {
        assertTrue(new SwitchScheduleTool().invocationSummaryComponent("INVALID").getString().isEmpty());
    }
}
