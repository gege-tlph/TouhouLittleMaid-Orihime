package com.github.tartaricacid.touhoulittlemaid.entity.passive;

import com.github.tartaricacid.touhoulittlemaid.entity.ai.combat.MaidCombatResponsePolicy;
import com.github.tartaricacid.touhoulittlemaid.init.InitEntities;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import static com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid.*;

public class MaidConfigManager {
    private static final String PICKUP_TAG = "MaidIsPickup";
    private static final String HOME_TAG = "MaidIsHome";
    private static final String RIDEABLE_TAG = "MaidIsRideable";

    private static final String MAID_SUB_CONFIG_TAG = "MaidSubConfig";
    private static final String BACKPACK_SHOW_TAG = "BackpackShow";
    private static final String BACK_ITEM_SHOW_TAG = "BackItemShow";
    private static final String CHATBUBBLE_SHOW_TAG = "ChatBubbleShow";
    private static final String SOUND_FREQ_TAG = "SoundFreq";
    private static final String PICKUP_TYPE_TAG = "PickupType";
    private static final String OPEN_DOOR_TAG = "OpenDoor";
    private static final String OPEN_FENCE_GATE_TAG = "OpenFenceGate";
    private static final String ACTIVE_CLIMBING_TAG = "ActiveClimbing";
    private static final String ALLOW_TABLE_FOOD_TAG = "AllowTableFood";
    private static final String COMBAT_RESPONSE_POLICY_TAG = "CombatResponsePolicy";

    private final SynchedEntityData entityData;

    MaidConfigManager(SynchedEntityData entityData) {
        this.entityData = entityData;
    }

