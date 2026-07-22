package com.github.tartaricacid.touhoulittlemaid.util.migrate;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;

import java.util.Optional;

/**
 * 实体类型创建与渲染所需的兼容辅助方法。
 */
public final class EntityTypeUtil {
    private EntityTypeUtil() {
    }

    public static Optional<EntityType<?>> byString(String id) {
        Identifier identifier = Identifier.tryParse(id);
        return BuiltInRegistries.ENTITY_TYPE.getOptional(identifier);
    }
}
