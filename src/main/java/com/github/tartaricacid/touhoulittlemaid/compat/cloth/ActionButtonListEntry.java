package com.github.tartaricacid.touhoulittlemaid.compat.cloth;

import me.shedaniel.clothconfig2.gui.entries.TooltipListEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Optional;

/** A side-effect-only button row for navigation actions in the Cloth screen. */
final class ActionButtonListEntry extends TooltipListEntry<Void> {
    private final Button button;

    ActionButtonListEntry(Component fieldName, Component buttonText, Runnable action) {
        super(fieldName, Optional::empty);
        this.button = Button.builder(buttonText, ignored -> action.run()).bounds(0, 0, 150, 20).build();
    }

    ActionButtonListEntry(Component fieldName, Component buttonText, Component tooltip, Runnable action) {
        super(fieldName, () -> Optional.of(new Component[]{tooltip}));
        this.button = Button.builder(buttonText, ignored -> action.run()).bounds(0, 0, 150, 20).build();
    }

    @Override
    public boolean isEdited() {
        return false;
    }

    @Override
    public Void getValue() {
        return null;
    }

    @Override
    public Optional<Void> getDefaultValue() {
        return Optional.empty();
    }

    @Override
    public void save() {
    }

    @Override
    public List<? extends GuiEventListener> children() {
        return List.of(this.button);
    }

    @Override
    public List<? extends NarratableEntry> narratables() {
        return List.of(this.button);
    }

    @Override
    public void render(GuiGraphics graphics, int index, int y, int x, int entryWidth, int entryHeight,
                       int mouseX, int mouseY, boolean hovered, float partialTick) {
        super.render(graphics, index, y, x, entryWidth, entryHeight, mouseX, mouseY, hovered, partialTick);
        this.button.setX(x + entryWidth - this.button.getWidth());
        this.button.setY(y);
        graphics.drawString(Minecraft.getInstance().font, this.getDisplayedFieldName(), x, y + 6,
                this.getPreferredTextColor(), false);
        this.button.render(graphics, mouseX, mouseY, partialTick);
    }
}
