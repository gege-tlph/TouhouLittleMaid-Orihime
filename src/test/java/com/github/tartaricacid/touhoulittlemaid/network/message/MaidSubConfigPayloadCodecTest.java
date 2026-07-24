package com.github.tartaricacid.touhoulittlemaid.network.message;

import com.github.tartaricacid.touhoulittlemaid.entity.ai.combat.MaidCombatResponsePolicy;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.MaidConfigManager;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.PickType;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MaidSubConfigPayloadCodecTest {
    @Test
    void standardFabricPayloadRoundTripsEveryMaidSetting() {
        MaidConfigManager.SyncNetwork settings = new MaidConfigManager.SyncNetwork(
                true, false, true, 0.375F, PickType.ONLY_XP,
                false, true, false, true, MaidCombatResponsePolicy.SELF_DEFENSE);
        MaidSubConfigPackage expected = new MaidSubConfigPackage(42, settings);
        ByteBuf buffer = Unpooled.buffer();
        try {
            MaidSubConfigPackage.STREAM_CODEC.encode(buffer, expected);
            MaidSubConfigPackage decoded = MaidSubConfigPackage.STREAM_CODEC.decode(buffer);

            assertEquals(42, decoded.id());
            MaidConfigManager.SyncNetwork actual = decoded.syncNetwork();
            assertTrue(actual.showBackpack());
            assertFalse(actual.showBackItem());
            assertTrue(actual.showChatBubble());
            assertEquals(0.375F, actual.soundFreq());
            assertEquals(PickType.ONLY_XP, actual.pickType());
            assertFalse(actual.openDoor());
            assertTrue(actual.openFenceGate());
            assertFalse(actual.activeClimbing());
            assertTrue(actual.allowTableFood());
            assertEquals(MaidCombatResponsePolicy.SELF_DEFENSE, actual.combatResponsePolicy());
        } finally {
            buffer.release();
        }
    }

    @Test
    void truncatedPolicyPayloadIsRejectedBeforeServerHandling() {
        MaidConfigManager.SyncNetwork settings = new MaidConfigManager.SyncNetwork(
                true, true, true, 1.0F, PickType.ALL,
                true, true, true, true, MaidCombatResponsePolicy.PROTECT_OWNER);
        ByteBuf complete = Unpooled.buffer();
        try {
            MaidSubConfigPackage.STREAM_CODEC.encode(complete, new MaidSubConfigPackage(7, settings));
            ByteBuf truncated = complete.copy(0, complete.readableBytes() - 1);
            try {
                assertThrows(RuntimeException.class,
                        () -> MaidSubConfigPackage.STREAM_CODEC.decode(truncated));
            } finally {
                truncated.release();
            }
        } finally {
            complete.release();
        }
    }
}
