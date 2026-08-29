package com.github.tartaricacid.touhoulittlemaid.entity.ai.targeting;

import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.github.tartaricacid.touhoulittlemaid.api.entity.targeting.IMaidHostilityAdapter;
import com.github.tartaricacid.touhoulittlemaid.api.entity.targeting.MaidHostilityDecision;
import com.github.tartaricacid.touhoulittlemaid.entity.data.AttackListData;
import com.github.tartaricacid.touhoulittlemaid.entity.item.EntityBroom;
import com.github.tartaricacid.touhoulittlemaid.entity.item.EntityChair;
import com.github.tartaricacid.touhoulittlemaid.entity.misc.DefaultMonsterType;
import com.github.tartaricacid.touhoulittlemaid.entity.monster.EntityFairy;
import com.github.tartaricacid.touhoulittlemaid.entity.misc.MonsterType;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.entity.task.TaskAttack;
import com.github.tartaricacid.touhoulittlemaid.entity.task.TaskFeedAnimal;
import com.github.tartaricacid.touhoulittlemaid.init.InitDataAttachment;
import com.github.tartaricacid.touhoulittlemaid.init.InitEntities;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

import java.util.Map;
import java.util.Set;

@SuppressWarnings("removal")
public class MaidTargetingPolicyGameTest {
    @GameTest(maxTicks = 100)
    public void ownedEntitiesStayProtectedFromOverridesAndDirectHits(GameTestHelper helper) {
        EntityMaid maid = preparedMaid(helper, new TaskFeedAnimal());
        Player wolfOwner = helper.makeMockPlayer(GameType.CREATIVE);
        Wolf wolf = helper.spawn(EntityType.WOLF, new BlockPos(3, 2, 2));
        wolf.tame(wolfOwner);
        maid.setAttached(InitDataAttachment.ATTACK_LIST, new AttackListData(Map.of(
                BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.WOLF), MonsterType.HOSTILE)));

