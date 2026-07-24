package com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.task;

import com.github.tartaricacid.touhoulittlemaid.api.task.IFarmTask;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.entity.task.TaskCocoa;
import com.github.tartaricacid.touhoulittlemaid.entity.task.TaskMelon;
import com.github.tartaricacid.touhoulittlemaid.entity.task.TaskNormalFarm;
import com.github.tartaricacid.touhoulittlemaid.entity.task.TaskSugarCane;
import com.github.tartaricacid.touhoulittlemaid.init.InitEntities;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

public class MaidFarmNavigationGameTest {
    private static final BlockPos MAID_POS = new BlockPos(1, 2, 2);
    private static final BlockPos BASE_POS = new BlockPos(5, 1, 2);
    private static final BlockPos HOME_EDGE_BASE_POS = new BlockPos(2, 1, 2);
    private static final BlockPos HOME_EDGE_CENTER_POS = new BlockPos(3, 2, 2);
    private static final BlockPos HOME_EDGE_MAID_POS = new BlockPos(1, 2, 2);
    private static final BlockPos NO_NEIGHBOR_BASE_POS = new BlockPos(3, 1, 2);
    private static final BlockPos NO_NEIGHBOR_MAID_POS = new BlockPos(2, 2, 2);

    @GameTest(maxTicks = 100)
    public void normalCropUsesReachableNeighbor(GameTestHelper helper) {
        prepareFloor(helper);
        setBlock(helper, BASE_POS, Blocks.FARMLAND.defaultBlockState());
        setBlock(helper, BASE_POS.above(), Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 7));

