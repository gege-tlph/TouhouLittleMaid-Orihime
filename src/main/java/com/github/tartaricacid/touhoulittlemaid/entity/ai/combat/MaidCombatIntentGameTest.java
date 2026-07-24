package com.github.tartaricacid.touhoulittlemaid.entity.ai.combat;

import com.github.tartaricacid.touhoulittlemaid.api.entity.targeting.MaidTargetingContext;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.MaidConfigManager;
import com.github.tartaricacid.touhoulittlemaid.init.InitEntities;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

@SuppressWarnings("removal")
public class MaidCombatIntentGameTest {
    @GameTest(maxTicks = 100)
    public void finalDamageSignalsRespectOffSelfAndProtectPolicies(GameTestHelper helper) {
        OwnedMaid party = ownedMaid(helper, new BlockPos(1, 2, 1), new BlockPos(2, 2, 1));
        Zombie attacker = threat(helper, new BlockPos(3, 2, 1));

        party.maid.getCombatManager().setResponsePolicy(MaidCombatResponsePolicy.OFF);
        attacker.doHurtTarget(helper.getLevel(), party.maid);
        assertFalse(helper, party.maid.isEmergencyCombatActive(),
                "OFF policy reacted to successful maid damage");

        party.maid.getCombatManager().setResponsePolicy(MaidCombatResponsePolicy.SELF_DEFENSE);
        helper.runAtTickTime(21, () -> {
            attacker.doHurtTarget(helper.getLevel(), party.owner);
            assertFalse(helper, party.maid.isEmergencyCombatActive(),
                    "SELF_DEFENSE policy reacted to owner damage");
            attacker.doHurtTarget(helper.getLevel(), party.maid);
            assertContext(helper, party.maid, MaidTargetingContext.SELF_DEFENSE,
                    "SELF_DEFENSE policy ignored successful maid damage");
            helper.succeed();
        });
    }

    @GameTest(maxTicks = 100)
    public void hostileOwnerAttackEstablishesProtection(GameTestHelper helper) {
        OwnedMaid offenseParty = ownedMaid(helper, new BlockPos(1, 2, 1), new BlockPos(2, 2, 1));
        Zombie attacked = threat(helper, new BlockPos(3, 2, 1));
        offenseParty.owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
        offenseParty.owner.attack(attacked);
        assertContext(helper, offenseParty.maid, MaidTargetingContext.PROTECT_OWNER,
                "first successful owner hit on a hostile target was not assisted");
        helper.succeed();
    }

    @GameTest(maxTicks = 100)
    public void successfulOwnerDamageEstablishesProtection(GameTestHelper helper) {
        OwnedMaid defenseParty = ownedMaid(helper, new BlockPos(1, 2, 1), new BlockPos(2, 2, 1));
        Zombie attacker = threat(helper, new BlockPos(3, 2, 1));
        assertTrue(helper, defenseParty.owner.hurtServer(
                        helper.getLevel(), defenseParty.owner.damageSources().mobAttack(attacker), 3.0F),
                "owner-damage fixture was not accepted by the server damage pipeline");
        assertContext(helper, defenseParty.maid, MaidTargetingContext.PROTECT_OWNER,
                "successful damage to the owner was not protected");
        helper.succeed();
    }

