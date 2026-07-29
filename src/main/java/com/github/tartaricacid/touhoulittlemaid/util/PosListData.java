package com.github.tartaricacid.touhoulittlemaid.util;

import com.google.common.collect.Lists;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;

import java.util.List;

public final class PosListData {
    private final List<BlockPos> data = Lists.newArrayList();

    public ListTag serialize() {
        ListTag nbt = new ListTag();
        for (BlockPos pos : data) {
            // 1.21.11: ListTag.addIntArray 移除 → add(new IntArrayTag(...))（javap 确认）
            nbt.add(new IntArrayTag(new int[]{pos.getX(), pos.getY(), pos.getZ()}));
        }
        return nbt;
    }

    public void deserialize(ListTag nbt) {
        data.clear();
        for (int i = 0; i < nbt.size(); i++) {
            int[] pos = nbt.getIntArray(i).orElse(new int[0]);
            data.add(new BlockPos(pos[0], pos[1], pos[2]));
        }
    }

    public List<BlockPos> getData() {
        return data;
    }

    public void add(BlockPos pos) {
        this.data.add(pos);
    }
}
