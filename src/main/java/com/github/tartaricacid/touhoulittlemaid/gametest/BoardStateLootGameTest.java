package com.github.tartaricacid.touhoulittlemaid.gametest;

import com.github.tartaricacid.touhoulittlemaid.datapack.BoardStateData;
import com.github.tartaricacid.touhoulittlemaid.init.InitItems;
import com.github.tartaricacid.touhoulittlemaid.item.ItemBoardState;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * 预设棋谱与随机残局战利品函数的运行期验收。
 *
 * <p>这里能测到 JUnit 测不到的两件事：数据包里的棋谱**真的被重载监听器读进来了**，
 * 以及战利品函数**真的注册进了注册表**（没注册的话，任何引用它的战利品表都会在加载时报错）。</p>
 */
public class BoardStateLootGameTest {
    @GameTest
    public void presetBoardStatesAreLoadedFromTheDatapack(GameTestHelper helper) {
        // 三种棋各自的预设都应当被读进来——随包附带的 board_states/*.json 里都有内容
        int gomoku = BoardStateData.getGomokuRecords().size();
        int xiangqi = BoardStateData.getXiangqiRecords().size();
        int chess = BoardStateData.getChessRecords().size();
        if (gomoku == 0 || xiangqi == 0 || chess == 0) {
            helper.fail("预设棋谱没被读进来（五子棋 " + gomoku + " / 象棋 " + xiangqi + " / 国象 " + chess
                    + "）：重载监听器没注册，或数据包路径不对");
            return;
        }
        helper.succeed();
    }

    @GameTest
    public void randomBoardStateLootFunctionIsRegistered(GameTestHelper helper) {
        Identifier id = Identifier.fromNamespaceAndPath("touhou_little_maid", "board_state_randomly");
        if (!BuiltInRegistries.LOOT_FUNCTION_TYPE.containsKey(id)) {
            helper.fail("board_state_randomly 战利品函数没注册：引用它的战利品表会在加载时报错");
            return;
        }
        helper.succeed();
    }

    /** 按物品分拣：三个道具各自只应拿到自己那一类的棋谱。 */
    @GameTest
    public void recordsAreRoutedByItemType(GameTestHelper helper) {
        record Case(ItemStack stack, int expected, String name) {
        }
        for (Case testCase : new Case[]{
                new Case(InitItems.GOMOKU_BOARD_STATE.getDefaultInstance(),
                        BoardStateData.getGomokuRecords().size(), "五子棋"),
                new Case(InitItems.CCHESS_BOARD_STATE.getDefaultInstance(),
                        BoardStateData.getXiangqiRecords().size(), "象棋"),
                new Case(InitItems.WCHESS_BOARD_STATE.getDefaultInstance(),
                        BoardStateData.getChessRecords().size(), "国际象棋")}) {
            int actual = BoardStateData.getRecordsByItem(testCase.stack()).size();
            if (actual != testCase.expected()) {
                helper.fail(testCase.name() + " 残局道具拿到的棋谱数不对：期望 "
                        + testCase.expected() + "，实为 " + actual);
                return;
            }
        }
        // 不是残局道具的物品必须拿到空表，而不是误命中某一类
        if (!BoardStateData.getRecordsByItem(new ItemStack(net.minecraft.world.item.Items.STONE)).isEmpty()) {
            helper.fail("非残局道具也拿到了棋谱");
            return;
        }
        helper.succeed();
    }

    /**
     * 那张战利品表真的被服务器加载了。
     *
     * <p>表是 datagen 产物、函数是代码注册的，**两边的 id 对不上时只会在加载时报错**，
     * 而报错发生在服务器启动早期，很容易被当成别的问题。这条把它钉在明面上。</p>
     */
    @GameTest
    public void randomBoardStateLootTableIsLoaded(GameTestHelper helper) {
        var key = net.minecraft.resources.ResourceKey.create(
                net.minecraft.core.registries.Registries.LOOT_TABLE,
                Identifier.fromNamespaceAndPath("touhou_little_maid", "chest/random_board_state"));
        var table = helper.getLevel().getServer().reloadableRegistries().getLootTable(key);
        if (table == net.minecraft.world.level.storage.loot.LootTable.EMPTY) {
            helper.fail("chest/random_board_state 战利品表没加载：datagen 产物与函数注册的 id 可能对不上");
            return;
        }
        helper.succeed();
    }

    /** 填过棋谱的道具必须真的带上了数据——这是战利品函数唯一的可见产物。 */
    @GameTest
    public void aPresetCanBeWrittenOntoTheItem(GameTestHelper helper) {
        var records = BoardStateData.getGomokuRecords();
        if (records.isEmpty()) {
            helper.fail("没有五子棋预设可用");
            return;
        }
        var record = records.get(0);
        ItemStack stack = InitItems.GOMOKU_BOARD_STATE.getDefaultInstance();
        ItemBoardState.setState(stack, record.data(), record.display().description(), record.display().author());
        String[] state = ItemBoardState.getState(stack);
        if (state == null || !record.data().equals(state[0])) {
            helper.fail("预设棋谱没写进道具");
            return;
        }
        helper.succeed();
    }
}
