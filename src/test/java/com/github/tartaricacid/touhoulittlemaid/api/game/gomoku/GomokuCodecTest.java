package com.github.tartaricacid.touhoulittlemaid.api.game.gomoku;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 五子棋残局的编解码。
 *
 * <p>这是「残局道具」这条链路的地基：道具里存的就是这个字符串，数据包里手写的也是它。
 * 编解码不对称的后果是**玩家的残局静默变成另一局棋**，而不是报错——所以往返必须有测试钉住。</p>
 *
 * <p>纯逻辑、不碰任何 MC 注册表，故落在 JUnit 而非 GameTest
 * （对照：物品的数据组件在纯 {@code Bootstrap} 下不绑定，那类只能进 GameTest）。</p>
 */
class GomokuCodecTest {
    private static byte[][] emptyBoard() {
        return new byte[15][15];
    }

    @Test
    void emptyBoardRoundTrips() {
        GomokuCodec.StateData original = new GomokuCodec.StateData(emptyBoard(), 0, new Point(0, 0, Point.EMPTY));
        GomokuCodec.StateData back = GomokuCodec.decode(GomokuCodec.encode(original));
        assertArrayEquals(original.board(), back.board());
        assertEquals(0, back.turnCount());
    }

    @Test
    void stonesOfBothColoursSurviveTheRoundTrip() {
        byte[][] board = emptyBoard();
        board[0][0] = Point.BLACK;
        board[14][14] = Point.WHITE;
        board[7][7] = Point.BLACK;
        board[7][8] = Point.WHITE;
        // 边角与中心都放：位图是按行优先铺的，端点错位最容易在这两处暴露
        GomokuCodec.StateData original = new GomokuCodec.StateData(board, 4, new Point(7, 8, Point.WHITE));

        GomokuCodec.StateData back = GomokuCodec.decode(GomokuCodec.encode(original));
        assertArrayEquals(board, back.board());
        assertEquals(4, back.turnCount());
        assertEquals(7, back.latestPoint().x);
        assertEquals(8, back.latestPoint().y);
        assertEquals(Point.WHITE, back.latestPoint().getType());
    }

    /** 最后一手的 type 是**从棋盘上重新读出来的**，不是原样带过去的——这条钉住那个语义。 */
    @Test
    void latestPointTypeIsDerivedFromTheBoard() {
        byte[][] board = emptyBoard();
        board[3][4] = Point.BLACK;
        // 故意传一个与棋盘不符的 type，解码后应以棋盘为准
        String encoded = GomokuCodec.encode(new GomokuCodec.StateData(board, 1, new Point(3, 4, Point.WHITE)));
        assertEquals(Point.BLACK, GomokuCodec.decode(encoded).latestPoint().getType());
    }

    @Test
    void encodedFormIsTheThreeCommaSeparatedFieldsDataPacksWrite() {
        String encoded = GomokuCodec.encode(new GomokuCodec.StateData(emptyBoard(), 2, new Point(1, 2, Point.EMPTY)));
        String[] parts = encoded.split(",");
        assertEquals(3, parts.length, "数据包里手写的就是这三段，格式变了会让现有棋谱全部读不出来");
        assertEquals("2", parts[1].trim());
        assertEquals("1_2", parts[2].trim());
    }

    /** 玩家/数据包会写出各种坏字符串，解码必须抛而不是给出一局错棋。 */
    @Test
    void malformedInputIsRejectedRatherThanSilentlyDecoded() {
        for (String bad : new String[]{
                "", "onlyonefield", "a,b", "a,b,c,d",
                "notbase64!!!, 0, 0_0",
                "AAAA, notanumber, 0_0",
        }) {
            assertThrows(RuntimeException.class, () -> GomokuCodec.decode(bad), bad);
        }
    }

    /** 编码里带空白（数据包为了好读常会加）必须能正常解析。 */
    @Test
    void whitespaceInEncodedStringIsTolerated() {
        String encoded = GomokuCodec.encode(new GomokuCodec.StateData(emptyBoard(), 3, new Point(5, 6, Point.EMPTY)));
        assertTrue(encoded.contains(", "), "编码输出本身就带空格，解码端必须容忍");
        GomokuCodec.StateData back = GomokuCodec.decode(encoded.replace(",", " ,  "));
        assertEquals(3, back.turnCount());
        assertEquals(5, back.latestPoint().x);
    }
}
