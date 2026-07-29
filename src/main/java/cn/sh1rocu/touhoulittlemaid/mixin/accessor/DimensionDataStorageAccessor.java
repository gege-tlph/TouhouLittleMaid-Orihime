package cn.sh1rocu.touhoulittlemaid.mixin.accessor;

import net.minecraft.world.level.storage.DimensionDataStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.nio.file.Path;

@Mixin(DimensionDataStorage.class)
public interface DimensionDataStorageAccessor {
    // 1.21.11: DimensionDataStorage.dataFolder 字段类型 java.io.File → java.nio.file.Path
    @Accessor("dataFolder")
    Path tlm$getDataFolder();
}