        EntityMaid maid = prepareMaid(helper);
        verifyHarvest(helper, maid, new TaskNormalFarm(), false, "tlm_farm_normal_mcp_pass");
        BlockState harvested = getBlock(helper, BASE_POS.above());
        assertTrue(helper, harvested.is(Blocks.WHEAT) && harvested.getValue(CropBlock.AGE) < 7,
                "normal crop was not harvested from its adjacent stand node");
        succeedAfterMcpSamplingWindow(helper);
    }

    @GameTest(maxTicks = 100)
    public void sugarCaneUsesReachableNeighbor(GameTestHelper helper) {
        prepareFloor(helper);
        setBlock(helper, BASE_POS.below(), Blocks.DIRT.defaultBlockState());
        setBlock(helper, BASE_POS, Blocks.SUGAR_CANE.defaultBlockState());
        setBlock(helper, BASE_POS.above(), Blocks.SUGAR_CANE.defaultBlockState());

        EntityMaid maid = prepareMaid(helper);
        verifyHarvest(helper, maid, new TaskSugarCane(), false, "tlm_farm_sugar_cane_mcp_pass");
        assertTrue(helper, getBlock(helper, BASE_POS.above()).isAir(),
                "upper sugar cane was not harvested from its adjacent stand node");
        succeedAfterMcpSamplingWindow(helper);
    }

    @GameTest(maxTicks = 100)
    public void cocoaUsesReachableNeighbor(GameTestHelper helper) {
        prepareFloor(helper);
        setBlock(helper, BASE_POS.above().relative(Direction.EAST), Blocks.JUNGLE_LOG.defaultBlockState());
        setBlock(helper, BASE_POS.above(), Blocks.COCOA.defaultBlockState()
                .setValue(CocoaBlock.AGE, 2)
                .setValue(HorizontalDirectionalBlock.FACING, Direction.EAST));

        EntityMaid maid = prepareMaid(helper);
        verifyHarvest(helper, maid, new TaskCocoa(), true, "tlm_farm_cocoa_mcp_pass");
        assertTrue(helper, getBlock(helper, BASE_POS.above()).isAir(),
                "cocoa was not harvested from its adjacent stand node");
        succeedAfterMcpSamplingWindow(helper);
    }

    @GameTest(maxTicks = 100)
    public void melonUsesReachableNeighbor(GameTestHelper helper) {
        prepareFloor(helper);
        setBlock(helper, BASE_POS.above(), Blocks.MELON.defaultBlockState());
        setBlock(helper, BASE_POS.above().relative(Direction.WEST), Blocks.ATTACHED_MELON_STEM.defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.EAST));

        EntityMaid maid = prepareMaid(helper);
        verifyHarvest(helper, maid, new TaskMelon(), true, "tlm_farm_melon_mcp_pass");
        assertTrue(helper, getBlock(helper, BASE_POS.above()).isAir(),
                "melon was not harvested from its adjacent stand node");
        succeedAfterMcpSamplingWindow(helper);
    }

    @GameTest(maxTicks = 100)
    public void pumpkinUsesReachableNeighbor(GameTestHelper helper) {
        prepareFloor(helper);
        setBlock(helper, BASE_POS.above(), Blocks.PUMPKIN.defaultBlockState());
        setBlock(helper, BASE_POS.above().relative(Direction.WEST), Blocks.ATTACHED_PUMPKIN_STEM.defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.EAST));

        EntityMaid maid = prepareMaid(helper);
        verifyHarvest(helper, maid, new TaskMelon(), true, "tlm_farm_pumpkin_mcp_pass");
        assertTrue(helper, getBlock(helper, BASE_POS.above()).isAir(),
                "pumpkin was not harvested from its adjacent stand node");
        succeedAfterMcpSamplingWindow(helper);
    }

    @GameTest(maxTicks = 100)
    public void homeEdgeCropUsesInsideNeighbor(GameTestHelper helper) {
        prepareFloor(helper);
        setBlock(helper, HOME_EDGE_BASE_POS, Blocks.FARMLAND.defaultBlockState());
        setBlock(helper, HOME_EDGE_BASE_POS.above(), Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 7));

        EntityMaid maid = prepareMaid(helper, HOME_EDGE_MAID_POS, HOME_EDGE_CENTER_POS, 2);
        verifyHarvestAt(helper, maid, new TaskNormalFarm(), false,
                "tlm_farm_home_edge_mcp_pass", HOME_EDGE_BASE_POS);
        BlockState harvested = getBlock(helper, HOME_EDGE_BASE_POS.above());
        assertTrue(helper, harvested.is(Blocks.WHEAT) && harvested.getValue(CropBlock.AGE) < 7,
                "home-edge crop was not harvested from an in-home stand node");
        succeedAfterMcpSamplingWindow(helper);
    }

    @GameTest(maxTicks = 100)
    public void homeEdgeCropWithoutInsideNeighborIsSkipped(GameTestHelper helper) {
        prepareFloor(helper);
        setBlock(helper, NO_NEIGHBOR_BASE_POS, Blocks.FARMLAND.defaultBlockState());
        setBlock(helper, NO_NEIGHBOR_BASE_POS.above(),
                Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 7));

        EntityMaid maid = prepareMaid(helper, NO_NEIGHBOR_MAID_POS, NO_NEIGHBOR_BASE_POS, 1);
        MaidFarmMoveTask moveTask = new MaidFarmMoveTask(new TaskNormalFarm(), 0.6F);
        moveTask.start(helper.getLevel(), maid, helper.getLevel().getGameTime());

        assertTrue(helper, maid.getBrain().getMemory(InitEntities.TARGET_POS).isEmpty(),
                "farm installed an interaction target without an in-home stand node");
        assertTrue(helper, maid.getBrain().getMemory(MemoryModuleType.WALK_TARGET).isEmpty(),
                "farm installed an out-of-home walk target");
        maid.addTag("tlm_farm_home_no_neighbor_mcp_pass");
        succeedAfterMcpSamplingWindow(helper);
    }

    @SuppressWarnings("removal")
    private static EntityMaid prepareMaid(GameTestHelper helper) {
        return prepareMaid(helper, MAID_POS, MAID_POS, 16);
    }

    @SuppressWarnings("removal")
    private static EntityMaid prepareMaid(GameTestHelper helper, BlockPos maidPos, BlockPos homePos, int homeRadius) {
        EntityMaid maid = helper.spawn(InitEntities.MAID, maidPos);
        maid.tame(helper.makeMockServerPlayerInLevel());
        maid.setHomeTo(helper.absolutePos(homePos), homeRadius);
        maid.setHomeModeEnable(true);
        return maid;
    }

    private static void verifyHarvest(GameTestHelper helper, EntityMaid maid, IFarmTask task,
                                      boolean surrounding, String passTag) {
        verifyHarvestAt(helper, maid, task, surrounding, passTag, BASE_POS);
    }

    private static void verifyHarvestAt(GameTestHelper helper, EntityMaid maid, IFarmTask task,
                                        boolean surrounding, String passTag, BlockPos relativeBasePos) {
        MaidFarmMoveTask moveTask = surrounding
                ? new MaidFarmSurroundingMoveTask(task, 0.6F)
                : new MaidFarmMoveTask(task, 0.6F);
        moveTask.start(helper.getLevel(), maid, helper.getLevel().getGameTime());

        BlockPos expectedBase = helper.absolutePos(relativeBasePos);
        BlockPos interactionBase = maid.getBrain().getMemory(InitEntities.TARGET_POS)
                .orElseThrow(() -> helper.assertionException("farm interaction target was not installed"))
                .currentBlockPosition();
        assertTrue(helper, interactionBase.equals(expectedBase),
                "farm selected the wrong interaction block: expected=%s got=%s".formatted(expectedBase, interactionBase));

        WalkTarget walkTarget = maid.getBrain().getMemory(MemoryModuleType.WALK_TARGET)
                .orElseThrow(() -> helper.assertionException("farm walk target was not installed"));
        BlockPos standPos = walkTarget.getTarget().currentBlockPosition();
        assertTrue(helper, standPos.getX() != interactionBase.getX() || standPos.getZ() != interactionBase.getZ(),
                "farm tried to stand in the crop column");
        assertTrue(helper, walkTarget.getCloseEnoughDist() == 0,
                "farm stand node must use exact navigation tolerance");
        assertTrue(helper, MaidFarmMoveTask.isWithinInteractionRange(standPos, interactionBase,
                        task.getCloseEnoughDist()),
                "selected stand node is outside the task interaction distance");
        assertTrue(helper, maid.isWithinHome(standPos),
                "selected stand node is outside the maid home boundary: %s".formatted(standPos));

        BlockState standState = helper.getLevel().getBlockState(standPos);
        assertTrue(helper, standState.getCollisionShape(helper.getLevel(), standPos).isEmpty(),
                "selected stand node is occupied: %s".formatted(standState));

        maid.snapTo(standPos.getX() + 0.5, standPos.getY(), standPos.getZ() + 0.5, 0, 0);
        MaidFarmPlantTask useTask = new MaidFarmPlantTask(task);
        assertTrue(helper, useTask.checkExtraStartConditions(helper.getLevel(), maid),
                "farm use task rejected its selected stand node");
        useTask.start(helper.getLevel(), maid, helper.getLevel().getGameTime());
        maid.addTag(passTag);
    }

    private static void prepareFloor(GameTestHelper helper) {
        for (int x = 0; x <= 8; x++) {
            for (int z = 0; z <= 4; z++) {
                setBlock(helper, new BlockPos(x, 1, z), Blocks.STONE.defaultBlockState());
                setBlock(helper, new BlockPos(x, 2, z), Blocks.AIR.defaultBlockState());
                setBlock(helper, new BlockPos(x, 3, z), Blocks.AIR.defaultBlockState());
            }
        }
    }

    private static void setBlock(GameTestHelper helper, BlockPos relativePos, BlockState state) {
        helper.getLevel().setBlock(helper.absolutePos(relativePos), state, Block.UPDATE_ALL);
    }

    private static BlockState getBlock(GameTestHelper helper, BlockPos relativePos) {
        return helper.getLevel().getBlockState(helper.absolutePos(relativePos));
    }

    private static void succeedAfterMcpSamplingWindow(GameTestHelper helper) {
        helper.runAtTickTime(20, helper::succeed);
    }

    private static void assertTrue(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            throw helper.assertionException(message);
        }
    }
}
