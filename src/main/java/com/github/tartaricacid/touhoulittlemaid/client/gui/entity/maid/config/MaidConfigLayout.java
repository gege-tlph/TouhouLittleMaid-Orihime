package com.github.tartaricacid.touhoulittlemaid.client.gui.entity.maid.config;

final class MaidConfigLayout {
    static final int PANEL_LEFT = 80;
    static final int PANEL_RIGHT = 256;
    static final int PANEL_TOP = 28;
    static final int PANEL_BOTTOM = 164;
    static final int BUTTON_START_Y = 33;
    static final int BUTTON_HEIGHT = 13;
    static final int BUTTON_STEP = 13;
    static final int VISIBLE_ROWS = 10;

    private MaidConfigLayout() {
    }

    static int buttonY(int row) {
        return BUTTON_START_Y + row * BUTTON_STEP;
    }

    static boolean allButtonsFitPanel() {
        int lastBottom = buttonY(VISIBLE_ROWS - 1) + BUTTON_HEIGHT;
        return BUTTON_START_Y >= PANEL_TOP && lastBottom <= PANEL_BOTTOM;
    }

    static int maxScrollOffset(int buttonCount) {
        return Math.max(0, buttonCount - VISIBLE_ROWS);
    }

    static int clampScrollOffset(int scrollOffset, int buttonCount) {
        return Math.clamp(scrollOffset, 0, maxScrollOffset(buttonCount));
    }

    static int scrolledOffset(int scrollOffset, int buttonCount, double scrollY) {
        int direction = scrollY < 0 ? 1 : -1;
        return clampScrollOffset(scrollOffset + direction, buttonCount);
    }

    static boolean isVisible(int buttonIndex, int scrollOffset) {
        return buttonIndex >= scrollOffset && buttonIndex < scrollOffset + VISIBLE_ROWS;
    }

    static int visibleButtonY(int buttonIndex, int scrollOffset) {
        return buttonY(buttonIndex - scrollOffset);
    }

    static boolean containsPanel(int leftPos, int topPos, double mouseX, double mouseY) {
        return mouseX >= leftPos + PANEL_LEFT && mouseX < leftPos + PANEL_RIGHT
                && mouseY >= topPos + PANEL_TOP && mouseY < topPos + PANEL_BOTTOM;
    }
}
