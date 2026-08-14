package com.github.tartaricacid.touhoulittlemaid.gametest;

import com.github.tartaricacid.touhoulittlemaid.api.game.gomoku.GomokuCodec;
import com.github.tartaricacid.touhoulittlemaid.api.game.gomoku.Point;
import com.github.tartaricacid.touhoulittlemaid.init.InitDataComponent;
import com.github.tartaricacid.touhoulittlemaid.init.InitItems;
import com.github.tartaricacid.touhoulittlemaid.item.ItemBoardState;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;

/**
 * 残局道具这条链路的运行期验收。
 *
 * <p>宿主迁移时把整簇删了（三个道具、编解码、方块里的分支、方块实体的存取），本刀补回。
 * 编解码本身由 {@code GomokuCodecTest} 在 JUnit 层测；这里测的是**只有真游戏里才成立的那半**：
 * 三个物品真的注册进了物品注册表、数据组件真的能往物品上存取。</p>
 *
 * <p>为什么必须是 GameTest：物品的数据组件在纯 {@code Bootstrap.bootStrap()} 下不绑定
 * （实测抛 {@code NullPointerException: Components not bound yet}），这一条 JUnit 做不到。</p>
 */
public class BoardStateItemGameTest {
    @GameTest
    public void allThreeBoardStateItemsAreRegistered(GameTestHelper helper) {
        for (var item : new Object[][]{
                {"gomoku", InitItems.GOMOKU_BOARD_STATE},
                {"cchess", InitItems.CCHESS_BOARD_STATE},
                {"wchess", InitItems.WCHESS_BOARD_STATE}}) {
            if (!(item[1] instanceof net.minecraft.world.item.Item registered)
                    || !(registered instanceof ItemBoardState)) {
                helper.fail(item[0] + " 残局道具没注册成 ItemBoardState");
                return;
            }
        }
        helper.succeed();
    }

    /** 存进去要能原样读回来——读不回来的话，玩家右键棋盘只会得到一个 FAIL。 */
    @GameTest
    public void stateSurvivesTheDataComponentRoundTrip(GameTestHelper helper) {
        byte[][] board = new byte[15][15];
        board[3][4] = Point.BLACK;
        board[10][11] = Point.WHITE;
        String encoded = GomokuCodec.encode(
                new GomokuCodec.StateData(board, 2, new Point(10, 11, Point.WHITE)));

        ItemStack stack = InitItems.GOMOKU_BOARD_STATE.getDefaultInstance();
        ItemBoardState.setState(stack, encoded, "board_state.touhou_little_maid.gomoku.default", "tester");

        String[] state = ItemBoardState.getState(stack);
        if (state == null) {
            helper.fail("刚存进去的残局读不回来：数据组件没生效");
            return;
        }
        if (!encoded.equals(state[0])) {
            helper.fail("残局数据读回来变了：存的 " + encoded + "，读到 " + state[0]);
            return;
        }
        if (!"tester".equals(state[2])) {
            helper.fail("作者字段读回来变了：" + state[2]);
            return;
        }

        // 再解码一次，确认整条链（编码 → 存物品 → 读物品 → 解码）是通的
        GomokuCodec.StateData back = GomokuCodec.decode(state[0]);
        if (back.board()[3][4] != Point.BLACK || back.board()[10][11] != Point.WHITE) {
            helper.fail("整条链走完棋子位置对不上了");
            return;
        }
        helper.succeed();
    }

    /** 没存过残局的道具必须读出 null，而不是空字符串——方块那边靠 null 判定拒绝。 */
    @GameTest
    public void freshItemHasNoState(GameTestHelper helper) {
        ItemStack fresh = InitItems.GOMOKU_BOARD_STATE.getDefaultInstance();
        if (fresh.get(InitDataComponent.BOARD_STATE_TAG) != null
                || ItemBoardState.getState(fresh) != null) {
            helper.fail("新道具上不该有残局数据");
            return;
        }
        helper.succeed();
    }
}
