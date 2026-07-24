package com.github.tartaricacid.touhoulittlemaid.entity.passive;

import com.github.tartaricacid.touhoulittlemaid.init.InitEntities;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class MaidSwimmingGameTest {
    private static final BlockPos WATER_POS = new BlockPos(3, 2, 3);
    private static final BlockPos SHORE_POS = new BlockPos(6, 2, 3);

    @GameTest(maxTicks = 120)
    public void shallowWaterAlwaysUsesWadingPose(GameTestHelper helper) {
        preparePool(helper, false);
        EntityMaid maid = helper.spawn(InitEntities.MAID, WATER_POS);
        attachFlyingOwner(helper, maid);
        int[] swimmingTransitions = {0};
        boolean[] previousSwimming = {maid.isSwimming()};

        for (int tick = 1; tick <= 80; tick++) {
            int sample = tick;
            helper.runAtTickTime(tick, () -> {
                BlockPos waterPos = helper.absolutePos(WATER_POS);
                maid.snapTo(waterPos.getX() + 0.5, waterPos.getY(), waterPos.getZ() + 0.5, 0, 0);
                maid.setOnGround((sample & 1) == 0);
                maid.getSwimManager().setWantToSwim(true);
                maid.updateSwimming();

                if (previousSwimming[0] != maid.isSwimming()) {
                    swimmingTransitions[0]++;
                    previousSwimming[0] = maid.isSwimming();
                }
                assertTrue(helper, !maid.getSwimManager().hasSwimmableDepth(),
                        "one-block water was classified as deep water");
                assertStanding(helper, maid, "shallow-water sample %d entered compact swimming".formatted(sample));
                if (sample == 80) {
                    assertTrue(helper, swimmingTransitions[0] == 0,
                            "shallow-water swimming state changed %d times".formatted(swimmingTransitions[0]));
                    maid.addTag("tlm_shallow_wade_mcp_pass");
                }
            });
        }
        helper.runAtTickTime(100, helper::succeed);
    }

    @GameTest(maxTicks = 120)
    public void deepWaterBreathingAndLandingRemainIntact(GameTestHelper helper) {
        preparePool(helper, true);
        EntityMaid maid = helper.spawn(InitEntities.MAID, WATER_POS);
        attachFlyingOwner(helper, maid);

        for (int tick = 1; tick <= 30; tick++) {
            helper.runAtTickTime(tick, () -> {
                snapTo(helper, maid, WATER_POS, false);
                maid.getSwimManager().setGoingToBreath(false);
                maid.getSwimManager().setWantToSwim(true);
                maid.updateSwimming();
                assertTrue(helper, maid.getSwimManager().hasSwimmableDepth(),
                        "two-block water was not classified as swimmable");
                assertSwimming(helper, maid, "deep water did not keep compact swimming");
            });
        }

        helper.runAtTickTime(35, () -> {
            snapTo(helper, maid, WATER_POS, false);
            maid.getSwimManager().setGoingToBreath(true);
            maid.getSwimManager().setWantToSwim(false);
            maid.updateSwimming();
            assertStanding(helper, maid, "breathing transition kept compact swimming");
        });

        helper.runAtTickTime(45, () -> {
            snapTo(helper, maid, WATER_POS, false);
            maid.getSwimManager().setGoingToBreath(false);
            maid.getSwimManager().setWantToSwim(true);
            maid.updateSwimming();
            assertSwimming(helper, maid, "deep swimming did not resume after breathing");
        });

        helper.runAtTickTime(55, () -> {
            snapTo(helper, maid, SHORE_POS, true);
            maid.getSwimManager().setWantToSwim(false);
            maid.getSwimManager().setReadyToLand(true);
            maid.updateSwimming();
            assertStanding(helper, maid, "landing transition kept compact swimming");
            maid.addTag("tlm_deep_swim_mcp_pass");
        });
        helper.runAtTickTime(75, helper::succeed);
    }

    private static void preparePool(GameTestHelper helper, boolean deep) {
        for (int x = 0; x <= 8; x++) {
            for (int z = 0; z <= 6; z++) {
                setBlock(helper, new BlockPos(x, 1, z), Blocks.STONE.defaultBlockState());
                setBlock(helper, new BlockPos(x, 2, z), Blocks.AIR.defaultBlockState());
                setBlock(helper, new BlockPos(x, 3, z), Blocks.AIR.defaultBlockState());
                setBlock(helper, new BlockPos(x, 4, z), Blocks.AIR.defaultBlockState());
            }
        }
        for (int x = 2; x <= 4; x++) {
            for (int z = 2; z <= 4; z++) {
                setBlock(helper, new BlockPos(x, 2, z), Blocks.WATER.defaultBlockState());
                if (deep) {
                    setBlock(helper, new BlockPos(x, 3, z), Blocks.WATER.defaultBlockState());
                }
            }
        }
    }

    @SuppressWarnings("removal")
    private static void attachFlyingOwner(GameTestHelper helper, EntityMaid maid) {
        var owner = helper.makeMockServerPlayerInLevel();
        BlockPos ownerPos = helper.absolutePos(new BlockPos(7, 5, 3));
        owner.snapTo(ownerPos.getX() + 0.5, ownerPos.getY(), ownerPos.getZ() + 0.5, 0, 0);
        owner.getAbilities().mayfly = true;
        owner.getAbilities().flying = true;
        owner.onUpdateAbilities();
        maid.tame(owner);
        maid.setHomeModeEnable(false);
    }

    private static void snapTo(GameTestHelper helper, EntityMaid maid, BlockPos relativePos, boolean onGround) {
        BlockPos pos = helper.absolutePos(relativePos);
        maid.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
        maid.setOnGround(onGround);
    }

    private static void setBlock(GameTestHelper helper, BlockPos relativePos, BlockState state) {
        helper.getLevel().setBlock(helper.absolutePos(relativePos), state, Block.UPDATE_ALL);
    }

    private static void assertSwimming(GameTestHelper helper, EntityMaid maid, String message) {
        assertTrue(helper, maid.isSwimming() && maid.getPose() == Pose.SWIMMING, message);
    }

    private static void assertStanding(GameTestHelper helper, EntityMaid maid, String message) {
        assertTrue(helper, !maid.isSwimming() && maid.getPose() == Pose.STANDING, message);
    }

    private static void assertTrue(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            throw helper.assertionException(message);
        }
    }
}