    void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_PICKUP, true);
        builder.define(DATA_HOME_MODE, false);
        builder.define(DATA_RIDEABLE, true);

        builder.define(BACKPACK_SHOW, true);
        builder.define(BACK_ITEM_SHOW, true);
        builder.define(CHATBUBBLE_SHOW, true);
        builder.define(SOUND_FREQ, 1.0f);
        builder.define(PICKUP_TYPE, PickType.ALL.ordinal());
        builder.define(OPEN_DOOR, true);
        builder.define(OPEN_FENCE_GATE, true);
        builder.define(ACTIVE_CLIMBING, true);
        builder.define(ALLOW_TABLE_FOOD, true);
        builder.define(COMBAT_RESPONSE_POLICY, MaidCombatResponsePolicy.PROTECT_OWNER.ordinal());
    }

    void addAdditionalSaveData(ValueOutput output) {
        output.putBoolean(PICKUP_TAG, isPickup());
        output.putBoolean(HOME_TAG, isHomeModeEnable());
        output.putBoolean(RIDEABLE_TAG, isRideable());

        ValueOutput maidSubConfig = output.child(MAID_SUB_CONFIG_TAG);
        maidSubConfig.putBoolean(BACKPACK_SHOW_TAG, isShowBackpack());
        maidSubConfig.putBoolean(BACK_ITEM_SHOW_TAG, isShowBackItem());
        maidSubConfig.putBoolean(CHATBUBBLE_SHOW_TAG, isChatBubbleShow());
        maidSubConfig.putFloat(SOUND_FREQ_TAG, getSoundFreq());
        maidSubConfig.putInt(PICKUP_TYPE_TAG, getPickupType().ordinal());
        maidSubConfig.putBoolean(OPEN_DOOR_TAG, isOpenDoor());
        maidSubConfig.putBoolean(OPEN_FENCE_GATE_TAG, isOpenFenceGate());
        maidSubConfig.putBoolean(ACTIVE_CLIMBING_TAG, isActiveClimbing());
        maidSubConfig.putBoolean(ALLOW_TABLE_FOOD_TAG, isTableFoodAllowed());
        maidSubConfig.putString(COMBAT_RESPONSE_POLICY_TAG, getCombatResponsePolicy().serializedName());
    }

    void readAdditionalSaveData(ValueInput input) {
        setPickup(input.getBooleanOr(PICKUP_TAG, false));
        setHomeModeEnable(input.getBooleanOr(HOME_TAG, false));
        setRideable(input.getBooleanOr(RIDEABLE_TAG, false));
        input.child(MAID_SUB_CONFIG_TAG).ifPresent(maidSubConfig -> {
            setShowBackpack(maidSubConfig.getBooleanOr(BACKPACK_SHOW_TAG, false));
            setShowBackItem(maidSubConfig.getBooleanOr(BACK_ITEM_SHOW_TAG, false));
            setChatBubbleShow(maidSubConfig.getBooleanOr(CHATBUBBLE_SHOW_TAG, false));
            setSoundFreq(maidSubConfig.getFloatOr(SOUND_FREQ_TAG, 0f));
            setPickupType(PickType.values()[maidSubConfig.getIntOr(PICKUP_TYPE_TAG, 0)]);
            setOpenDoor(maidSubConfig.getBooleanOr(OPEN_DOOR_TAG, false));
            setOpenFenceGate(maidSubConfig.getBooleanOr(OPEN_FENCE_GATE_TAG, false));
            setActiveClimbing(maidSubConfig.getBooleanOr(ACTIVE_CLIMBING_TAG, false));
            // This setting did not exist in older saves. Preserve the original
            // always-enabled table-food behavior unless the player opts out.
            setTableFoodAllowed(maidSubConfig.getBooleanOr(ALLOW_TABLE_FOOD_TAG, true));
            setCombatResponsePolicy(MaidCombatResponsePolicy.fromSerializedName(
                    maidSubConfig.getStringOr(COMBAT_RESPONSE_POLICY_TAG,
                            MaidCombatResponsePolicy.PROTECT_OWNER.serializedName())));
        });
    }

    boolean isHomeModeEnable() {
        return this.entityData.get(DATA_HOME_MODE);
    }

    void setHomeModeEnable(boolean enable) {
        this.entityData.set(DATA_HOME_MODE, enable);
    }

    boolean isPickup() {
        return this.entityData.get(DATA_PICKUP);
    }

    void setPickup(boolean isPickup) {
        this.entityData.set(DATA_PICKUP, isPickup);
    }

    boolean isRideable() {
        return this.entityData.get(DATA_RIDEABLE);
    }

    void setRideable(boolean rideable) {
        this.entityData.set(DATA_RIDEABLE, rideable);
    }

    public SyncNetwork getSyncNetwork() {
        return new SyncNetwork(
                this.isShowBackpack(),
                this.isShowBackItem(),
                this.isChatBubbleShow(),
                this.getSoundFreq(),
                this.getPickupType(),
                this.isOpenDoor(),
                this.isOpenFenceGate(),
                this.isActiveClimbing(),
                this.isTableFoodAllowed(),
                this.getCombatResponsePolicy()
        );
    }

    public boolean isShowBackpack() {
        return this.entityData.get(BACKPACK_SHOW);
    }

    public void setShowBackpack(boolean show) {
        this.entityData.set(BACKPACK_SHOW, show);
    }

    public boolean isShowBackItem() {
        return this.entityData.get(BACK_ITEM_SHOW);
    }

    public void setShowBackItem(boolean show) {
        this.entityData.set(BACK_ITEM_SHOW, show);
    }

    public boolean isChatBubbleShow() {
        return this.entityData.get(CHATBUBBLE_SHOW);
    }

    public void setChatBubbleShow(boolean show) {
        this.entityData.set(CHATBUBBLE_SHOW, show);
    }

    public float getSoundFreq() {
        return this.entityData.get(SOUND_FREQ);
    }

    public void setSoundFreq(float freq) {
        this.entityData.set(SOUND_FREQ, Mth.clamp(freq, 0f, 1f));
    }

    public PickType getPickupType() {
        int index = this.entityData.get(PICKUP_TYPE);
        return PickType.values()[index];
    }

    public void setPickupType(PickType type) {
        this.entityData.set(PICKUP_TYPE, type.ordinal());
    }

    public boolean isOpenDoor() {
        return this.entityData.get(OPEN_DOOR);
    }

    public void setOpenDoor(boolean openDoor) {
        this.entityData.set(OPEN_DOOR, openDoor);
    }

    public boolean isOpenFenceGate() {
        return this.entityData.get(OPEN_FENCE_GATE);
    }

    public void setOpenFenceGate(boolean openFenceGate) {
        this.entityData.set(OPEN_FENCE_GATE, openFenceGate);
    }

    public boolean isActiveClimbing() {
        return this.entityData.get(ACTIVE_CLIMBING);
    }

    public void setActiveClimbing(boolean activeClimbing) {
        this.entityData.set(ACTIVE_CLIMBING, activeClimbing);
    }

    public boolean isTableFoodAllowed() {
        return this.entityData.get(ALLOW_TABLE_FOOD);
    }

    public void setTableFoodAllowed(boolean allowed) {
        this.entityData.set(ALLOW_TABLE_FOOD, allowed);
    }

    public MaidCombatResponsePolicy getCombatResponsePolicy() {
        return MaidCombatResponsePolicy.fromOrdinal(this.entityData.get(COMBAT_RESPONSE_POLICY));
    }

    public void setCombatResponsePolicy(MaidCombatResponsePolicy policy) {
        this.entityData.set(COMBAT_RESPONSE_POLICY, policy.ordinal());
    }

    public static final class SyncNetwork {
        private boolean showBackpack;
        private boolean showBackItem;
        private boolean showChatBubble;
        private float soundFreq;
        private PickType pickType;
        private boolean openDoor;
        private boolean openFenceGate;
        private boolean activeClimbing;
        private boolean allowTableFood;
        private MaidCombatResponsePolicy combatResponsePolicy;

        public SyncNetwork(boolean showBackpack, boolean showBackItem, boolean showChatBubble, float soundFreq,
                           PickType pickType, boolean openDoor, boolean openFenceGate, boolean activeClimbing,
                           boolean allowTableFood, MaidCombatResponsePolicy combatResponsePolicy) {
            this.showBackpack = showBackpack;
            this.showBackItem = showBackItem;
            this.showChatBubble = showChatBubble;
            this.soundFreq = soundFreq;
            this.pickType = pickType;
            this.openDoor = openDoor;
            this.openFenceGate = openFenceGate;
            this.activeClimbing = activeClimbing;
            this.allowTableFood = allowTableFood;
            this.combatResponsePolicy = combatResponsePolicy;
        }

        public static void encode(SyncNetwork message, FriendlyByteBuf buf) {
            buf.writeBoolean(message.showBackpack);
            buf.writeBoolean(message.showBackItem);
            buf.writeBoolean(message.showChatBubble);
            buf.writeFloat(message.soundFreq);
            buf.writeEnum(message.pickType);
            buf.writeBoolean(message.openDoor);
            buf.writeBoolean(message.openFenceGate);
            buf.writeBoolean(message.activeClimbing);
            buf.writeBoolean(message.allowTableFood);
            buf.writeEnum(message.combatResponsePolicy);
        }

        public static SyncNetwork decode(FriendlyByteBuf buf) {
            boolean showBackpack = buf.readBoolean();
            boolean showBackItem = buf.readBoolean();
            boolean showChatBubble = buf.readBoolean();
            float soundFreq = buf.readFloat();
            PickType pickType = buf.readEnum(PickType.class);
            boolean openDoor = buf.readBoolean();
            boolean openFenceGate = buf.readBoolean();
            boolean activeClimbing = buf.readBoolean();
            boolean allowTableFood = buf.readBoolean();
            MaidCombatResponsePolicy combatResponsePolicy = buf.readEnum(MaidCombatResponsePolicy.class);
            return new SyncNetwork(showBackpack, showBackItem, showChatBubble, soundFreq, pickType, openDoor,
                    openFenceGate, activeClimbing, allowTableFood, combatResponsePolicy);
        }

        public static void handle(SyncNetwork message, EntityMaid maid) {
            MaidConfigManager configManager = maid.getConfigManager();
            configManager.setShowBackpack(message.showBackpack);
            configManager.setShowBackItem(message.showBackItem);
            configManager.setChatBubbleShow(message.showChatBubble);
            configManager.setSoundFreq(message.soundFreq);
            configManager.setPickupType(message.pickType);
            configManager.setOpenDoor(message.openDoor);
            configManager.setOpenFenceGate(message.openFenceGate);
            configManager.setActiveClimbing(message.activeClimbing);
            configManager.setTableFoodAllowed(message.allowTableFood);
            if (!message.allowTableFood) {
                // Stop the table-food action immediately without clearing the
                // shared movement target of an unrelated work behavior.
                maid.getBrain().eraseMemory(InitEntities.MAID_EDIBLE_BLOCK_ACTION);
            }
            if (configManager.getCombatResponsePolicy() != message.combatResponsePolicy) {
                maid.getCombatManager().setResponsePolicy(message.combatResponsePolicy);
            }
        }

        public boolean showBackpack() {
            return showBackpack;
        }

        public boolean showBackItem() {
            return showBackItem;
        }

        public boolean showChatBubble() {
            return showChatBubble;
        }

        public float soundFreq() {
            return soundFreq;
        }

        public PickType pickType() {
            return pickType;
        }

        public boolean openDoor() {
            return openDoor;
        }

        public boolean openFenceGate() {
            return openFenceGate;
        }

        public boolean activeClimbing() {
            return activeClimbing;
        }

        public boolean allowTableFood() {
            return allowTableFood;
        }

        public MaidCombatResponsePolicy combatResponsePolicy() {
            return combatResponsePolicy;
        }

        public void setShowBackpack(boolean showBackpack) {
            this.showBackpack = showBackpack;
        }

        public void setShowBackItem(boolean showBackItem) {
            this.showBackItem = showBackItem;
        }

        public void setShowChatBubble(boolean showChatBubble) {
            this.showChatBubble = showChatBubble;
        }

        public void setSoundFreq(float soundFreq) {
            this.soundFreq = Mth.clamp(soundFreq, 0f, 1f);
        }

        public void setPickType(PickType pickType) {
            this.pickType = pickType;
        }

        public void setOpenDoor(boolean openDoor) {
            this.openDoor = openDoor;
        }

        public void setOpenFenceGate(boolean openFenceGate) {
            this.openFenceGate = openFenceGate;
        }

        public void setActiveClimbing(boolean activeClimbing) {
            this.activeClimbing = activeClimbing;
        }

        public void setAllowTableFood(boolean allowTableFood) {
            this.allowTableFood = allowTableFood;
        }

        public void setCombatResponsePolicy(MaidCombatResponsePolicy combatResponsePolicy) {
            this.combatResponsePolicy = combatResponsePolicy;
        }
    }
}
