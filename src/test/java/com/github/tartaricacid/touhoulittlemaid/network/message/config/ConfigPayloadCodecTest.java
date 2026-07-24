package com.github.tartaricacid.touhoulittlemaid.network.message.config;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConfigPayloadCodecTest {
    @Test
    void saveRulesPayloadRoundTripsChangedOnlyJson() {
        SaveServerRulesPacket expected = new SaveServerRulesPacket("{\"MaidConfig.MaidWorkRange\":17}");
        ByteBuf buffer = Unpooled.buffer();
        try {
            SaveServerRulesPacket.STREAM_CODEC.encode(buffer, expected);
            assertEquals(expected, SaveServerRulesPacket.STREAM_CODEC.decode(buffer));
        } finally {
            buffer.release();
        }
    }

    @Test
    void truncatedPayloadIsRejectedByTheStandardFabricCodec() {
        ByteBuf complete = Unpooled.buffer();
        try {
            SaveServerRulesPacket.STREAM_CODEC.encode(complete,
                    new SaveServerRulesPacket("{\"field\":\"value\"}"));
            ByteBuf truncated = complete.copy(0, complete.readableBytes() - 1);
            try {
                assertThrows(RuntimeException.class,
                        () -> SaveServerRulesPacket.STREAM_CODEC.decode(truncated));
            } finally {
                truncated.release();
            }
        } finally {
            complete.release();
        }
    }

    @Test
    void oversizedPayloadIsRejectedBeforeItCanReachTheServerThread() {
        String oversized = "x".repeat(1_048_577);
        ByteBuf buffer = Unpooled.buffer();
        try {
            assertThrows(RuntimeException.class, () -> SaveServerRulesPacket.STREAM_CODEC.encode(
                    buffer, new SaveServerRulesPacket(oversized)));
        } finally {
            buffer.release();
        }
    }
}
