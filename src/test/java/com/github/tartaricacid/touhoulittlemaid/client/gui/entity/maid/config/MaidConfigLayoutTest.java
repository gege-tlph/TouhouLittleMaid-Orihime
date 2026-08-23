package com.github.tartaricacid.touhoulittlemaid.client.gui.entity.maid.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MaidConfigLayoutTest {
    @Test
    void tenRowsFitThePanelWithoutOverlappingTheInventory() {
        assertTrue(MaidConfigLayout.allButtonsFitPanel());
        assertTrue(MaidConfigLayout.buttonY(0) >= MaidConfigLayout.PANEL_TOP);

        int lastBottom = MaidConfigLayout.buttonY(MaidConfigLayout.VISIBLE_ROWS - 1)
                + MaidConfigLayout.BUTTON_HEIGHT;
        assertTrue(lastBottom <= MaidConfigLayout.PANEL_BOTTOM);
    }

    @Test
    void adjacentRowsNeitherOverlapNorLeaveAnAccidentalGap() {
        for (int row = 1; row < MaidConfigLayout.VISIBLE_ROWS; row++) {
            assertEquals(
                    MaidConfigLayout.buttonY(row - 1) + MaidConfigLayout.BUTTON_HEIGHT,
                    MaidConfigLayout.buttonY(row));
        }
    }

    @Test
    void wheelScrollingRevealsOverflowRowsAndClampsAtBothEnds() {
        int buttonCount = MaidConfigLayout.VISIBLE_ROWS + 2;

        assertEquals(0, MaidConfigLayout.maxScrollOffset(MaidConfigLayout.VISIBLE_ROWS));
        assertEquals(2, MaidConfigLayout.maxScrollOffset(buttonCount));
        assertEquals(1, MaidConfigLayout.scrolledOffset(0, buttonCount, -1));
        assertEquals(2, MaidConfigLayout.scrolledOffset(2, buttonCount, -1));
        assertEquals(1, MaidConfigLayout.scrolledOffset(2, buttonCount, 1));
        assertEquals(0, MaidConfigLayout.scrolledOffset(0, buttonCount, 1));

        assertTrue(MaidConfigLayout.isVisible(0, 0));
        assertTrue(MaidConfigLayout.isVisible(buttonCount - 1, 2));
        assertEquals(MaidConfigLayout.buttonY(MaidConfigLayout.VISIBLE_ROWS - 1),
                MaidConfigLayout.visibleButtonY(buttonCount - 1, 2));
    }

    @Test
    void wheelInputIsLimitedToTheConfigPanel() {
        assertTrue(MaidConfigLayout.containsPanel(100, 200, 180, 228));
        assertTrue(MaidConfigLayout.containsPanel(100, 200, 355, 363));
        assertEquals(false, MaidConfigLayout.containsPanel(100, 200, 179, 228));
        assertEquals(false, MaidConfigLayout.containsPanel(100, 200, 180, 364));
    }
}
