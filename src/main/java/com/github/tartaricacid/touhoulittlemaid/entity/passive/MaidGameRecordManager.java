package com.github.tartaricacid.touhoulittlemaid.entity.passive;

import com.github.tartaricacid.touhoulittlemaid.init.InitSounds;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import static com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid.GAME_STATUE;

public class MaidGameRecordManager {

    private static final String GAME_SKILL_TAG = "MaidGameSkillData";
    private static final String GOMOKU = "Gomoku";
    private static final byte NONE = 0, WIN = 1, LOSE = 2;

    private final EntityMaid maid;
    private int gomokuWinCount = 0;

    public MaidGameRecordManager(EntityMaid maid) {
        this.maid = maid;
    }

    void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(GAME_STATUE, (byte) 0);
    }

    void addAdditionalSaveData(ValueOutput output) {
        output.child(GAME_SKILL_TAG).putInt(GOMOKU, gomokuWinCount);
    }

    void readAdditionalSaveData(ValueInput input) {
        input.child(GAME_SKILL_TAG).ifPresent(gameSkill -> gomokuWinCount = gameSkill.getIntOr(GOMOKU, 0));
    }

    void tick() {
        if (getGameStatue() != NONE) {
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
        return gomokuWinCount;
    }

    public void increaseGomokuWinCount() {
        gomokuWinCount++;
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
