package com.github.tartaricacid.touhoulittlemaid.entity.ai.targeting;

import com.github.tartaricacid.touhoulittlemaid.api.entity.targeting.IMaidHostilityAdapter;
import com.github.tartaricacid.touhoulittlemaid.api.entity.targeting.MaidHostilityDecision;
import com.github.tartaricacid.touhoulittlemaid.entity.data.inner.AttackListData;
import com.github.tartaricacid.touhoulittlemaid.entity.misc.MonsterType;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.entity.task.TaskAttack;
import com.github.tartaricacid.touhoulittlemaid.entity.task.TaskFeedAnimal;
import com.github.tartaricacid.touhoulittlemaid.init.InitEntities;
import com.github.tartaricacid.touhoulittlemaid.init.InitTaskData;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

import java.util.Map;

@SuppressWarnings("removal")
public class MaidTargetingPolicyGameTest {
    @GameTest(maxTicks = 100)
    public void ownedEntitiesStayProtectedFromOverridesAndDirectHits(GameTestHelper helper) {
        EntityMaid maid = preparedMaid(helper, new TaskFeedAnimal());
        Player wolfOwner = helper.makeMockPlayer(GameType.CREATIVE);
        Wolf wolf = helper.spawn(EntityType.WOLF, new BlockPos(3, 2, 2));
        wolf.tame(wolfOwner);
        maid.setData(InitTaskData.ATTACK_LIST, new AttackListData(Map.of(
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

    @GameTest(maxTicks = 100)
    public void conditionalVanillaHostilityUsesActualTarget(GameTestHelper helper) {
        EntityMaid maid = preparedMaid(helper, new TaskAttack());
        ServerPlayer owner = helper.makeMockServerPlayerInLevel();
        owner.setGameMode(GameType.SURVIVAL);
        maid.tame(owner);
        Zombie unrelated = helper.spawn(EntityType.ZOMBIE, new BlockPos(4, 2, 2));
        Spider spider = helper.spawn(EntityType.SPIDER, new BlockPos(3, 2, 2));

        spider.setTarget(null);
        assertFalse(helper, maid.canAttack(spider), "a calm conditional hostile was attacked");
        spider.setTarget(unrelated);
        assertFalse(helper, maid.canAttack(spider), "an unrelated actual target was treated as party hostility");
        spider.setTarget(owner);
        assertTrue(helper, maid.canAttack(spider), "an actual target of the owner was not recognized");
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
}