        float health = wolf.getHealth();
        assertFalse(helper, maid.canAttack(wolf),
                "an owned entity bypassed the task override or configured attack list");
        assertFalse(helper, maid.doHurtTarget(helper.getLevel(), wolf),
                "the final direct-hit guard accepted an owned entity");
        assertEquals(helper, health, wolf.getHealth(),
                "a rejected direct hit changed the owned entity's health");
        helper.succeed();
    }

    @GameTest(maxTicks = 100)
    public void neutralMobRequiresRealAngerAtMaidOrOwner(GameTestHelper helper) {
        EntityMaid maid = preparedMaid(helper, new TaskFeedAnimal());
        ServerPlayer owner = helper.makeMockServerPlayerInLevel();
        owner.setGameMode(GameType.SURVIVAL);
        maid.tame(owner);
        Zombie unrelated = helper.spawn(EntityType.ZOMBIE, new BlockPos(4, 2, 2));
        ZombifiedPiglin piglin = helper.spawn(EntityType.ZOMBIFIED_PIGLIN, new BlockPos(3, 2, 2));
        piglin.setTimeToRemainAngry(400);

        piglin.setPersistentAngerTarget(EntityReference.of(unrelated));
        assertFalse(helper, maid.canAttack(piglin),
                "anger at an unrelated entity was treated as hostility to the maid party");
        piglin.setPersistentAngerTarget(EntityReference.of(owner));
        assertTrue(helper, maid.canAttack(piglin), "anger at the owner was not recognized");
        piglin.setPersistentAngerTarget(EntityReference.of(maid));
        assertTrue(helper, maid.canAttack(piglin), "anger at the maid was not recognized");
        helper.succeed();
    }

    /**
     * 条件敌意适配器读的是「这只怪当前实际盯着谁」。
     *
     * <p>⚠️ 26.1.2 的 {@code Mob.setTarget} 与 {@code getTarget} 都经 {@code asValidTarget} 过滤
     * （javap -c 实查）：创造/旁观模式的玩家一律被过滤成 null。而 GameTest 的 mock 玩家
     * {@code setGameMode(SURVIVAL)} 不生效（实测 {@code isCreative()} 恒 true），
     * 故「蜘蛛盯着主人」这一支在 GameTest 里造不出 fixture——那是测试环境的限制，
     * 不是判据不成立。本用例改用「蜘蛛盯着女仆」这一支覆盖 HOSTILE 路径，
     * 判据（读实际目标而非持久化愤怒）完全相同。</p>
     */
    @GameTest(maxTicks = 100)
    public void conditionalVanillaHostilityUsesActualTarget(GameTestHelper helper) {
        EntityMaid maid = preparedMaid(helper, new TaskAttack());
        ServerPlayer owner = helper.makeMockServerPlayerInLevel();
        maid.tame(owner);
        Zombie unrelated = helper.spawn(EntityType.ZOMBIE, new BlockPos(4, 2, 2));
        Spider spider = helper.spawn(EntityType.SPIDER, new BlockPos(3, 2, 2));

        spider.setTarget(null);
        assertFalse(helper, maid.canAttack(spider), "a calm conditional hostile was attacked");

        spider.setTarget(unrelated);
        // 先证明 fixture 真的装进去了，否则「恒 null → 恒 FRIENDLY」会让下一行以假绿通过
        assertSame(helper, unrelated, spider.getTarget(),
                "fixture failed: an unrelated target was filtered out by asValidTarget");
        assertFalse(helper, maid.canAttack(spider), "an unrelated actual target was treated as party hostility");

        spider.setTarget(maid);
        assertSame(helper, maid, spider.getTarget(),
                "fixture failed: the maid was filtered out by asValidTarget");
        assertTrue(helper, maid.canAttack(spider), "an actual target of the maid was not recognized");
        helper.succeed();
    }

    @GameTest(maxTicks = 100)
    public void externalAdapterCanClassifyAndProtectTargets(GameTestHelper helper) {
        EntityMaid maid = preparedMaid(helper, new TaskAttack());
        Cow cow = helper.spawn(EntityType.COW, new BlockPos(3, 2, 2));
        IMaidHostilityAdapter hostile = (m, target, context) -> target == cow
                ? MaidHostilityDecision.HOSTILE : MaidHostilityDecision.PASS;
        IMaidHostilityAdapter protectedTarget = (m, target, context) -> target == cow
                ? MaidHostilityDecision.PROTECTED : MaidHostilityDecision.PASS;

        try {
            MaidTargetingPolicy.registerAdapter(hostile);
            assertTrue(helper, maid.canAttack(cow), "external hostility classification was ignored");
            MaidTargetingPolicy.registerAdapter(protectedTarget);
            assertFalse(helper, maid.canAttack(cow), "adapter protection did not override hostility");
        } finally {
            MaidTargetingPolicy.unregisterAdapter(protectedTarget);
            MaidTargetingPolicy.unregisterAdapter(hostile);
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 100)
    public void sweepUsesTheSameHardTargetGuard(GameTestHelper helper) {
        EntityMaid maid = preparedMaid(helper, new TaskAttack());
        maid.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
        maid.getAttribute(Attributes.SWEEPING_DAMAGE_RATIO).setBaseValue(1.0);
        Zombie primary = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 2, 2));
        Player wolfOwner = helper.makeMockPlayer(GameType.CREATIVE);
        Wolf protectedWolf = helper.spawn(EntityType.WOLF, new BlockPos(2, 2, 3));
        protectedWolf.tame(wolfOwner);

        float protectedHealth = protectedWolf.getHealth();
        assertTrue(helper, maid.doHurtTarget(helper.getLevel(), primary),
                "the valid primary target was not hit");
        assertEquals(helper, protectedHealth, protectedWolf.getHealth(),
                "sweep damage bypassed the owned-entity hard guard");
        helper.succeed();
    }

    /**
     * 妖精必须走默认分类。基准 {@code IAttackTask.canAttack} 的**注释**写「排除…本模组的实体」，
     * 但它的**代码**只排除 {@code AbstractEntityFromItem}；集中化目标规则时照抄了注释，
     * 于是多出一条按命名空间的硬安全否决，把妖精一并保护了——女仆从此打不了妖精。
     *
     * <p>{@code EntityFairy extends Monster implements Enemy}，默认分类就是 HOSTILE。</p>
     */
    @GameTest(maxTicks = 100)
    public void hostileFairyUsesDefaultTargetClassification(GameTestHelper helper) {
        EntityMaid maid = preparedMaid(helper, new TaskAttack());
        EntityFairy fairy = helper.spawn(InitEntities.FAIRY, new BlockPos(3, 2, 2));

        assertSame(helper, MonsterType.HOSTILE, DefaultMonsterType.getMonsterType(fairy),
                "a fairy was no longer classified hostile by the default rules");
        assertTrue(helper, maid.canAttack(fairy),
                "a mod-owned hostile entity was denied purely by its namespace");
        helper.succeed();
    }

    /**
     * 删掉命名空间兜底后，其余本模组实体仍须受保护。
     *
     * <p>判据必须落在 {@link MaidTargetingPolicy#canAttackOnOwnerCommand}——技能 / LLM 工具
     * 那条路**只**受硬安全约束。用 {@code maid.canAttack} 是空转的：坐垫与扫帚
     * （{@code AbstractEntityFromItem}，既非 Enemy 也非 TamableAnimal）默认分类是 NEUTRAL、
     * 女仆（{@code TamableAnimal}）是 FRIENDLY，把整段硬安全删光它照样绿。</p>
     *
     * <p>自带下限断言：本模组的 {@code LivingEntity} 恰好是这四个。将来新增本模组生物时
     * 这条会红，逼下一个人回来决定它该不该受保护——命名空间兜底没了，不会再有人替它兜。</p>
     */
    @GameTest(maxTicks = 100)
    public void modOwnedEntitiesStayProtectedWithoutTheNamespaceFallback(GameTestHelper helper) {
        EntityMaid maid = preparedMaid(helper, new TaskAttack());
        EntityMaid otherMaid = helper.spawn(InitEntities.MAID, new BlockPos(3, 2, 4));
        EntityChair chair = helper.spawn(InitEntities.CHAIR, new BlockPos(3, 2, 3));
        EntityBroom broom = helper.spawn(InitEntities.BROOM, new BlockPos(3, 2, 5));
        EntityFairy fairy = helper.spawn(InitEntities.FAIRY, new BlockPos(3, 2, 2));

        assertFalse(helper, MaidTargetingPolicy.canAttackOnOwnerCommand(maid, otherMaid),
                "an owner command was allowed to target another maid");
        assertFalse(helper, MaidTargetingPolicy.canAttackOnOwnerCommand(maid, chair),
                "an owner command was allowed to target a chair");
        assertFalse(helper, MaidTargetingPolicy.canAttackOnOwnerCommand(maid, broom),
                "an owner command was allowed to target a broom");
        assertTrue(helper, MaidTargetingPolicy.canAttackOnOwnerCommand(maid, fairy),
                "an owner command could not target a fairy");

        Set<String> expectedLiving = Set.of("maid", "chair", "fairy", "broom");
        Set<String> actualLiving = modOwnedLivingEntityPaths(helper);
        if (!expectedLiving.equals(actualLiving)) {
            throw helper.assertionException(
                    "the set of mod-owned living entities changed: expected=%s got=%s."
                            + " Decide whether the new one needs an explicit hard-safety guard,"
                            + " now that the namespace fallback is gone",
                    expectedLiving, actualLiving);
        }
        helper.succeed();
    }

    /**
     * 按「实例是不是 {@code LivingEntity}」枚举，而不是按属性注册表——后者是我算影响面时
     * 用的另一条通道，拿它当判据就成了自证。这里只构造不入世，构造失败一律报错而非跳过，
     * 否则「枚举不到」会伪装成「没有」。
     */
    private static Set<String> modOwnedLivingEntityPaths(GameTestHelper helper) {
        Set<String> paths = new java.util.HashSet<>();
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
            if (id == null || !TouhouLittleMaid.MOD_ID.equals(id.getNamespace())) {
                continue;
            }
            Entity probe;
            try {
                probe = type.create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
            } catch (Exception e) {
                throw helper.assertionException(
                        "could not probe mod entity %s, so the living-entity set is unknown: %s", id, e);
            }
            if (probe == null) {
                throw helper.assertionException(
                        "mod entity %s produced no probe instance, so the living-entity set is unknown", id);
            }
            if (probe instanceof LivingEntity) {
                paths.add(id.getPath());
            }
            probe.discard();
        }
        return paths;
    }

    private static EntityMaid preparedMaid(GameTestHelper helper,
                                            com.github.tartaricacid.touhoulittlemaid.api.task.IMaidTask task) {
        EntityMaid maid = helper.spawn(InitEntities.MAID, new BlockPos(1, 2, 2));
        maid.setTask(task);
        return maid;
    }

    private static void assertTrue(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            throw helper.assertionException(message);
        }
    }

    private static void assertFalse(GameTestHelper helper, boolean condition, String message) {
        assertTrue(helper, !condition, message);
    }

    private static void assertEquals(GameTestHelper helper, float expected, float actual, String message) {
        if (Float.compare(expected, actual) != 0) {
            throw helper.assertionException("%s: expected=%s got=%s", message, expected, actual);
        }
    }

    private static void assertSame(GameTestHelper helper, Object expected, Object actual, String message) {
        if (expected != actual) {
            throw helper.assertionException("%s: expected=%s got=%s", message, expected, actual);
        }
    }
}
