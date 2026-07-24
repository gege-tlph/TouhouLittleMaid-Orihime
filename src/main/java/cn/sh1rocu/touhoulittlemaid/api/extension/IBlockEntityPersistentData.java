package cn.sh1rocu.touhoulittlemaid.api.extension;

import net.minecraft.nbt.CompoundTag;

public interface IBlockEntityPersistentData {
    // 保留 ForgeData 键名，以兼容已有方块实体存档。
    String PERSISTENT_DATA = "NeoForgeData";

    default CompoundTag tlm$getPersistentData() {
        throw new RuntimeException();
    }
}