    @GameTest(maxTicks = 100)
    public void peacefulOwnerTargetRequiresTwoIndependentSuccessfulHits(GameTestHelper helper) {
        OwnedMaid party = ownedMaid(helper, new BlockPos(1, 2, 1), new BlockPos(2, 2, 1));
        Cow cow = helper.spawn(EntityType.COW, new BlockPos(3, 2, 1));
        party.owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));

        party.owner.attack(cow);
        MaidCombatDamageListener.onAfterDamage(
                cow, party.owner.damageSources().playerAttack(party.owner), 2, 2, false);
        assertFalse(helper, party.maid.isEmergencyCombatActive(),
                "same-tick duplicate damage callbacks counted twice");

        helper.runAtTickTime(21, () -> {
            party.owner.attack(cow);
            assertContext(helper, party.maid, MaidTargetingContext.PROTECT_OWNER,
                    "second independent successful owner hit did not confirm the peaceful target");
            helper.succeed();
        });
    }

    @GameTest(maxTicks = 100)
    public void neutralWaitsForRealHostility(GameTestHelper helper) {
        OwnedMaid neutralParty = ownedMaid(helper, new BlockPos(1, 2, 1), new BlockPos(2, 2, 1));
        ZombifiedPiglin piglin = helper.spawn(EntityType.ZOMBIFIED_PIGLIN, new BlockPos(3, 2, 1));
        MaidCombatDamageListener.onAfterDamage(
                piglin, neutralParty.owner.damageSources().playerAttack(neutralParty.owner),
                2, 2, false);
        assertFalse(helper, neutralParty.maid.isEmergencyCombatActive(),
                "calm NeutralMob was accepted from owner damage alone");

        piglin.setNoAi(true);
        piglin.setTimeToRemainAngry(400);
        piglin.setPersistentAngerTarget(EntityReference.of(neutralParty.maid));
        piglin.setTarget(neutralParty.maid);

        helper.runAtTickTime(21, () -> {
            assertContext(helper, neutralParty.maid, MaidTargetingContext.PROTECT_OWNER,
                    "pending neutral target did not activate after real anger");
            helper.succeed();
        });
    }

    @GameTest(maxTicks = 100)
    public void ownedTargetNeverQualifies(GameTestHelper helper) {
        OwnedMaid protectedParty = ownedMaid(helper, new BlockPos(1, 2, 1), new BlockPos(2, 2, 1));
        ServerPlayer wolfOwner = helper.makeMockServerPlayerInLevel();
        Wolf wolf = helper.spawn(EntityType.WOLF, new BlockPos(3, 2, 1));
        wolf.tame(wolfOwner);
        protectedParty.owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
        protectedParty.owner.attack(wolf);

        helper.runAtTickTime(21, () -> {
            protectedParty.owner.attack(wolf);
            assertFalse(helper, protectedParty.maid.isEmergencyCombatActive(),
                    "two owner hits bypassed owned-entity hard protection");
            helper.succeed();
        });
    }

    @GameTest(maxTicks = 100)
    public void indirectOwnerDamageSourcesDoNotCreateIntent(GameTestHelper helper) {
        OwnedMaid party = ownedMaid(helper, new BlockPos(1, 2, 1), new BlockPos(2, 2, 1));
        Cow cow = helper.spawn(EntityType.COW, new BlockPos(3, 2, 1));
        Wolf pet = helper.spawn(EntityType.WOLF, new BlockPos(3, 2, 3));
        pet.tame(party.owner);

        MaidCombatDamageListener.onAfterDamage(
                cow, party.owner.damageSources().thorns(party.owner), 2, 2, false);
        MaidCombatDamageListener.onAfterDamage(
                cow, party.owner.damageSources().onFire(), 2, 2, false);
        MaidCombatDamageListener.onAfterDamage(
                cow, party.owner.damageSources().fall(), 2, 2, false);
        MaidCombatDamageListener.onAfterDamage(
                cow, party.owner.damageSources().indirectMagic(pet, party.owner), 2, 2, false);
        MaidCombatDamageListener.onAfterDamage(
                cow, party.owner.damageSources().mobAttack(pet), 2, 2, false);
        assertFalse(helper, party.maid.isEmergencyCombatActive(),
                "indirect owner damage source created a combat target");

        Zombie zombie = threat(helper, new BlockPos(4, 2, 1));
        Arrow arrow = new Arrow(EntityType.ARROW, helper.getLevel());
        arrow.setOwner(party.owner);
        MaidCombatDamageListener.onAfterDamage(
                zombie, party.owner.damageSources().arrow(arrow, party.owner), 2, 2, false);
        assertContext(helper, party.maid, MaidTargetingContext.PROTECT_OWNER,
                "direct owner projectile was not counted");
        helper.succeed();
    }

    @GameTest(maxTicks = 130)
    public void peacefulOwnerConfirmationExpiresAfterOneHundredTicks(GameTestHelper helper) {
        OwnedMaid party = ownedMaid(helper, new BlockPos(1, 2, 1), new BlockPos(2, 2, 1));
        Cow cow = helper.spawn(EntityType.COW, new BlockPos(3, 2, 1));
        MaidCombatDamageListener.onAfterDamage(
                cow, party.owner.damageSources().playerAttack(party.owner), 2, 2, false);

        helper.runAtTickTime(101, () -> {
            MaidCombatDamageListener.onAfterDamage(
                    cow, party.owner.damageSources().playerAttack(party.owner), 2, 2, false);
            assertFalse(helper, party.maid.isEmergencyCombatActive(),
                    "expired peaceful-target confirmation was replayed");
            party.maid.addTag("tlm_combat_intent_mcp_pass");
            helper.runAtTickTime(120, helper::succeed);
        });
    }

    @GameTest(maxTicks = 120)
    public void localityStickinessAndUnreachableTimeoutBoundTheTarget(GameTestHelper helper) {
        OwnedMaid party = ownedMaid(helper, new BlockPos(1, 2, 1), new BlockPos(2, 2, 1));
        Zombie first = threat(helper, new BlockPos(3, 2, 1));
        Zombie replacement = threat(helper, new BlockPos(4, 2, 1));
        assertTrue(helper, party.maid.getCombatManager().beginEmergency(
                first, MaidTargetingContext.PROTECT_OWNER), "local protection target was rejected");
        assertFalse(helper, party.maid.getCombatManager().beginEmergency(
                replacement, MaidTargetingContext.PROTECT_OWNER),
                "fresh target replaced a valid sticky target immediately");

        party.maid.getBrain().setMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE,
                helper.getLevel().getGameTime() - 60L);
        party.maid.getCombatManager().tick(helper.getLevel());
        assertFalse(helper, party.maid.isEmergencyCombatActive(),
                "60-tick unreachable target was retained");

        Zombie distant = threat(helper, new BlockPos(25, 2, 1));
        assertFalse(helper, party.maid.getCombatManager().beginEmergency(
                distant, MaidTargetingContext.SELF_DEFENSE),
                "target outside the 16-block local range was accepted");

        assertTrue(helper, party.maid.getCombatManager().beginEmergency(
                first, MaidTargetingContext.PROTECT_OWNER), "fresh local target was rejected");
        BlockPos farOwnerPos = helper.absolutePos(new BlockPos(30, 2, 1));
        party.owner.snapTo(farOwnerPos.getX() + 0.5, farOwnerPos.getY(),
                farOwnerPos.getZ() + 0.5, 0, 0);
        helper.runAtTickTime(45, () -> {
            assertFalse(helper, party.maid.isEmergencyCombatActive(),
                    "owner-range stickiness exceeded 40 ticks");
            helper.succeed();
        });
    }

    @GameTest(maxTicks = 100)
    public void responsePolicyPersistsAndOldOrInvalidValuesDefaultToProtectOwner(GameTestHelper helper) {
        EntityMaid source = helper.spawn(InitEntities.MAID, new BlockPos(1, 2, 1));
        source.getCombatManager().setResponsePolicy(MaidCombatResponsePolicy.SELF_DEFENSE);

        CompoundTag saved = saveMaid(helper, source);
        assertTrue(helper,
                "self_defense".equals(saved.getCompoundOrEmpty("MaidSubConfig")
                        .getStringOr("CombatResponsePolicy", "")),
                "response policy was not saved using its stable serialized name");
        assertTrue(helper,
                loadMaid(helper, saved).getCombatManager().getResponsePolicy()
                        == MaidCombatResponsePolicy.SELF_DEFENSE,
                "saved response policy did not survive entity reload");

        CompoundTag oldSave = saved.copy();
        CompoundTag oldSubConfig = oldSave.getCompoundOrEmpty("MaidSubConfig");
        oldSubConfig.remove("CombatResponsePolicy");
        oldSave.put("MaidSubConfig", oldSubConfig);
        assertTrue(helper,
                loadMaid(helper, oldSave).getCombatManager().getResponsePolicy()
                        == MaidCombatResponsePolicy.PROTECT_OWNER,
                "old save without response policy did not default to Protect Owner");

        CompoundTag invalidSave = saved.copy();
        CompoundTag invalidSubConfig = invalidSave.getCompoundOrEmpty("MaidSubConfig");
        invalidSubConfig.putString("CombatResponsePolicy", "future_invalid_value");
        invalidSave.put("MaidSubConfig", invalidSubConfig);
        assertTrue(helper,
                loadMaid(helper, invalidSave).getCombatManager().getResponsePolicy()
                        == MaidCombatResponsePolicy.PROTECT_OWNER,
                "invalid saved response policy did not safely default to Protect Owner");
        helper.succeed();
    }

    @GameTest(maxTicks = 100)
    public void nbtRestoreDoesNotCreatePlayerCommandSuppression(GameTestHelper helper) {
        EntityMaid source = helper.spawn(InitEntities.MAID, new BlockPos(1, 2, 1));
        CompoundTag saved = saveMaid(helper, source);
        EntityMaid restored = loadMaid(helper, saved);
        Zombie attacker = threat(helper, new BlockPos(2, 2, 1));

        assertTrue(helper, restored.getCombatManager().beginEmergency(
                        attacker, MaidTargetingContext.SELF_DEFENSE),
                "NBT restoration was misclassified as an explicit player command");
        helper.succeed();
    }

    @GameTest(maxTicks = 100)
    public void subConfigPolicyCommandClearsEmergencyAndSyncsTrackedValue(GameTestHelper helper) {
        OwnedMaid party = ownedMaid(helper, new BlockPos(1, 2, 1), new BlockPos(2, 2, 1));
        Zombie threat = threat(helper, new BlockPos(3, 2, 1));
        assertTrue(helper, party.maid.getCombatManager().beginEmergency(
                threat, MaidTargetingContext.SELF_DEFENSE), "emergency fixture was rejected");

        MaidConfigManager.SyncNetwork settings = party.maid.getConfigManager().getSyncNetwork();
        settings.setCombatResponsePolicy(MaidCombatResponsePolicy.OFF);
        MaidConfigManager.SyncNetwork.handle(settings, party.maid);

        assertTrue(helper,
                party.maid.getCombatManager().getResponsePolicy() == MaidCombatResponsePolicy.OFF,
                "sub-config command did not update the tracked response policy");
        assertFalse(helper, party.maid.isEmergencyCombatActive(),
                "explicit response-policy command did not clear active emergency combat");
        party.maid.addTag("tlm_t5_policy_mcp_pass");
        helper.runAtTickTime(50, helper::succeed);
    }

    private static CompoundTag saveMaid(GameTestHelper helper, EntityMaid maid) {
        TagValueOutput output = TagValueOutput.createWithContext(
                ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
        maid.saveWithoutId(output);
        return output.buildResult();
    }

    private static EntityMaid loadMaid(GameTestHelper helper, CompoundTag tag) {
        EntityMaid maid = InitEntities.MAID.create(helper.getLevel(), EntitySpawnReason.LOAD);
        if (maid == null) {
            throw helper.assertionException("failed to construct maid reload fixture");
        }
        maid.load(TagValueInput.create(
                ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), tag));
        return maid;
    }

    private static OwnedMaid ownedMaid(GameTestHelper helper, BlockPos maidPos, BlockPos ownerPos) {
        EntityMaid maid = helper.spawn(InitEntities.MAID, maidPos);
        maid.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
        ServerPlayer owner = helper.makeMockServerPlayerInLevel();
        owner.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
        owner.setGameMode(GameType.SURVIVAL);
        BlockPos absoluteOwnerPos = helper.absolutePos(ownerPos);
        owner.snapTo(absoluteOwnerPos.getX() + 0.5, absoluteOwnerPos.getY(),
                absoluteOwnerPos.getZ() + 0.5, 0, 0);
        maid.tame(owner);
        return new OwnedMaid(maid, owner);
    }

    private static Zombie threat(GameTestHelper helper, BlockPos pos) {
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, pos);
        zombie.setNoAi(true);
        return zombie;
    }

    private static void assertContext(GameTestHelper helper, EntityMaid maid,
                                      MaidTargetingContext expected, String message) {
        if (!maid.isEmergencyCombatActive()
                || maid.getCombatManager().getTargetingContext() != expected) {
            throw helper.assertionException(message);
        }
    }

    private static void assertTrue(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            throw helper.assertionException(message);
        }
    }

    private static void assertFalse(GameTestHelper helper, boolean condition, String message) {
        assertTrue(helper, !condition, message);
    }

    private record OwnedMaid(EntityMaid maid, ServerPlayer owner) {
    }
}
