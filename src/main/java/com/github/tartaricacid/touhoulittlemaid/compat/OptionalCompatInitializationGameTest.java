package com.github.tartaricacid.touhoulittlemaid.compat;

import com.github.tartaricacid.touhoulittlemaid.compat.kaleidoscope.KaleidoscopeCompat;
import com.github.tartaricacid.touhoulittlemaid.entity.task.crop.SpecialCropManager;
import com.github.tartaricacid.touhoulittlemaid.entity.task.meal.MaidMealManager;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

public final class OptionalCompatInitializationGameTest {
    private static final Identifier RICE_SEED =
            Identifier.fromNamespaceAndPath(KaleidoscopeCompat.COOKERY_ID, "rice");
    private static final Identifier WILD_RICE_SEED =
            Identifier.fromNamespaceAndPath(KaleidoscopeCompat.COOKERY_ID, "wild_rice");
    private static final Identifier RICE_CROP =
            Identifier.fromNamespaceAndPath(KaleidoscopeCompat.COOKERY_ID, "rice_crop");
    private static final String TAVERN_ID = "kaleidoscope_tavern";
    private static final Identifier TAVERN_GRAPE =
            Identifier.fromNamespaceAndPath(TAVERN_ID, "grape");
    private static final Identifier TAVERN_GRAPE_CROP =
            Identifier.fromNamespaceAndPath(TAVERN_ID, "grape_crop");

    @GameTest(maxTicks = 100)
    public void optionalCompatDoesNotCorruptForeignItemInitialization(GameTestHelper helper) {
        if (FabricLoader.getInstance().isModLoaded(KaleidoscopeCompat.COOKERY_ID)) {
            verifyCookery(helper);
            assertHashableDefaultStacks(helper, KaleidoscopeCompat.COOKERY_ID);
        }
        if (FabricLoader.getInstance().isModLoaded(TAVERN_ID)) {
            verifyTavern(helper);
            assertHashableDefaultStacks(helper, TAVERN_ID);
        }
        if (FabricLoader.getInstance().isModLoaded("farmersdelight")) {
            assertHashableDefaultStacks(helper, "farmersdelight");
        }
        helper.succeed();
    }

    private static void verifyCookery(GameTestHelper helper) {
        Item riceSeed = BuiltInRegistries.ITEM.getValue(RICE_SEED);
        Item wildRiceSeed = BuiltInRegistries.ITEM.getValue(WILD_RICE_SEED);
        Block riceCrop = BuiltInRegistries.BLOCK.getValue(RICE_CROP);
        assertTrue(helper, riceSeed != null && wildRiceSeed != null && riceCrop != null,
                "Cookery rice registry entries must exist after mod initialization");
        assertTrue(helper, SpecialCropManager.getItemSeedHandlers().containsKey(riceSeed),
                "deferred rice seed handler was not resolved at server start");
        assertTrue(helper, SpecialCropManager.getItemSeedHandlers().containsKey(wildRiceSeed),
                "deferred wild rice seed handler was not resolved at server start");
        assertTrue(helper, SpecialCropManager.getBlockCropHandlers().containsKey(riceCrop),
                "deferred rice crop handler was not resolved at server start");
    }

    private static void verifyTavern(GameTestHelper helper) {
        Item grape = BuiltInRegistries.ITEM.getValue(TAVERN_GRAPE);
        Block grapeCrop = BuiltInRegistries.BLOCK.getValue(TAVERN_GRAPE_CROP);
        assertTrue(helper, grape != null && grapeCrop != null,
                "Tavern grape registry entries must exist after mod initialization");
        assertTrue(helper, MaidMealManager.isWorkMealExcluded(grape.getDefaultInstance()),
                "Tavern work-meal predicate did not resolve after mod initialization");
    }

    private static void assertHashableDefaultStacks(GameTestHelper helper, String modId) {
        List<String> malformedStacks = new ArrayList<>();
        BuiltInRegistries.ITEM.entrySet().forEach(entry -> {
            Identifier id = entry.getKey().identifier();
            if (modId.equals(id.getNamespace())) {
                try {
                    ItemStack.hashItemAndComponents(entry.getValue().getDefaultInstance());
                } catch (RuntimeException malformedComponent) {
                    malformedStacks.add(id + " (" + malformedComponent.getClass().getSimpleName() + ")");
                }
            }
        });
        assertTrue(helper, malformedStacks.isEmpty(),
                modId + " default stacks are malformed after optional compatibility initialization: "
                        + malformedStacks);
    }

    private static void assertTrue(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            helper.fail(message);
        }
    }
}
