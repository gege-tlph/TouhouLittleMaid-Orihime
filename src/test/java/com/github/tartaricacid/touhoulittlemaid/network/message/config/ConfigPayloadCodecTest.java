package com.github.tartaricacid.touhoulittlemaid.network.message.config;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConfigPayloadCodecTest {
    @Test
    void saveRulesPayloadRoundTripsChangedOnlyJson() {
        SaveServerRulesPacket expected = new SaveServerRulesPacket("{\"maid.MaidWorkRange\":17}");
        ByteBuf buffer = Unpooled.buffer();
        try {
            SaveServerRulesPacket.STREAM_CODEC.encode(buffer, expected);
            assertEquals(expected, SaveServerRulesPacket.STREAM_CODEC.decode(buffer));
        } finally {
            buffer.release();
        }
    }

    @Test
    void syncRulesPayloadRoundTripsBothSnapshotsAndFlags() {
        SyncServerRulesPacket expected = new SyncServerRulesPacket(
                "{\"maid.MaidWorkRange\":12}", false, true, "{\"maid.MaidWorkRange\":17}");
        ByteBuf buffer = Unpooled.buffer();
        try {
            SyncServerRulesPacket.STREAM_CODEC.encode(buffer, expected);
            SyncServerRulesPacket actual = SyncServerRulesPacket.STREAM_CODEC.decode(buffer);
            assertEquals(expected, actual);
            // 两份快照不是同一份：专服上运行期值与文件值可以不同，编解码不许把它们并成一个字段。
            assertEquals("{\"maid.MaidWorkRange\":12}", actual.runtimeRulesJson());
            assertEquals("{\"maid.MaidWorkRange\":17}", actual.editableRulesJson());
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
    void oversizedPayloadIsRejectedByOurOwnEncoder() {
        String oversized = "x".repeat(1_048_577);
        ByteBuf buffer = Unpooled.buffer();
        try {
            assertThrows(RuntimeException.class, () -> SaveServerRulesPacket.STREAM_CODEC.encode(
                    buffer, new SaveServerRulesPacket(oversized)));
        } finally {
            buffer.release();
        }
    }

    /**
     * 上面那条只证明**我们自己的**客户端发不出超长载荷，而攻击者不会使用我们的编码器。
     * 这里绕开编码器、直接写一个格式合法但超长的字符串，断言**解码侧自己**会拒绝——
     * 服务端的长度上限必须由接收方强制，不能依赖发送方自律。
     */
    @Test
    void oversizedPayloadIsRejectedOnDecodeWhenOurEncoderIsBypassed() {
        String oversized = "x".repeat(1_048_577);
        ByteBuf buffer = Unpooled.buffer();
        try {
            new FriendlyByteBuf(buffer).writeUtf(oversized, oversized.length());
            assertThrows(RuntimeException.class,
                    () -> SaveServerRulesPacket.STREAM_CODEC.decode(buffer));
        } finally {
            buffer.release();
        }
    }
}
