package com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.task;

import com.github.tartaricacid.touhoulittlemaid.api.entity.targeting.MaidTargetingContext;
import com.github.tartaricacid.touhoulittlemaid.entity.ai.combat.MaidCombatManager;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.api.task.IMaidTask;
import com.github.tartaricacid.touhoulittlemaid.api.task.IRangedAttackTask;
import com.github.tartaricacid.touhoulittlemaid.entity.task.TaskAttack;
import com.github.tartaricacid.touhoulittlemaid.entity.task.TaskBowAttack;
import com.github.tartaricacid.touhoulittlemaid.entity.task.TaskCrossBowAttack;
import com.github.tartaricacid.touhoulittlemaid.init.InitEntities;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * 威胁响应的远程行为验收。
 *
 * <p>用户报告：「女仆手拿着弓/弩/枪还要上去手打敌人，看起来很蠢」。改之前
 * {@code MaidMeleeAttack} 的闸门是 {@code emergency || !isHoldingUsableProjectileWeapon(maid)}
 * ——应战时无条件放行近战，因为那时应战活动里根本没有远程行为可选。</p>
 *
 * <p>拦的**不是近战本身**——敌人贴到脸上时端着弓也得还手（既有用例
 * {@code usableBowCanMeleeOnlyThroughEmergencyLayer} 钉的就是这条），拦的是「主动走过去」。
 * 故这里钉三件事：① 端着能用的远程武器时不再设 WALK_TARGET（站定射击）；
 * ② 没弹药时照旧走过去打近战（不许端着空弓站桩）；③ 近战武器不受影响。
 * 判据落在 WALK_TARGET 这个可观测量上，不看代码文本。</p>
 */
public class MaidRangedEmergencyGameTest {
    @GameTest(maxTicks = 100)
    public void holdingLoadedBowBlocksMeleeDuringEmergency(GameTestHelper helper) {
        EntityMaid maid = armedMaid(helper, new BlockPos(1, 2, 1), Items.BOW, Items.ARROW, new TaskBowAttack());
        Zombie target = threat(helper, new BlockPos(3, 2, 1));

        assertTrue(helper, MaidCombatManager.isHoldingUsableRangedWeapon(maid),
                "有弓有箭时共用判据应认定为可用远程武器");
        assertTrue(helper, maid.getCombatManager().beginEmergency(target, MaidTargetingContext.SELF_DEFENSE),
                "未能进入应战");
        rememberVisibleTarget(helper, maid, target);

        assertFalse(helper, walksTowardTarget(helper, maid, target),
                "端着上了箭的弓，应战时不应再主动走过去贴脸");
        helper.succeed();
    }

    @GameTest(maxTicks = 100)
    public void emptyBowFallsBackToMelee(GameTestHelper helper) {
        // 只给弓不给箭：canUseNonMeleeWeapon 为 false → 判据为 false → 近战放行，
        // 否则她会端着一把射不出去的弓站在原地挨打
        EntityMaid maid = armedMaid(helper, new BlockPos(1, 2, 1), Items.BOW, null, new TaskBowAttack());
        Zombie target = threat(helper, new BlockPos(6, 2, 1));

        assertFalse(helper, MaidCombatManager.isHoldingUsableRangedWeapon(maid),
                "没有箭时不应被判为可用远程武器");
        assertTrue(helper, maid.getCombatManager().beginEmergency(target, MaidTargetingContext.SELF_DEFENSE),
                "未能进入应战");
        rememberVisibleTarget(helper, maid, target);

        assertTrue(helper, walksTowardTarget(helper, maid, target),
                "没有弹药时应照旧走过去打近战，而不是端着空弓站桩");
        helper.succeed();
    }

    @GameTest(maxTicks = 100)
    public void meleeWeaponStillFightsInMelee(GameTestHelper helper) {
        EntityMaid maid = armedMaid(helper, new BlockPos(1, 2, 1), Items.IRON_SWORD, null, new TaskAttack());
        Zombie target = threat(helper, new BlockPos(6, 2, 1));

        assertFalse(helper, MaidCombatManager.isHoldingUsableRangedWeapon(maid),
                "铁剑不是远程武器");
        assertTrue(helper, maid.getCombatManager().beginEmergency(target, MaidTargetingContext.SELF_DEFENSE),
                "未能进入应战");
        rememberVisibleTarget(helper, maid, target);

        assertTrue(helper, walksTowardTarget(helper, maid, target),
                "拿近战武器时应战仍应走过去打——这条防的是「为了修远程把近战也堵死」");
        helper.succeed();
    }

