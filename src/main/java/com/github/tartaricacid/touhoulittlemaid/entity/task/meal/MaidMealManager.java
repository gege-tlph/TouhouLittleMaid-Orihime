package com.github.tartaricacid.touhoulittlemaid.entity.task.meal;

import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.github.tartaricacid.touhoulittlemaid.api.ILittleMaid;
import com.github.tartaricacid.touhoulittlemaid.api.task.meal.IMaidMeal;
import com.github.tartaricacid.touhoulittlemaid.api.task.meal.MaidMealType;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class MaidMealManager {
    private static Map<MaidMealType, List<IMaidMeal>> ALL_MEAL_TYPES;
    private static List<Predicate<ItemStack>> WORK_MEAL_EXCLUSIONS;

    public MaidMealManager() {
        ALL_MEAL_TYPES = Maps.newEnumMap(MaidMealType.class);
        WORK_MEAL_EXCLUSIONS = Lists.newArrayList();
    }

    public static void init() {
        MaidMealManager manager = new MaidMealManager();
        manager.addMaidMeal(MaidMealType.WORK_MEAL, new DefaultMaidWorkMeal());
        manager.addMaidMeal(MaidMealType.HEAL_MEAL, new DefaultMaidHealSelfMeal());
        manager.addMaidMeal(MaidMealType.HOME_MEAL, new DefaultMaidHomeMeal());
        for (ILittleMaid littleMaid : TouhouLittleMaid.EXTENSIONS) {
            littleMaid.addMaidMeal(manager);
        }
        ALL_MEAL_TYPES = ImmutableMap.copyOf(ALL_MEAL_TYPES);
        WORK_MEAL_EXCLUSIONS = List.copyOf(WORK_MEAL_EXCLUSIONS);
    }

    public void addMaidMeal(MaidMealType type, IMaidMeal maidMeal) {
        ALL_MEAL_TYPES.putIfAbsent(type, Lists.newArrayList());
        ALL_MEAL_TYPES.get(type).add(maidMeal);
    }

    /**
     * Prevents matching stacks from being consumed by the periodic work-meal
     * behavior. Healing meals are deliberately unaffected.
     */
    public void addWorkMealExclusion(Predicate<ItemStack> exclusion) {
        WORK_MEAL_EXCLUSIONS.add(exclusion);
    }

    public static boolean isWorkMealExcluded(ItemStack stack) {
        return WORK_MEAL_EXCLUSIONS.stream().anyMatch(exclusion -> exclusion.test(stack));
    }

    public static List<IMaidMeal> getMaidMeals(MaidMealType type) {
        List<IMaidMeal> maidMeals = ALL_MEAL_TYPES.get(type);
        if (maidMeals == null) {
            return Collections.emptyList();
        }
        return maidMeals;
    }
}
