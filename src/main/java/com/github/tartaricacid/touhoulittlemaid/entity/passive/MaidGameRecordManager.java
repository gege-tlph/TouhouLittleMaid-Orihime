package com.github.tartaricacid.touhoulittlemaid.entity.passive;

import com.github.tartaricacid.touhoulittlemaid.entity.item.EntitySit;
import com.github.tartaricacid.touhoulittlemaid.init.InitSounds;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import static com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid.GAME_STATUE;
import static com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid.GOMOKU_WIN_COUNT;

public class MaidGameRecordManager {
    // 逐字节对齐 origin/1.21.1 磁盘格式：嵌套 "MaidGameSkillData"{"Gomoku":int}（此前被移植期改名扁平化为根 int
    //   "GomokuWinCount" → 跨版本升级世界 Gomoku 胜场重置，见 CLIENT_AUDIT §I.B/S3）。
    private static final String GAME_SKILL_TAG = "MaidGameSkillData";
    private static final String GOMOKU = "Gomoku";
    private static final byte NONE = 0, WIN = 1, LOSE = 2;

    private final EntityMaid maid;

    public MaidGameRecordManager(EntityMaid maid) {
        this.maid = maid;
    }

    void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(GAME_STATUE, (byte) 0);
        // 胜场必须走同步数据：客户端 GUI tooltip（AbstractMaidContainerGui 的五子棋段位行）直接读它，
        // 普通字段在客户端实体上恒为 0
        builder.define(GOMOKU_WIN_COUNT, 0);
    }

    void addAdditionalSaveData(ValueOutput output) {
        output.child(GAME_SKILL_TAG).putInt(GOMOKU, getGomokuWinCount());
    }

    void readAdditionalSaveData(ValueInput input) {
        input.child(GAME_SKILL_TAG).ifPresent(gameSkill ->
                maid.getEntityData().set(GOMOKU_WIN_COUNT, gameSkill.getIntOr(GOMOKU, 0)));
    }

    void tick() {
        // 移植漂移回填：origin/1.21.1 这里是
        //   if (!(this.maid.getVehicle() instanceof EntitySit) && getGameStatue() != NONE)
        // 移植期把 EntitySit 判据丢了，于是胜负状态在 markStatue 的下一 tick 就被清零，
        // AnimationManager 的 isWin()/isLost() 恒为 false —— 女仆下棋胜负动画从此不再播放。
        // 该判据同时是上游 #912（棋盘被破坏后女仆卡在棋局状态）的自愈机制：
        // TileEntityJoy.preRemoveSideEffects 会 discard 座位实体，女仆随即不再骑乘 EntitySit，
        // 下一 tick 状态自动复位。
        if (!(this.maid.getVehicle() instanceof EntitySit) && getGameStatue() != NONE) {
            resetStatue();
        }
    }

    private byte getGameStatue() {
        return maid.getEntityData().get(GAME_STATUE);
    }

    private void setGameStatue(byte gameStatue) {
        maid.getEntityData().set(GAME_STATUE, gameStatue);
    }

    public int getGomokuWinCount() {
        return maid.getEntityData().get(GOMOKU_WIN_COUNT);
    }

    public void increaseGomokuWinCount() {
        maid.getEntityData().set(GOMOKU_WIN_COUNT, getGomokuWinCount() + 1);
    }

    public boolean isWin() {
        return this.getGameStatue() == WIN;
    }

    public boolean isLost() {
        return this.getGameStatue() == LOSE;
    }

    public void markStatue(boolean isWin) {
        this.setGameStatue(isWin ? WIN : LOSE);
        if (isWin) {
            maid.playSound(InitSounds.GAME_WIN, 1, 1);
        } else {
            maid.playSound(InitSounds.GAME_LOST, 1, 1);
        }
    }

    public void resetStatue() {
        this.setGameStatue(NONE);
    }
}
