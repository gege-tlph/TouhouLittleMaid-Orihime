package cn.sh1rocu.touhoulittlemaid.gametest;

import com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.MaidSchedule;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.init.InitEntities;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.schedule.Activity;

/**
 * P7a runtime verification (2026-07-17): assert {@link EntityMaid#getScheduleDetail()} maps
 * (MaidSchedule mode, day time) to the correct {@link Activity}, replicating the HEAD
 * MAID_{DAY,NIGHT,ALL}_SHIFT_SCHEDULES keyframes and staying in lock-step with the datapack
 * {@code timeline/maid_schedule.json}. This is the automated, observable proof for the fix that
 * un-stubbed the DAY/NIGHT branches (previously hard-coded to IDLE in the 1.21.11 port).
 *
 * DAY:   0 -> WORK, 12000 -> IDLE, 16000 -> REST
 * NIGHT: 0 -> REST, 8000 -> IDLE, 12000 -> WORK
 * ALL:   always WORK
 */
public class ScheduleGameTest {
    @GameTest(maxTicks = 100)
    public void maidScheduleDetail(GameTestHelper helper) {
        EntityMaid maid = helper.spawn(InitEntities.MAID, new BlockPos(1, 2, 1));

        // DAY shift
        check(helper, maid, MaidSchedule.DAY, 0, Activity.WORK);
        check(helper, maid, MaidSchedule.DAY, 6000, Activity.WORK);
        check(helper, maid, MaidSchedule.DAY, 11999, Activity.WORK);
        check(helper, maid, MaidSchedule.DAY, 12000, Activity.IDLE);
        check(helper, maid, MaidSchedule.DAY, 15999, Activity.IDLE);
        check(helper, maid, MaidSchedule.DAY, 16000, Activity.REST);
        check(helper, maid, MaidSchedule.DAY, 23999, Activity.REST);

        // NIGHT shift
        check(helper, maid, MaidSchedule.NIGHT, 0, Activity.REST);
        check(helper, maid, MaidSchedule.NIGHT, 7999, Activity.REST);
        check(helper, maid, MaidSchedule.NIGHT, 8000, Activity.IDLE);
        check(helper, maid, MaidSchedule.NIGHT, 11999, Activity.IDLE);
        check(helper, maid, MaidSchedule.NIGHT, 12000, Activity.WORK);
        check(helper, maid, MaidSchedule.NIGHT, 23999, Activity.WORK);

        // ALL day
        check(helper, maid, MaidSchedule.ALL, 0, Activity.WORK);
        check(helper, maid, MaidSchedule.ALL, 12000, Activity.WORK);
        check(helper, maid, MaidSchedule.ALL, 18000, Activity.WORK);

        helper.succeed();
    }

    // NOTE (2026-07-17): Mechanism ① — the datapack timeline -> EnvironmentAttribute<Activity> that
    // MaidUpdateActivityFromSchedule samples to drive the brain — was also probed here, but the
    // EnvironmentAttributeSystem samples on its own time-interpolated clock (with daylight-cycle drift),
    // so it does not respond deterministically to setDayTime jumps inside a gametest (observed values were
    // correct across full runs but flaky point-to-point). It loads without error (verified via RCON /reload)
    // and returned correct activities in passing runs; its live per-tick behavior is best confirmed by
    // client observation (P6, Jade/TOP activity display). Only the deterministic pure-function query
    // getScheduleDetail() is asserted automatically here.

    private void check(GameTestHelper helper, EntityMaid maid, MaidSchedule schedule, long dayTime, Activity expected) {
        maid.setSchedule(schedule);
        helper.getLevel().setDayTime(dayTime);
        Activity actual = maid.getScheduleDetail();
        if (actual != expected) {
            throw helper.assertionException("schedule=%s time=%d expected=%s got=%s", schedule, dayTime, expected, actual);
        }
    }
}