    @GameTest(maxTicks = 100)
    public void loadedCrossbowAlsoBlocksMelee(GameTestHelper helper) {
        EntityMaid maid = armedMaid(helper, new BlockPos(1, 2, 1), Items.CROSSBOW, Items.ARROW, new TaskCrossBowAttack());
        Zombie target = threat(helper, new BlockPos(3, 2, 1));

        assertTrue(helper, MaidCombatManager.isHoldingUsableRangedWeapon(maid),
                "弩加箭同样应被判为可用远程武器");
        assertTrue(helper, maid.getCombatManager().beginEmergency(target, MaidTargetingContext.SELF_DEFENSE),
                "未能进入应战");
        rememberVisibleTarget(helper, maid, target);

        assertFalse(helper, walksTowardTarget(helper, maid, target), "端着上了箭的弩不应再主动贴脸");
        helper.succeed();
    }

    @GameTest(maxTicks = 100)
    public void nonRangedTaskStillFiresBecauseImplementationIsResolvedFromTheWeapon(GameTestHelper helper) {
        // 委派给当前工作任务，任务不对就是空操作，于是「农场女仆手持弓有箭」在应战里只能冲上去肉搏，
        // 而同样情形下手持枪却会站定开枪（枪走 TaCZ 自己的射击链，不受任务限制）——两者不一致。
        // 解绑后按**手里的武器**去找能开火的实现，两条路终于对齐。
        EntityMaid maid = armedMaid(helper, new BlockPos(1, 2, 1), Items.BOW, Items.ARROW, new TaskAttack());
        Zombie target = threat(helper, new BlockPos(6, 2, 1));

        assertTrue(helper, MaidCombatManager.isHoldingUsableRangedWeapon(maid),
                "工作任务不是远程任务，但手里是上了箭的弓——解绑后应判为可用远程武器");
        assertTrue(helper, maid.getCombatManager().beginEmergency(target, MaidTargetingContext.SELF_DEFENSE),
                "未能进入应战");
        rememberVisibleTarget(helper, maid, target);

        assertFalse(helper, walksTowardTarget(helper, maid, target),
                "既然开得出火，就不该再主动贴脸");

        // 判据落在**真的射出了箭**上，不看代码文本：performRangedAttack 空操作时场上不会有箭
        int before = arrowsNear(helper, maid);
        maid.performRangedAttack(target, 1.0F);
        int after = arrowsNear(helper, maid);
        assertTrue(helper, after > before,
                "非远程任务下 performRangedAttack 没射出任何箭——实现解析没生效");
        helper.succeed();
    }

    @GameTest(maxTicks = 100)
    public void currentRangedTaskWinsEvenWhenItsOwnIsWeaponRejectsTheHeldItem(GameTestHelper helper) {
        // MaidShootTargetTask 的进入条件只要求 ProjectileWeaponItem，不要求具体是弓。
        // 所以「弓兵模式手持弩」在基准里是能开火的（用 TaskBowAttack 的实现）。
        // 解析口若对当前任务额外要求 isWeapon，这种情形会改用别的实现，
        // 而手持**无人认领的模组远程武器**时更会解析为 null → 直接不开火。
        // 故当前任务只要是远程任务就直接用它，扫注册表只在任务不是远程任务时兜底。
        EntityMaid maid = armedMaid(helper, new BlockPos(1, 2, 1), Items.CROSSBOW, Items.ARROW, new TaskBowAttack());

        IRangedAttackTask resolved = IRangedAttackTask.resolveImplementation(maid, maid.getMainHandItem());
        assertTrue(helper, resolved instanceof TaskBowAttack,
                "当前任务是远程任务时必须直接用它，实得：" + (resolved == null ? "null" : resolved.getClass().getSimpleName()));
        helper.succeed();
    }

