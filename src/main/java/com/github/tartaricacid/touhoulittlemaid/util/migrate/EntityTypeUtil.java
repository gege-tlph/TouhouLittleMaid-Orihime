package com.github.tartaricacid.touhoulittlemaid.util.migrate;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;

import java.util.Optional;

/**
 * Utility for 1.21.1 → 1.21.11 entity type migration
 */
public final class EntityTypeUtil {
    private EntityTypeUtil() {
    }

    public static Optional<EntityType<?>> byString(String id) {
        Identifier identifier = Identifier.tryParse(id);
        return BuiltInRegistries.ENTITY_TYPE.getOptional(identifier);
    }
}
