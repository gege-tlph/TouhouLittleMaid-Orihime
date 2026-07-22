package cn.sh1rocu.touhoulittlemaid.util.forge;

import cn.sh1rocu.touhoulittlemaid.api.event.FarmlandTrampleEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

/**
 * 来自 NeoForge
 */
public class CommonHooks {
    public static Optional<BlockPos> isLivingOnLadder(BlockState state, Level level, BlockPos pos, LivingEntity entity) {
        boolean isSpectator = (entity instanceof Player && entity.isSpectator());
        if (isSpectator)
            return Optional.empty();

        return isLadder(state, level, pos, entity) ? Optional.of(pos) : Optional.empty();
    }

    public static boolean isLadder(BlockState state, LevelReader level, BlockPos pos, LivingEntity entity) {
        return state.is(BlockTags.CLIMBABLE);
    }

    public static boolean onFarmlandTrample(Level level, BlockPos pos, BlockState state, float fallDistance, Entity entity) {
        FarmlandTrampleEvent event = new FarmlandTrampleEvent(level, pos, state, fallDistance, entity);
        FarmlandTrampleEvent.CALLBACK.invoker().post(event);
        return !event.isCanceled();
    }
}
