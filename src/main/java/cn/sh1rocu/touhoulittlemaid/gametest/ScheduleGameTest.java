package cn.sh1rocu.touhoulittlemaid.gametest;

import com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.MaidSchedule;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.init.InitEntities;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.schedule.Activity;


public class ScheduleGameTest {
    @GameTest(maxTicks = 100)
    public void maidScheduleDetail(GameTestHelper helper) {
        EntityMaid maid = helper.spawn(InitEntities.MAID, new BlockPos(1, 2, 1));

        // DAY 移位
        check(helper, maid, MaidSchedule.DAY, 0, Activity.WORK);
        check(helper, maid, MaidSchedule.DAY, 6000, Activity.WORK);
        check(helper, maid, MaidSchedule.DAY, 11999, Activity.WORK);
        check(helper, maid, MaidSchedule.DAY, 12000, Activity.IDLE);
        check(helper, maid, MaidSchedule.DAY, 15999, Activity.IDLE);
        check(helper, maid, MaidSchedule.DAY, 16000, Activity.REST);
        check(helper, maid, MaidSchedule.DAY, 23999, Activity.REST);

        // NIGHT 移位
        check(helper, maid, MaidSchedule.NIGHT, 0, Activity.REST);
        check(helper, maid, MaidSchedule.NIGHT, 7999, Activity.REST);
        check(helper, maid, MaidSchedule.NIGHT, 8000, Activity.IDLE);
        check(helper, maid, MaidSchedule.NIGHT, 11999, Activity.IDLE);
        check(helper, maid, MaidSchedule.NIGHT, 12000, Activity.WORK);
        check(helper, maid, MaidSchedule.NIGHT, 23999, Activity.WORK);

        // ALL 天
        check(helper, maid, MaidSchedule.ALL, 0, Activity.WORK);
        check(helper, maid, MaidSchedule.ALL, 12000, Activity.WORK);
        check(helper, maid, MaidSchedule.ALL, 18000, Activity.WORK);

        helper.succeed();
    }


    private void check(GameTestHelper helper, EntityMaid maid, MaidSchedule schedule, long dayTime, Activity expected) {
        maid.setSchedule(schedule);
        helper.getLevel().setDayTime(dayTime);
        Activity actual = maid.getScheduleDetail();
        if (actual != expected) {
            throw helper.assertionException("schedule=%s time=%d expected=%s got=%s", schedule, dayTime, expected, actual);
        }
    }
}
