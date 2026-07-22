package com.github.tartaricacid.touhoulittlemaid.entity.data;

import cn.sh1rocu.touhoulittlemaid.api.entity.data.TaskDataKey;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public final class MaidTaskDataMaps {
    private static final String TAG_NAME = "MaidTaskDataMaps";
    private final Reference2ObjectMap<TaskDataKey<?>, Optional<?>> dataMaps = new Reference2ObjectOpenHashMap<>();

    @Nullable
    @SuppressWarnings("all")
    public <T> T getData(TaskDataKey<T> dataKey) {
        Optional<T> optional = (Optional<T>) dataMaps.get(dataKey);
        if (optional != null && optional.isPresent()) {
            return optional.get();
        }
        return null;
    }

    public <T> T getOrCreateData(TaskDataKey<T> dataKey, T defaultValue) {
        if (dataMaps.containsKey(dataKey)) {
            T data = this.getData(dataKey);
            if (data != null) {
                return data;
            }
        }
        dataMaps.put(dataKey, Optional.of(defaultValue));
        return defaultValue;
    }

    public <T> void setData(TaskDataKey<?> dataKey, T value) {
        dataMaps.put(dataKey, Optional.of(value));
    }


    @SuppressWarnings("all")
    public void writeSaveData(ValueOutput output) {
        CompoundTag dataTags = new CompoundTag();
        dataMaps.forEach((key, value) -> {
            TaskDataKey dataKey = key;
            value.ifPresent(data -> {
                CompoundTag saveData = dataKey.writeSaveData(data);
                dataTags.put(key.getKey().toString(), saveData);
            });
        });
        output.store(TAG_NAME, CustomData.COMPOUND_TAG_CODEC, dataTags);
    }

    public void readSaveData(ValueInput input) {
        dataMaps.clear();
        input.read(TAG_NAME, CustomData.COMPOUND_TAG_CODEC).ifPresent(dataTags -> {
            for (String key : dataTags.keySet()) {
                TaskDataKey<?> dataKey = TaskDataRegister.getValue(Identifier.parse(key));
                if (dataKey != null) {
                    CompoundTag tag = dataTags.getCompound(key).orElse(new CompoundTag());
                    dataMaps.put(dataKey, Optional.of(dataKey.readSaveData(tag)));
                }
            }
        });
    }

    @SuppressWarnings("all")
    public CompoundTag getUpdateTag() {
        CompoundTag taskTags = new CompoundTag();
        dataMaps.forEach((key, value) -> {
            TaskDataKey dataKey = key;
            value.ifPresent(data -> {
                CompoundTag syncData = dataKey.writeSyncData(data);
                taskTags.put(key.getKey().toString(), syncData);
            });
        });
        return taskTags;
    }

    @Environment(EnvType.CLIENT)
    public void readFromServer(CompoundTag taskTags) {
        dataMaps.clear();
        for (String key : taskTags.keySet()) {
            TaskDataKey<?> dataKey = TaskDataRegister.getValue(Identifier.parse(key));
            if (dataKey != null) {
                CompoundTag tag = taskTags.getCompound(key).orElse(new CompoundTag());
                dataMaps.put(dataKey, Optional.of(dataKey.readSyncData(tag)));
            }
        }
    }
}
