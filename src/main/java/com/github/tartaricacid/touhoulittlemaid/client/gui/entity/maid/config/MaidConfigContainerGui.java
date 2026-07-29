package com.github.tartaricacid.touhoulittlemaid.client.gui.entity.maid.config;

import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.github.tartaricacid.touhoulittlemaid.client.gui.entity.maid.AbstractMaidContainerGui;
import com.github.tartaricacid.touhoulittlemaid.client.gui.widget.button.MaidConfigButton;
import com.github.tartaricacid.touhoulittlemaid.entity.ai.combat.MaidCombatResponsePolicy;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.MaidConfigManager;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.PickType;
import com.github.tartaricacid.touhoulittlemaid.inventory.container.config.MaidConfigContainer;
import com.github.tartaricacid.touhoulittlemaid.network.message.MaidSubConfigPackage;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import org.anti_ad.mc.ipn.api.IPNButton;
import org.anti_ad.mc.ipn.api.IPNGuiHint;
import org.anti_ad.mc.ipn.api.IPNPlayerSideOnly;

import java.util.ArrayList;
import java.util.List;

@IPNPlayerSideOnly
@IPNGuiHint(button = IPNButton.SORT, horizontalOffset = -36, bottom = -12)
@IPNGuiHint(button = IPNButton.SORT_COLUMNS, horizontalOffset = -24, bottom = -24)
@IPNGuiHint(button = IPNButton.SORT_ROWS, horizontalOffset = -12, bottom = -36)
@IPNGuiHint(button = IPNButton.SHOW_EDITOR, horizontalOffset = -5)
@IPNGuiHint(button = IPNButton.SETTINGS, horizontalOffset = -5)
public class MaidConfigContainerGui extends AbstractMaidContainerGui<MaidConfigContainer> {
    private static final Identifier ICON = Identifier.fromNamespaceAndPath(TouhouLittleMaid.MOD_ID, "textures/gui/maid_gui_config.png");
    private final MaidConfigManager.SyncNetwork syncNetwork;
    private final List<MaidConfigButton> configButtons = new ArrayList<>();
    private int scrollOffset;

    public MaidConfigContainerGui(MaidConfigContainer screenContainer, Inventory inv, Component titleIn) {
        super(screenContainer, inv, titleIn);
        this.syncNetwork = getMaid().getConfigManager().getSyncNetwork();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTicks, int x, int y) {
        super.renderBg(graphics, partialTicks, x, y);
        // 1.21.11: blit 增 RenderPipeline 首参 + 显式贴图尺寸（旧 7 参隐含 256x256）
        graphics.blit(RenderPipelines.GUI_TEXTURED, ICON, leftPos + 80, topPos + 28, 0F, 0F, imageWidth, imageHeight, 256, 256);
    }

