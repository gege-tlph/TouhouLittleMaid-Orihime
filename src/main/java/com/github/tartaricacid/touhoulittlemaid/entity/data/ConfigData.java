package com.github.tartaricacid.touhoulittlemaid.entity.data;

import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.github.tartaricacid.touhoulittlemaid.entity.ai.combat.MaidCombatResponsePolicy;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.PickType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

public record ConfigData(long booleanValue, PickType pickupType, float soundFreq,
                         MaidCombatResponsePolicy combatResponsePolicy) {
    private static final int PICKUP_FLAG = 0;
    private static final int HOME_MODE_FLAG = 1;
    private static final int RIDEABLE_FLAG = 2;
    private static final int SHOW_BACKPACK_FLAG = 3;
    private static final int SHOW_BACK_ITEM_FLAG = 4;
    private static final int CHAT_BUBBLE_SHOW_FLAG = 5;
    private static final int OPEN_DOOR_FLAG = 6;
    private static final int OPEN_FENCE_GATE_FLAG = 7;
    private static final int ACTIVE_CLIMBING_FLAG = 8;
    /**
     * ⚠️ **这一位是反的：置位 = 不许吃桌上食物**，读口 {@link #isTableFoodAllowed()} 取反。
     *
     * <p>行为基准那边这项存成独立 NBT 键、缺键时 {@code getBooleanOr(TAG, true)} 回落 true，
     * 于是旧档天然保持「一直允许」。**照搬到位域上会翻转**：旧档的 boolean_value 里没有第 9 位，
     * 正读就是 false = 不许吃，等于给每一只已存在的女仆静默关掉这项行为，且无人会发现。
     * 把语义定成「置位 = 禁止」，旧档（位为 0）自然落在「允许」，新默认也不必置位——
     * 不需要任何存档迁移，也不需要给 boolean_value 加版本号。</p>
     */
    private static final int DISALLOW_TABLE_FOOD_FLAG = 9;

    private static final long DEFAULT_BOOLEAN_VALUE =
            (1L << PICKUP_FLAG)
                    // HOME_MODE 默认关闭
                    | (1L << RIDEABLE_FLAG)
                    | (1L << SHOW_BACKPACK_FLAG)
                    | (1L << SHOW_BACK_ITEM_FLAG)
                    | (1L << CHAT_BUBBLE_SHOW_FLAG)
                    | (1L << OPEN_DOOR_FLAG)
                    | (1L << OPEN_FENCE_GATE_FLAG)
                    | (1L << ACTIVE_CLIMBING_FLAG);
    // DISALLOW_TABLE_FOOD 有意不置位：默认允许吃桌上食物（见该常量的 javadoc）

    private static final float DEFAULT_SOUND_FREQ = 1.0f;

    /**
     * 威胁响应策略按稳定序列化名存字符串，且解码必须宽容：缺键（旧档）与坏值（未来值/手改档）
     * 都回落到护主，绝不能让整个 ConfigData 解码失败——那会把其余配置一并打回默认。
     */
    private static final Codec<MaidCombatResponsePolicy> RESPONSE_POLICY_CODEC = Codec.STRING.xmap(
            MaidCombatResponsePolicy::fromSerializedName, MaidCombatResponsePolicy::serializedName);

    private static final Codec<ConfigData> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.LONG.fieldOf("boolean_value").forGetter(ConfigData::booleanValue),
            PickType.CODEC.fieldOf("pickup_type").forGetter(ConfigData::pickupType),
            Codec.FLOAT.fieldOf("sound_freq").forGetter(ConfigData::soundFreq),
            RESPONSE_POLICY_CODEC.optionalFieldOf("combat_response_policy",
                    MaidCombatResponsePolicy.PROTECT_OWNER).forGetter(ConfigData::combatResponsePolicy)
    ).apply(ins, ConfigData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ConfigData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.LONG, ConfigData::booleanValue,
            PickType.STREAM_CODEC, ConfigData::pickupType,
            ByteBufCodecs.FLOAT, ConfigData::soundFreq,
            ByteBufCodecs.idMapper(MaidCombatResponsePolicy::fromOrdinal, MaidCombatResponsePolicy::ordinal),
            ConfigData::combatResponsePolicy,
            ConfigData::new
    );

    public static final AttachmentType<ConfigData> TYPE = AttachmentRegistry.create(
            Identifier.fromNamespaceAndPath(TouhouLittleMaid.MOD_ID, "config"), builder -> builder
                    .initializer(ConfigData::defaultConfig)
                    .persistent(CODEC)
                    .syncWith(STREAM_CODEC, AttachmentSyncPredicate.all()));

    private static ConfigData defaultConfig() {
        return new ConfigData(DEFAULT_BOOLEAN_VALUE, PickType.ALL, DEFAULT_SOUND_FREQ,
                MaidCombatResponsePolicy.PROTECT_OWNER);
    }

    public ConfigData {
        soundFreq = Math.max(0f, Math.min(1f, soundFreq));
    }

    private ConfigData setFlag(int flag, boolean enabled) {
        long mask = 1L << flag;
        long newBooleanValue = enabled ? (booleanValue | mask) : (booleanValue & ~mask);
        return new ConfigData(newBooleanValue, pickupType, soundFreq, combatResponsePolicy);
    }

    private boolean hasFlag(int flag) {
        return (booleanValue & (1L << flag)) != 0;
    }

    public ConfigData setPickup(boolean isPickup) {
        return setFlag(PICKUP_FLAG, isPickup);
    }

    public boolean isPickup() {
        return hasFlag(PICKUP_FLAG);
    }

    public ConfigData setHomeModeEnable(boolean enable) {
        return setFlag(HOME_MODE_FLAG, enable);
    }

    public boolean isHomeModeEnable() {
        return hasFlag(HOME_MODE_FLAG);
    }

    public ConfigData setRideable(boolean rideable) {
        return setFlag(RIDEABLE_FLAG, rideable);
    }

    public boolean isRideable() {
        return hasFlag(RIDEABLE_FLAG);
    }

    public ConfigData setShowBackpack(boolean show) {
        return setFlag(SHOW_BACKPACK_FLAG, show);
    }

    public boolean isShowBackpack() {
        return hasFlag(SHOW_BACKPACK_FLAG);
    }

    public ConfigData setShowBackItem(boolean show) {
        return setFlag(SHOW_BACK_ITEM_FLAG, show);
    }

    public boolean isShowBackItem() {
        return hasFlag(SHOW_BACK_ITEM_FLAG);
    }

    public ConfigData setChatBubbleShow(boolean show) {
        return setFlag(CHAT_BUBBLE_SHOW_FLAG, show);
    }

    public boolean isChatBubbleShow() {
        return hasFlag(CHAT_BUBBLE_SHOW_FLAG);
    }

    public ConfigData setOpenDoor(boolean openDoor) {
        return setFlag(OPEN_DOOR_FLAG, openDoor);
    }

    public boolean isOpenDoor() {
        return hasFlag(OPEN_DOOR_FLAG);
    }

    public ConfigData setOpenFenceGate(boolean openFenceGate) {
        return setFlag(OPEN_FENCE_GATE_FLAG, openFenceGate);
    }

    public boolean isOpenFenceGate() {
        return hasFlag(OPEN_FENCE_GATE_FLAG);
    }

    public ConfigData setActiveClimbing(boolean activeClimbing) {
        return setFlag(ACTIVE_CLIMBING_FLAG, activeClimbing);
    }

    public boolean isActiveClimbing() {
        return hasFlag(ACTIVE_CLIMBING_FLAG);
    }

    /**
     * 女仆是否允许吃已摆放的桌上食物。默认允许——这项是**退出式**开关，
     * 玩家不主动关它就一直是开的（旧档同理，见 {@code DISALLOW_TABLE_FOOD_FLAG}）。
     */
    public boolean isTableFoodAllowed() {
        return !hasFlag(DISALLOW_TABLE_FOOD_FLAG);
    }

    public ConfigData setTableFoodAllowed(boolean allowed) {
        return setFlag(DISALLOW_TABLE_FOOD_FLAG, !allowed);
    }

    public ConfigData setPickupType(PickType pickupType) {
        return new ConfigData(booleanValue, pickupType, soundFreq, combatResponsePolicy);
    }

    public PickType getPickupType() {
        return pickupType;
    }

    public ConfigData setSoundFreq(float soundFreq) {
        return new ConfigData(booleanValue, pickupType, soundFreq, combatResponsePolicy);
    }

    public ConfigData setCombatResponsePolicy(MaidCombatResponsePolicy combatResponsePolicy) {
        return new ConfigData(booleanValue, pickupType, soundFreq, combatResponsePolicy);
    }
}
