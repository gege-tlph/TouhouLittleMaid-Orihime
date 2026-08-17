package com.github.tartaricacid.touhoulittlemaid.datagen.tag;

import com.github.tartaricacid.touhoulittlemaid.init.InitRecipes;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.jspecify.annotations.NonNull;

import java.util.concurrent.CompletableFuture;

/**
 * 把祭坛配方序列化器登记进机械动力的 {@code create:automation_ignore}，
 * <b>禁止机械动力自动化祭坛合成</b>（机械手 / 搅拌机 / 压床 / 动力锯 / 工厂面板都读这张表）。
 *
 * <p>纯数据标签，<b>对机械动力无编译期依赖</b>：标签 id 用裸命名空间构造，条目写成
 * {@code addOptional} 即 {@code "required": false}，机械动力不在场时这份产物是惰性的。</p>
 *
 * <p><b>为什么 26.1.2 上仍然值得补</b>（2026-08-18 实查，别按「Fabric 上没有机械动力」想当然）：
 * 官方 Create Fabric 停在 1.20.1，但社区分叉 <b>Create Fly</b>（{@code ZurrTum/Create-Fly}）
 * 发布了 26.1.2 Fabric 构件，其 {@code fabric.mod.json} 的 <b>mod id 就是 {@code create}</b>，
 * 命名空间因此一致；jar 内 {@code AllRecipeTypes.shouldIgnoreInAutomation(RecipeHolder)} 正是
 * {@code Holder.is(AUTOMATION_IGNORE_TAG)}，全 jar 有 14 个消费者。契约是活的，不是纸面对齐。</p>
 *
 * <p><b>条目 id 机械推导，不写字面量</b>：宿主把序列化器从基准的
 * {@code altar_recipe_serializers} 改名成了 {@code altar_recipe}，照抄基准产物就会写进一个
 * 不存在的 id 而且因为 {@code required: false} 永远不报错——正是「照抄基准的常量」那一类陷阱。
 * 故这里从注册表反查 {@link InitRecipes#ALTAR_RECIPE_SERIALIZER} 的 key。</p>
 */
public class TagRecipeSerializer extends FabricTagsProvider<RecipeSerializer<?>> {
    public static final TagKey<RecipeSerializer<?>> AUTOMATION_IGNORE =
            TagKey.create(Registries.RECIPE_SERIALIZER, Identifier.parse("create:automation_ignore"));

    public TagRecipeSerializer(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, Registries.RECIPE_SERIALIZER, lookupProvider);
    }

    @Override
    protected void addTags(HolderLookup.@NonNull Provider provider) {
        this.builder(AUTOMATION_IGNORE).addOptional(altarRecipeSerializerKey());
    }

    /**
     * 从注册表反查祭坛序列化器的 key —— 改名后产物自动跟着走，不会静默指向旧 id。
     */
    private static ResourceKey<RecipeSerializer<?>> altarRecipeSerializerKey() {
        return BuiltInRegistries.RECIPE_SERIALIZER.getResourceKey(InitRecipes.ALTAR_RECIPE_SERIALIZER)
                .orElseThrow(() -> new IllegalStateException(
                        "Altar recipe serializer is not registered; cannot emit create:automation_ignore"));
    }
}
