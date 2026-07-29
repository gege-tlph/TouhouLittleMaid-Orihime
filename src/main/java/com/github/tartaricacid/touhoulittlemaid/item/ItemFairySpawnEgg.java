package com.github.tartaricacid.touhoulittlemaid.item;

import com.github.tartaricacid.touhoulittlemaid.entity.monster.EntityFairy;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public class ItemFairySpawnEgg extends SpawnEggItem {
    public ItemFairySpawnEgg(Identifier id) {
        // B5: SpawnEggItem 构造器改为仅 Properties；实体类型经 Properties.spawnEgg 绑定
        super(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id)).spawnEgg(EntityFairy.TYPE));
    }

    @Override
    public Optional<Mob> spawnOffspringFromSpawnEgg(Player player, Mob parentMob, EntityType<? extends Mob> type, ServerLevel level, Vec3 pos, ItemStack stack) {
        if (!this.spawnsEntity(stack, type)) {
            return Optional.empty();
        }
        if (!(parentMob instanceof EntityFairy)) {
            return Optional.empty();
        }
        // B5: EntityType.create(Level) → create(Level, EntitySpawnReason)
        EntityFairy fairy = EntityFairy.TYPE.create(level, net.minecraft.world.entity.EntitySpawnReason.SPAWN_ITEM_USE);
        if (fairy == null) {
            return Optional.empty();
        }
        fairy.finalizeSpawn(level, level.getCurrentDifficultyAt(fairy.blockPosition()), EntitySpawnReason.SPAWN_ITEM_USE, null);
        fairy.setBaby(true);
        if (!fairy.isBaby()) {
            return Optional.empty();
        }
        // 1.21.11: Entity.moveTo(...) -> snapTo(...)
        fairy.snapTo(pos.x(), pos.y(), pos.z(), 0.0F, 0.0F);
        level.addFreshEntityWithPassengers(fairy);
        fairy.setCustomName(stack.get(DataComponents.CUSTOM_NAME));
        if (!player.isCreative()) {
            stack.shrink(1);
        }
        return Optional.of(fairy);
    }
}