    @Override
    protected void initAdditionWidgets() {
        int buttonLeft = leftPos + 86;
        this.configButtons.clear();

        MaidConfigButton responsePolicyButton = new MaidConfigButton(buttonLeft, nextButtonTop(),
                Component.translatable("gui.touhou_little_maid.maid_config.response_policy"),
                responsePolicyValue(this.syncNetwork.combatResponsePolicy()),
                button -> {
                    this.syncNetwork.setCombatResponsePolicy(
                            this.syncNetwork.combatResponsePolicy().previous());
                    button.setValue(responsePolicyValue(this.syncNetwork.combatResponsePolicy()));
                    ClientPlayNetworking.send(new MaidSubConfigPackage(this.maid.getId(), this.syncNetwork));
                },
                button -> {
                    this.syncNetwork.setCombatResponsePolicy(
                            this.syncNetwork.combatResponsePolicy().next());
                    button.setValue(responsePolicyValue(this.syncNetwork.combatResponsePolicy()));
                    ClientPlayNetworking.send(new MaidSubConfigPackage(this.maid.getId(), this.syncNetwork));
                }
        );
        responsePolicyButton.setTooltip(Tooltip.create(
                Component.translatable("gui.touhou_little_maid.maid_config.response_policy.tooltip")));
        addConfigButton(responsePolicyButton);

        addConfigButton(new MaidConfigButton(buttonLeft, nextButtonTop(),
                Component.translatable("gui.touhou_little_maid.maid_config.show_backpack"),
                Component.translatable("gui.touhou_little_maid.maid_config.value." + this.syncNetwork.showBackpack()),
                button -> {
                    this.syncNetwork.setShowBackpack(!this.syncNetwork.showBackpack());
                    button.setValue(Component.translatable("gui.touhou_little_maid.maid_config.value." + this.syncNetwork.showBackpack()));
                    ClientPlayNetworking.send(new MaidSubConfigPackage(this.maid.getId(), this.syncNetwork));
                }
        ));

        addConfigButton(new MaidConfigButton(buttonLeft, nextButtonTop(),
                Component.translatable("gui.touhou_little_maid.maid_config.show_back_item"),
                Component.translatable("gui.touhou_little_maid.maid_config.value." + this.syncNetwork.showBackItem()),
                button -> {
                    this.syncNetwork.setShowBackItem(!this.syncNetwork.showBackItem());
                    button.setValue(Component.translatable("gui.touhou_little_maid.maid_config.value." + this.syncNetwork.showBackItem()));
                    ClientPlayNetworking.send(new MaidSubConfigPackage(this.maid.getId(), this.syncNetwork));
                }
        ));

        addConfigButton(new MaidConfigButton(buttonLeft, nextButtonTop(),
                Component.translatable("gui.touhou_little_maid.maid_config.show_chat_bubble"),
                Component.translatable("gui.touhou_little_maid.maid_config.value." + this.syncNetwork.showChatBubble()),
                button -> {
                    this.syncNetwork.setShowChatBubble(!this.syncNetwork.showChatBubble());
                    button.setValue(Component.translatable("gui.touhou_little_maid.maid_config.value." + this.syncNetwork.showChatBubble()));
                    ClientPlayNetworking.send(new MaidSubConfigPackage(this.maid.getId(), this.syncNetwork));
                }
        ));

        addConfigButton(new MaidConfigButton(buttonLeft, nextButtonTop(),
                Component.translatable("gui.touhou_little_maid.maid_config.sound_frequency"),
                Component.literal(Math.round(this.syncNetwork.soundFreq() * 100) + "%").withStyle(ChatFormatting.YELLOW),
                button -> {
                    this.syncNetwork.setSoundFreq(this.syncNetwork.soundFreq() - 0.1f);
                    button.setValue(Component.literal(Math.round(this.syncNetwork.soundFreq() * 100) + "%").withStyle(ChatFormatting.YELLOW));
                    ClientPlayNetworking.send(new MaidSubConfigPackage(this.maid.getId(), this.syncNetwork));
                },
                button -> {
                    this.syncNetwork.setSoundFreq(this.syncNetwork.soundFreq() + 0.1f);
                    button.setValue(Component.literal(Math.round(this.syncNetwork.soundFreq() * 100) + "%").withStyle(ChatFormatting.YELLOW));
                    ClientPlayNetworking.send(new MaidSubConfigPackage(this.maid.getId(), this.syncNetwork));
                }
        ));

        addConfigButton(new MaidConfigButton(buttonLeft, nextButtonTop(),
                Component.translatable("gui.touhou_little_maid.maid_config.pick_type"),
                Component.translatable(PickType.getTransKey(this.syncNetwork.pickType())).withStyle(ChatFormatting.DARK_RED),
                button -> {
                    this.syncNetwork.setPickType(PickType.getPreviousPickType(this.syncNetwork.pickType()));
                    button.setValue(Component.translatable(PickType.getTransKey(this.syncNetwork.pickType())).withStyle(ChatFormatting.DARK_RED));
                    ClientPlayNetworking.send(new MaidSubConfigPackage(this.maid.getId(), this.syncNetwork));
                },
                button -> {
                    this.syncNetwork.setPickType(PickType.getNextPickType(this.syncNetwork.pickType()));
                    button.setValue(Component.translatable(PickType.getTransKey(this.syncNetwork.pickType())).withStyle(ChatFormatting.DARK_RED));
                    ClientPlayNetworking.send(new MaidSubConfigPackage(this.maid.getId(), this.syncNetwork));
                }
        ));

        addConfigButton(new MaidConfigButton(buttonLeft, nextButtonTop(),
                Component.translatable("gui.touhou_little_maid.maid_config.open_door"),
                Component.translatable("gui.touhou_little_maid.maid_config.value." + this.syncNetwork.openDoor()),
                button -> {
                    this.syncNetwork.setOpenDoor(!this.syncNetwork.openDoor());
                    button.setValue(Component.translatable("gui.touhou_little_maid.maid_config.value." + this.syncNetwork.openDoor()));
                    ClientPlayNetworking.send(new MaidSubConfigPackage(this.maid.getId(), this.syncNetwork));
                }
        ));

        addConfigButton(new MaidConfigButton(buttonLeft, nextButtonTop(),
                Component.translatable("gui.touhou_little_maid.maid_config.open_fence_gate"),
                Component.translatable("gui.touhou_little_maid.maid_config.value." + this.syncNetwork.openFenceGate()),
                button -> {
                    this.syncNetwork.setOpenFenceGate(!this.syncNetwork.openFenceGate());
                    button.setValue(Component.translatable("gui.touhou_little_maid.maid_config.value." + this.syncNetwork.openFenceGate()));
                    ClientPlayNetworking.send(new MaidSubConfigPackage(this.maid.getId(), this.syncNetwork));
                }
        ));

        addConfigButton(new MaidConfigButton(buttonLeft, nextButtonTop(),
                Component.translatable("gui.touhou_little_maid.maid_config.active_climbing"),
                Component.translatable("gui.touhou_little_maid.maid_config.value." + this.syncNetwork.activeClimbing()),
                button -> {
                    this.syncNetwork.setActiveClimbing(!this.syncNetwork.activeClimbing());
                    button.setValue(Component.translatable("gui.touhou_little_maid.maid_config.value." + this.syncNetwork.activeClimbing()));
                    ClientPlayNetworking.send(new MaidSubConfigPackage(this.maid.getId(), this.syncNetwork));
                }
        ));

        MaidConfigButton allowTableFoodButton = new MaidConfigButton(buttonLeft, nextButtonTop(),
                Component.translatable("gui.touhou_little_maid.maid_config.allow_table_food"),
                Component.translatable("gui.touhou_little_maid.maid_config.value." + this.syncNetwork.allowTableFood()),
                button -> {
                    this.syncNetwork.setAllowTableFood(!this.syncNetwork.allowTableFood());
                    button.setValue(Component.translatable("gui.touhou_little_maid.maid_config.value." + this.syncNetwork.allowTableFood()));
                    ClientPlayNetworking.send(new MaidSubConfigPackage(this.maid.getId(), this.syncNetwork));
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
