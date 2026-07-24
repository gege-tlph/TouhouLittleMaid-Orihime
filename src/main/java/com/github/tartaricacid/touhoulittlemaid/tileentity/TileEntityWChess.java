package com.github.tartaricacid.touhoulittlemaid.tileentity;

import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import cn.sh1rocu.touhoulittlemaid.api.extension.IBlockEntityPersistentData;
import com.github.tartaricacid.touhoulittlemaid.api.block.IBoardGameEntityBlock;
import com.github.tartaricacid.touhoulittlemaid.api.game.chess.Position;
import com.github.tartaricacid.touhoulittlemaid.init.InitBlocks;
import com.github.tartaricacid.touhoulittlemaid.util.WChessUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class TileEntityWChess extends TileEntityJoy implements IBoardGameEntityBlock, IBlockEntityPersistentData {
    public static final BlockEntityType<TileEntityWChess> TYPE = FabricBlockEntityTypeBuilder.create(TileEntityWChess::new, InitBlocks.WCHESS).build();

    private static final String CHESS_DATA = "ChessData";
    private static final String CHESS_COUNTER = "ChessCounter";
    private static final String SELECT_CHESS_POINT = "SelectChessPoint";
    private static final String CHECKMATE = "Checkmate";
    private static final String REPEAT = "Repeat";
    private static final String MOVE_NUMBER_LIMIT = "MoveNumberLimit";

    private final Position chessData;

    // 回合计数器
    private int chessCounter = 0;
    // 当前选中的棋子
    private int selectChessPoint = 0;
    // 将死（依据下棋方，判断谁输谁赢）
    private boolean checkmate = false;
    // 长打（判和）
    private boolean repeat = false;
    // 50 回限着（判和）
    private boolean moveNumberLimit = false;

    public TileEntityWChess(BlockPos pos, BlockState blockState) {
        super(TYPE, pos, blockState);
        this.chessData = new Position();
        this.chessData.fromFen(WChessUtil.INIT);
    }

    public void setEndgame(String endgame) {
        this.chessData.fromFen(endgame);
        this.refresh();
    }

    @Override
    protected void saveAdditional(ValueOutput output){
        CompoundTag data = tlm$getPersistentData();
        data.putString(CHESS_DATA, chessData.toFen());
        data.putInt(CHESS_COUNTER, chessCounter);
        data.putInt(SELECT_CHESS_POINT, selectChessPoint);
        data.putBoolean(CHECKMATE, checkmate);
        data.putBoolean(REPEAT, repeat);
        data.putBoolean(MOVE_NUMBER_LIMIT, moveNumberLimit);
        super.saveAdditional(output);
    }

    @Override
    public void loadAdditional(ValueInput input){
        super.loadAdditional(input);
        CompoundTag data = tlm$getPersistentData();
        chessCounter = data.getIntOr(CHESS_COUNTER, 0);
        selectChessPoint = data.getIntOr(SELECT_CHESS_POINT, 0);
        chessData.fromFen(data.getStringOr(CHESS_DATA, ""));
        checkmate = data.getBooleanOr(CHECKMATE, false);
        repeat = data.getBooleanOr(REPEAT, false);
        moveNumberLimit = data.getBooleanOr(MOVE_NUMBER_LIMIT, false);
    }

    public void reset() {
        this.chessCounter = 0;
        this.selectChessPoint = 0;
        this.chessData.fromFen(WChessUtil.INIT);
        this.checkmate = false;
        this.repeat = false;
        this.moveNumberLimit = false;
    }

    public Position getChessData() {
        return chessData;
    }

    public boolean isCheckmate() {
        return checkmate;
    }

    public void setCheckmate(boolean checkmate) {
        this.checkmate = checkmate;
    }

    public boolean isPlayerTurn() {
        return WChessUtil.isPlayer(this.chessData);
    }

    public int getChessCounter() {
        return chessCounter;
    }

    public void addChessCounter() {
        this.chessCounter += 1;
    }

    public int getSelectChessPoint() {
        return selectChessPoint;
    }

    public void setSelectChessPoint(int selectChessPoint) {
        this.selectChessPoint = selectChessPoint;
    }

    public boolean isRepeat() {
        return repeat;
    }

    public void setRepeat(boolean repeat) {
        this.repeat = repeat;
    }

    public boolean isMoveNumberLimit() {
        return moveNumberLimit;
    }

    public void setMoveNumberLimit(boolean moveNumberLimit) {
        this.moveNumberLimit = moveNumberLimit;
    }
}
