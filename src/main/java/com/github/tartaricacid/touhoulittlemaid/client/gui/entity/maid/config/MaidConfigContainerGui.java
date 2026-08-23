package com.github.tartaricacid.touhoulittlemaid.client.gui.entity.maid.config;

import com.github.tartaricacid.touhoulittlemaid.util.IdentifierUtil;
import com.github.tartaricacid.touhoulittlemaid.client.gui.entity.maid.AbstractMaidContainerGui;
import com.github.tartaricacid.touhoulittlemaid.client.gui.widget.button.MaidConfigButton;
import com.github.tartaricacid.touhoulittlemaid.entity.ai.combat.MaidCombatResponsePolicy;
import com.github.tartaricacid.touhoulittlemaid.entity.data.ConfigData;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.PickType;
import com.github.tartaricacid.touhoulittlemaid.init.InitDataAttachment;
import com.github.tartaricacid.touhoulittlemaid.inventory.container.config.MaidConfigContainer;
import com.github.tartaricacid.touhoulittlemaid.network.message.MaidSubConfigPackage;
import com.github.tartaricacid.touhoulittlemaid.util.GuiTools;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import org.anti_ad.mc.ipn.api.IPNButton;
import org.anti_ad.mc.ipn.api.IPNGuiHint;
import org.anti_ad.mc.ipn.api.IPNPlayerSideOnly;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

@IPNPlayerSideOnly
@IPNGuiHint(button = IPNButton.SORT, horizontalOffset = -36, bottom = -12)
@IPNGuiHint(button = IPNButton.SORT_COLUMNS, horizontalOffset = -24, bottom = -24)
@IPNGuiHint(button = IPNButton.SORT_ROWS, horizontalOffset = -12, bottom = -36)
@IPNGuiHint(button = IPNButton.SHOW_EDITOR, horizontalOffset = -5)
@IPNGuiHint(button = IPNButton.SETTINGS, horizontalOffset = -5)
public class MaidConfigContainerGui extends AbstractMaidContainerGui<MaidConfigContainer> {
    private static final Identifier ICON = IdentifierUtil.modLoc("textures/gui/maid_gui_config.png");
    private ConfigData configData;
    private final List<MaidConfigButton> configButtons = new ArrayList<>();
    private int scrollOffset;

    public MaidConfigContainerGui(MaidConfigContainer screenContainer, Inventory inv, Component titleIn) {
        super(screenContainer, inv, titleIn);
        this.configData = getMaid().getAttachedOrCreate(InitDataAttachment.CONFIG);
    }

    @Override
    public void extractContents(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        GuiTools.guiBlit(graphics, ICON, leftPos + 80, topPos + 28, 0, 0, imageWidth, imageHeight);
        super.extractContents(graphics, mouseX, mouseY, a);
    }

    private void syncConfigData(ConfigData newData) {
        this.configData = newData;
        ClientPlayNetworking.send(new MaidSubConfigPackage(this.maid.getId(), this.configData));
    }

