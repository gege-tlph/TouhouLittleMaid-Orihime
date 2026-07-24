package com.github.tartaricacid.touhoulittlemaid.entity.task;

import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.github.tartaricacid.touhoulittlemaid.api.task.IFeedTask;
import com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.task.MaidFeedOwnerTask;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.init.InitSounds;
import com.github.tartaricacid.touhoulittlemaid.util.SoundUtil;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.item.consume_effects.ConsumeEffect;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemUseAnimation;

import javax.annotation.Nullable;
import java.util.List;

public class TaskFeedOwner implements IFeedTask {
    public static final Identifier UID = Identifier.fromNamespaceAndPath(TouhouLittleMaid.MOD_ID, "feed");

    @Override
    public Identifier getUid() {
        return UID;
    }

    @Override
    public ItemStack getIcon() {
        return Items.COOKED_BEEF.getDefaultInstance();
    }

    @Override
    public boolean isFood(ItemStack stack, Player owner) {
        if (stack.getItem() == Items.MILK_BUCKET) {
            for (MobEffectInstance effect : owner.getActiveEffects()) {
                if (isHarmfulEffect(effect) && effect.getDuration() > 60) {
                    return true;
                }
            }
            return false;
        }

        if (stack.get(DataComponents.FOOD) != null) {

            Consumable consumable = stack.get(DataComponents.CONSUMABLE);
            if (consumable == null) {
                return true;
            }
            List<MobEffectInstance> effects = new java.util.ArrayList<>();
            for (ConsumeEffect consumeEffect : consumable.onConsumeEffects()) {
                if (consumeEffect instanceof ApplyStatusEffectsConsumeEffect apply) {
                    effects.addAll(apply.effects());
                }
            }
            return effects.isEmpty() || effects.stream().noneMatch(this::isHarmfulEffect);
        }
        return false;
    }

    @Override
    public Priority getPriority(ItemStack stack, Player owner) {
        if (stack.is(Items.MILK_BUCKET)) {
            return Priority.HIGH;
        }


        if (stack.is(Items.HONEY_BOTTLE) && owner.hasEffect(MobEffects.POISON)) {
            return Priority.HIGH;
        }
        ;
        if (stack.is(Items.GOLDEN_APPLE)) {
            if (owner.getHealth() * 2 < owner.getMaxHealth()) {
                return Priority.HIGH;
            } else {
                return Priority.LOWEST;
            }
        }


        if (stack.get(DataComponents.FOOD) != null) {
            FoodData foodData = owner.getFoodData();
            if (!foodData.needsFood()) {
                return Priority.LOWEST;
            }

            FoodProperties food = stack.get(DataComponents.FOOD);
            int heal = 0;
            if (food != null) {
                heal = food.nutrition();
            }
            int hunger = 20 - foodData.getFoodLevel();
            if (heal >= hunger) {
                return Priority.HIGH;
            } else {
                return Priority.LOW;
            }
        }

        return Priority.NONE;
    }

    @Override
    public ItemStack feed(ItemStack stack, Player owner) {
        if (stack.getUseAnimation() == ItemUseAnimation.DRINK) {

            Consumable consumable = stack.get(DataComponents.CONSUMABLE);
            SoundEvent drinkSound = consumable != null ? consumable.sound().value() : SoundEvents.GENERIC_DRINK.value();
            owner.level.playSound(null, owner, drinkSound, SoundSource.NEUTRAL,
                    0.5f, owner.level.getRandom().nextFloat() * 0.1f + 0.9f);
        }
        return stack.getItem().finishUsingItem(stack, owner.level, owner);
    }

    @Nullable
    @Override
    public SoundEvent getAmbientSound(EntityMaid maid) {
        return SoundUtil.environmentSound(maid, InitSounds.MAID_FEED, 0.3f);
    }

    @Override
    public List<Pair<Integer, BehaviorControl<? super EntityMaid>>> createBrainTasks(EntityMaid maid) {
        return Lists.newArrayList(Pair.of(5, new MaidFeedOwnerTask(this, 2, 0.6f)));
    }

    private boolean isHarmfulEffect(MobEffectInstance effect) {
        return effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL;
    }

    @Override
    public String getMaidActionSummary() {
        return "Feed the user when they are hungry";
    }
}
