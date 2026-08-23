package com.github.tartaricacid.touhoulittlemaid.loot;

import com.github.tartaricacid.touhoulittlemaid.datapack.BoardStateData;
import com.github.tartaricacid.touhoulittlemaid.datapack.pojo.BoardStateRecord;
import com.github.tartaricacid.touhoulittlemaid.item.ItemBoardState;
import com.github.tartaricacid.touhoulittlemaid.util.IdentifierUtil;
import com.github.tartaricacid.touhoulittlemaid.util.WeightedPicker;
import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import java.util.List;

/**
 * 战利品函数：给残局道具随机填一份预设棋谱。
 *
 * <p>按棋谱的 {@code tags} 过滤、再按 {@code weight} 加权抽取，
 * 所以同一张战利品表可以只放「图书馆」类的残局，也可以放别的分类。</p>
 *
 * <p>写法对新基：宿主的 {@code SetInitMaidOwnerFunction} 用的是
 * {@code codec()} 返回 {@code MapCodec}，而行为基准那边是 {@code getType()} 返回
 * {@code LootItemFunctionType}。26.1.2 走前者，照宿主写。</p>
 */
public class RandomBoardStateFunction extends LootItemConditionalFunction {
    public static final Identifier ID = IdentifierUtil.modLoc("board_state_randomly");
    public static final MapCodec<RandomBoardStateFunction> CODEC = RecordCodecBuilder.mapCodec(
            instance -> commonFields(instance).and(
                    Codec.STRING.listOf().fieldOf("tags").forGetter(function -> function.tags)
            ).apply(instance, RandomBoardStateFunction::new)
    );

    /** 会被选中的残局所需具有的 tag */
    private final List<String> tags;

    protected RandomBoardStateFunction(List<LootItemCondition> predicates, List<String> tags) {
        super(predicates);
        this.tags = tags;
    }

    public static Builder create() {
        return new Builder();
    }

    private boolean checkTags(BoardStateRecord record) {
        for (String tag : tags) {
            if (record.tags().contains(tag)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected ItemStack run(ItemStack stack, LootContext context) {
        // 任何一步取不到东西都原样返回：宁可给一个空白残局道具，也不要让整张战利品表炸掉
        List<BoardStateRecord> records = BoardStateData.getRecordsByItem(stack);
        if (records.isEmpty() || this.tags.isEmpty()) {
            return stack;
        }
        List<BoardStateRecord> matchedRecords = records.stream().filter(this::checkTags).toList();
        if (matchedRecords.isEmpty()) {
            return stack;
        }
        BoardStateRecord selected = WeightedPicker.pickRandom(matchedRecords, BoardStateRecord::weight);
        if (selected == null) {
            return stack;
        }
        BoardStateRecord.Display display = selected.display();
        ItemBoardState.setState(stack, selected.data(), display.description(), display.author());
        return stack;
    }

    @Override
    public MapCodec<? extends LootItemConditionalFunction> codec() {
        return CODEC;
    }

    public static class Builder extends LootItemConditionalFunction.Builder<Builder> {
        private final List<String> tags = Lists.newArrayList();

        @Override
        protected Builder getThis() {
            return this;
        }

        public Builder addTag(String tag) {
            this.tags.add(tag);
            return this;
        }

        @Override
        public LootItemFunction build() {
            return new RandomBoardStateFunction(this.getConditions(), this.tags);
        }
    }
}