    @Override
    protected void initAdditionWidgets() {
        int buttonLeft = leftPos + 86;
        this.configButtons.clear();

        MaidConfigButton responsePolicyButton = new MaidConfigButton(buttonLeft, nextButtonTop(),
                Component.translatable("gui.touhou_little_maid.maid_config.response_policy"),
                responsePolicyValue(this.configData.combatResponsePolicy()),
                button -> {
                    this.syncConfigData(this.configData.setCombatResponsePolicy(
                            this.configData.combatResponsePolicy().previous()));
                    button.setValue(responsePolicyValue(this.configData.combatResponsePolicy()));
                },
                button -> {
                    this.syncConfigData(this.configData.setCombatResponsePolicy(
                            this.configData.combatResponsePolicy().next()));
                    button.setValue(responsePolicyValue(this.configData.combatResponsePolicy()));
                }
        );
        responsePolicyButton.setTooltip(Tooltip.create(
                Component.translatable("gui.touhou_little_maid.maid_config.response_policy.tooltip")));
        addConfigButton(responsePolicyButton);

        addConfigButton(new MaidConfigButton(buttonLeft, nextButtonTop(),
                Component.translatable("gui.touhou_little_maid.maid_config.show_backpack"),
                Component.translatable("gui.touhou_little_maid.maid_config.value." + this.configData.isShowBackpack()),
                button -> {
                    this.syncConfigData(this.configData.setShowBackpack(!this.configData.isShowBackpack()));
                    button.setValue(Component.translatable("gui.touhou_little_maid.maid_config.value." + this.configData.isShowBackpack()));
                }
        ));

        addConfigButton(new MaidConfigButton(buttonLeft, nextButtonTop(),
                Component.translatable("gui.touhou_little_maid.maid_config.show_back_item"),
                Component.translatable("gui.touhou_little_maid.maid_config.value." + this.configData.isShowBackItem()),
                button -> {
                    this.syncConfigData(this.configData.setShowBackItem(!this.configData.isShowBackItem()));
                    button.setValue(Component.translatable("gui.touhou_little_maid.maid_config.value." + this.configData.isShowBackItem()));
                }
        ));

        addConfigButton(new MaidConfigButton(buttonLeft, nextButtonTop(),
                Component.translatable("gui.touhou_little_maid.maid_config.show_chat_bubble"),
                Component.translatable("gui.touhou_little_maid.maid_config.value." + this.configData.isChatBubbleShow()),
                button -> {
                    this.syncConfigData(this.configData.setChatBubbleShow(!this.configData.isChatBubbleShow()));
                    button.setValue(Component.translatable("gui.touhou_little_maid.maid_config.value." + this.configData.isChatBubbleShow()));
                }
        ));

        addConfigButton(new MaidConfigButton(buttonLeft, nextButtonTop(),
                Component.translatable("gui.touhou_little_maid.maid_config.sound_frequency"),
                Component.literal(Math.round(this.configData.soundFreq() * 100) + "%").withStyle(ChatFormatting.YELLOW),
                button -> {
                    this.syncConfigData(this.configData.setSoundFreq(this.configData.soundFreq() - 0.1f));
                    button.setValue(Component.literal(Math.round(this.configData.soundFreq() * 100) + "%").withStyle(ChatFormatting.YELLOW));
                },
                button -> {
                    this.syncConfigData(this.configData.setSoundFreq(this.configData.soundFreq() + 0.1f));
                    button.setValue(Component.literal(Math.round(this.configData.soundFreq() * 100) + "%").withStyle(ChatFormatting.YELLOW));
                }
        ));

        addConfigButton(new MaidConfigButton(buttonLeft, nextButtonTop(),
                Component.translatable("gui.touhou_little_maid.maid_config.pick_type"),
                Component.translatable(PickType.getTransKey(this.configData.getPickupType())).withStyle(ChatFormatting.DARK_RED),
                button -> {
                    this.syncConfigData(this.configData.setPickupType(PickType.getPreviousPickType(this.configData.getPickupType())));
                    button.setValue(Component.translatable(PickType.getTransKey(this.configData.getPickupType())).withStyle(ChatFormatting.DARK_RED));
                },
                button -> {
                    this.syncConfigData(this.configData.setPickupType(PickType.getNextPickType(this.configData.getPickupType())));
                    button.setValue(Component.translatable(PickType.getTransKey(this.configData.getPickupType())).withStyle(ChatFormatting.DARK_RED));
                }
        ));

        addConfigButton(new MaidConfigButton(buttonLeft, nextButtonTop(),
                Component.translatable("gui.touhou_little_maid.maid_config.open_door"),
                Component.translatable("gui.touhou_little_maid.maid_config.value." + this.configData.isOpenDoor()),
                button -> {
                    this.syncConfigData(this.configData.setOpenDoor(!this.configData.isOpenDoor()));
                    button.setValue(Component.translatable("gui.touhou_little_maid.maid_config.value." + this.configData.isOpenDoor()));
                }
        ));

        addConfigButton(new MaidConfigButton(buttonLeft, nextButtonTop(),
                Component.translatable("gui.touhou_little_maid.maid_config.open_fence_gate"),
                Component.translatable("gui.touhou_little_maid.maid_config.value." + this.configData.isOpenFenceGate()),
                button -> {
                    this.syncConfigData(this.configData.setOpenFenceGate(!this.configData.isOpenFenceGate()));
                    button.setValue(Component.translatable("gui.touhou_little_maid.maid_config.value." + this.configData.isOpenFenceGate()));
                }
        ));

        addConfigButton(new MaidConfigButton(buttonLeft, nextButtonTop(),
                Component.translatable("gui.touhou_little_maid.maid_config.active_climbing"),
                Component.translatable("gui.touhou_little_maid.maid_config.value." + this.configData.isActiveClimbing()),
                button -> {
                    this.syncConfigData(this.configData.setActiveClimbing(!this.configData.isActiveClimbing()));
                    button.setValue(Component.translatable("gui.touhou_little_maid.maid_config.value." + this.configData.isActiveClimbing()));
                }
        ));

        MaidConfigButton allowTableFoodButton = new MaidConfigButton(buttonLeft, nextButtonTop(),
                Component.translatable("gui.touhou_little_maid.maid_config.allow_table_food"),
                Component.translatable("gui.touhou_little_maid.maid_config.value." + this.configData.isTableFoodAllowed()),
                button -> {
                    this.syncConfigData(this.configData.setTableFoodAllowed(!this.configData.isTableFoodAllowed()));
                    button.setValue(Component.translatable("gui.touhou_little_maid.maid_config.value." + this.configData.isTableFoodAllowed()));
                }
        );
        allowTableFoodButton.setTooltip(Tooltip.create(
                Component.translatable("gui.touhou_little_maid.maid_config.allow_table_food.tooltip")));
        addConfigButton(allowTableFoodButton);
        refreshVisibleButtons();
    }

    private static Component responsePolicyValue(MaidCombatResponsePolicy policy) {
        return Component.translatable(
                "gui.touhou_little_maid.maid_config.response_policy.value." + policy.serializedName())
                .withStyle(ChatFormatting.DARK_RED);
    }

    private int nextButtonTop() {
        return topPos + MaidConfigLayout.buttonY(this.configButtons.size());
    }

    private void addConfigButton(MaidConfigButton button) {
        this.configButtons.add(button);
        this.addRenderableWidget(button);
    }

    private void refreshVisibleButtons() {
        this.scrollOffset = MaidConfigLayout.clampScrollOffset(this.scrollOffset, this.configButtons.size());
        for (int index = 0; index < this.configButtons.size(); index++) {
            MaidConfigButton button = this.configButtons.get(index);
            boolean visible = MaidConfigLayout.isVisible(index, this.scrollOffset);
            button.visible = visible;
            if (visible) {
                button.setY(topPos + MaidConfigLayout.visibleButtonY(index, this.scrollOffset));
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (MaidConfigLayout.containsPanel(leftPos, topPos, mouseX, mouseY)
                && this.configButtons.size() > MaidConfigLayout.VISIBLE_ROWS
                && scrollY != 0) {
            int nextOffset = MaidConfigLayout.scrolledOffset(
                    this.scrollOffset, this.configButtons.size(), scrollY);
            if (nextOffset != this.scrollOffset) {
                this.scrollOffset = nextOffset;
                refreshVisibleButtons();
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }
}
