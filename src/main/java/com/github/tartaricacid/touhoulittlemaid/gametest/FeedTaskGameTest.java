package com.github.tartaricacid.touhoulittlemaid.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Consumable;

/**
 * 女仆喂主人这条链路上，饮用音效所依赖的两个 API 事实。
 *
 * <p>代码宿主 {@code origin/26.1} 曾把这段整块注释掉，理由写的是
 * 「getUseAnimation and getDrinkingSound API changed」。javap 26.1.2 实查发现**只对了一半**：
 * {@code ItemStack.getUseAnimation()} 一直都在，变的只有 {@code getDrinkingSound()}——
 * 音效挪进了 {@code Consumable} 组件的 {@code sound()}。</p>
 *
 * <p>为什么是 GameTest 而不是 JUnit：**物品的数据组件在纯 {@code Bootstrap.bootStrap()} 下并不绑定**
 * （实测抛 {@code NullPointerException: Components not bound yet}，抛点在 {@code Holder$Reference}）。
 * 组件要真跑起来的游戏才有，所以这条只能落在 GameTest 层。</p>
 *
 * <p>这里**不测音效响没响**（那要真客户端），测的是那段代码赖以成立的两个前提：
 * ① 判「这是喝的东西」这条路还在；② 音效取得到。
 * 将来 Mojang 再挪一次，这里会红，而不是等到有人发现女仆喂药水没声了。</p>
 */
public class FeedTaskGameTest {
    @GameTest
    public void drinkablesStillReportTheDrinkAnimation(GameTestHelper helper) {
        for (ItemStack stack : new ItemStack[]{
                Items.POTION.getDefaultInstance(),
                Items.MILK_BUCKET.getDefaultInstance()}) {
            if (stack.getUseAnimation() != ItemUseAnimation.DRINK) {
                helper.fail(stack.getItem() + " 不再报 DRINK 动作，女仆喂它时就不会有饮用音效了：实为 "
                        + stack.getUseAnimation());
                return;
            }
        }
        // 反面：吃的东西不许走饮用分支，否则啃苹果会发出咕咚声
        for (ItemStack stack : new ItemStack[]{
                Items.COOKED_BEEF.getDefaultInstance(),
                Items.GOLDEN_APPLE.getDefaultInstance()}) {
            if (stack.getUseAnimation() == ItemUseAnimation.DRINK) {
                helper.fail(stack.getItem() + " 报成了 DRINK 动作，吃东西会错误地播放饮用音效");
                return;
            }
        }
        helper.succeed();
    }

    @GameTest
    public void drinkSoundIsReachableThroughTheConsumableComponent(GameTestHelper helper) {
        for (ItemStack stack : new ItemStack[]{
                Items.POTION.getDefaultInstance(),
                Items.MILK_BUCKET.getDefaultInstance(),
                Items.HONEY_BOTTLE.getDefaultInstance()}) {
            Consumable consumable = stack.get(DataComponents.CONSUMABLE);
            if (consumable == null) {
                helper.fail(stack.getItem() + " 没有 CONSUMABLE 组件，饮用音效无处可取");
                return;
            }
            SoundEvent sound = consumable.sound().value();
            if (sound == null) {
                helper.fail(stack.getItem() + " 的 Consumable.sound() 取不到音效");
                return;
            }
        }
        helper.succeed();
    }
}