    /**
     * 跑一次应战走位行为，回答「她会不会主动走向目标」。
     *
     * <p>判据落在 WALK_TARGET 这个可观测量上，而不是「代码里有没有那一行」：
     * 设了 = 她要贴过去打；没设 = 她站定（把 WALK_TARGET 让给远程走位与射击行为）。</p>
     */
    /**
     * 坐着的女仆挨打后不许再平移走位——但照旧原地还手。
     *
     * <p>根因不在「没判坐姿」，而在<b>走位这条路绕过了移动闸</b>：它不设 {@code WALK_TARGET}，
     * 直接写 MoveControl（{@code getMoveControl().strafe(...)}），所以兄弟任务
     * {@code MaidEmergencyWalkToTarget} 用的 {@code canBrainMoving()} 管不到它。
     * 修法补的是<b>同一个</b>移动闸，于是坐 / 骑乘 / 睡觉 / 被拴一并挡住，
     * 而不是新造一个「坐姿」判据——后者只堵住今天这一种。</p>
     *
     * <p><b>对照组不能省</b>：站着必须返回 true。只断言「坐着不走位」时，任务因为任何别的
     * 原因起不来（拿错武器、没目标）也会绿——那正是本仓库反复栽的「零覆盖恒绿」。
     * 另外坐姿是<b>写进去再读出来</b>的 fixture，必须显式断言写成功了：setter 带过滤器是常态，
     * 写入被静默丢弃会让「期望 false」的断言集体假绿。</p>
     */
    @GameTest(maxTicks = 100)
    public void sittingMaidDoesNotStrafeDuringEmergency(GameTestHelper helper) {
        EntityMaid standing = armedMaid(helper, new BlockPos(1, 2, 1), Items.BOW, Items.ARROW, new TaskBowAttack());
        Zombie target = threat(helper, new BlockPos(3, 2, 1));
        rememberVisibleTarget(helper, standing, target);
        assertFalse(helper, standing.isMaidInSittingPose(), "对照组不该是坐姿");
        assertTrue(helper, strafes(helper, standing, target),
                "对照组：站着且端着上了箭的弓时，走位行为本该起得来——起不来说明这条用例什么都没测");

        EntityMaid sitting = armedMaid(helper, new BlockPos(1, 2, 3), Items.BOW, Items.ARROW, new TaskBowAttack());
        Zombie sitTarget = threat(helper, new BlockPos(3, 2, 3));
        rememberVisibleTarget(helper, sitting, sitTarget);
        // 走玩家指令那支会设重入抑制窗口，故用 without-player-command
        sitting.setInSittingPoseWithoutPlayerCommand(true);
        assertTrue(helper, sitting.isMaidInSittingPose(),
                "坐姿没写进去——setter 被过滤时，下面那条期望 false 的断言会恒真假绿");
        assertFalse(helper, sitting.canBrainMoving(), "坐姿下 canBrainMoving 应为假");

        assertFalse(helper, strafes(helper, sitting, sitTarget), "坐着的女仆不该再平移走位");
        helper.succeed();
    }

    /** 跑一次应战走位行为，回答「她会不会平移走位」。接线与 MaidBrain 应战表那一项同参。 */
    private static boolean strafes(GameTestHelper helper, EntityMaid maid, Zombie target) {
        maid.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, target);
        maid.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        // 射程与走位速度只影响 tick 里的进退阈值，不影响进入条件；射程仍照 MaidBrain 取同一常量
        var strafing = new MaidAttackStrafingAnyItemTask(
                MaidCombatManager::isHoldingUsableRangedWeapon,
                (float) MaidCombatManager.LOCAL_PROTECTION_RANGE, 0.5f);
        return strafing.tryStart(helper.getLevel(), maid, helper.getLevel().getGameTime());
    }

    private static boolean walksTowardTarget(GameTestHelper helper, EntityMaid maid, Zombie target) {
        maid.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, target);
        maid.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        var walk = MaidEmergencyWalkToTarget.create(0.7f);
        walk.tryStart(helper.getLevel(), maid, helper.getLevel().getGameTime());
        return maid.getBrain().hasMemoryValue(MemoryModuleType.WALK_TARGET);
    }

    private static EntityMaid armedMaid(GameTestHelper helper, BlockPos pos,
                                        net.minecraft.world.item.Item weapon,
                                        net.minecraft.world.item.Item ammo,
                                        IMaidTask task) {
        EntityMaid maid = helper.spawn(InitEntities.MAID, pos);
        maid.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(weapon));
        if (ammo != null) {
            maid.getMaidInv().setStackInSlot(0, new ItemStack(ammo, 16));
        }
        // 必须用 without-player-command 那支：setTask 走玩家指令路径，会设 20 tick 的
        // 重入抑制窗口，窗口内 beginEmergency 一律返回 false，用例会以「未能进入应战」假红
        maid.setTaskWithoutPlayerCommand(task);
        return maid;
    }

    private static int arrowsNear(GameTestHelper helper, EntityMaid maid) {
        return helper.getLevel().getEntitiesOfClass(AbstractArrow.class,
                maid.getBoundingBox().inflate(24)).size();
    }

    private static Zombie threat(GameTestHelper helper, BlockPos pos) {
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, pos);
        zombie.setNoAi(true);
        return zombie;
    }

    private static void rememberVisibleTarget(GameTestHelper helper, EntityMaid maid, Zombie target) {
        maid.getBrain().setMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES,
                new NearestVisibleLivingEntities(helper.getLevel(), maid, List.of(target)));
    }

    private static void assertTrue(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            throw helper.assertionException(message);
        }
    }

    private static void assertFalse(GameTestHelper helper, boolean condition, String message) {
        assertTrue(helper, !condition, message);
    }
}
