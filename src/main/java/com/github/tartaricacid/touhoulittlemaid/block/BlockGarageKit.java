package com.github.tartaricacid.touhoulittlemaid.block;

import cn.sh1rocu.touhoulittlemaid.api.extension.IBlock;
import cn.sh1rocu.touhoulittlemaid.util.particle.ParticleUtil;
import com.github.tartaricacid.touhoulittlemaid.client.resource.loader.CustomPackLoader;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.init.InitBlocks;
import com.github.tartaricacid.touhoulittlemaid.init.InitDataComponent;
import com.github.tartaricacid.touhoulittlemaid.init.InitEntities;
import com.github.tartaricacid.touhoulittlemaid.item.ItemGarageKit;
import com.github.tartaricacid.touhoulittlemaid.tileentity.TileEntityGarageKit;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;

import static com.github.tartaricacid.touhoulittlemaid.init.InitDataComponent.MODEL_ID_TAG_NAME;

public class BlockGarageKit extends Block implements EntityBlock, IBlock {
    public static final VoxelShape BLOCK_AABB = Block.box(4, 0, 4, 12, 16, 12);

    @Override
    public boolean tlm$addHitEffects(BlockState state, Level world, HitResult target, ParticleEngine manager) {
        if (target instanceof BlockHitResult blockTarget && world instanceof ClientLevel clientWorld) {
            BlockPos pos = blockTarget.getBlockPos();
            this.crack(clientWorld, pos, Blocks.CLAY.defaultBlockState(), blockTarget.getDirection());
        }
        return true;
    }

    @Environment(EnvType.CLIENT)
    @Override
    public boolean tlm$addDestroyEffects(BlockState state, Level world, BlockPos pos, ParticleEngine manager) {
        // 1.21.11：ParticleEngine.destroy → ClientLevel.addDestroyBlockEffect（origin 语义=黏土粒子）
        if (world instanceof ClientLevel clientLevel) {
            clientLevel.addDestroyBlockEffect(pos, Blocks.CLAY.defaultBlockState());
        }
        return true;
    }

    @Environment(EnvType.CLIENT)
    private void crack(ClientLevel world, BlockPos pos, BlockState state, Direction side) {
        if (state.getRenderShape() != RenderShape.INVISIBLE) {
            int posX = pos.getX();
            int posY = pos.getY();
            int posZ = pos.getZ();
            AABB aabb = state.getShape(world, pos).bounds();
            double x = posX + world.random.nextDouble() * (aabb.maxX - aabb.minX - 0.2) + 0.1 + aabb.minX;
            double y = posY + world.random.nextDouble() * (aabb.maxY - aabb.minY - 0.2) + 0.1 + aabb.minY;
            double z = posZ + world.random.nextDouble() * (aabb.maxZ - aabb.minZ - 0.2) + 0.1 + aabb.minZ;
            if (side == Direction.DOWN) {
                y = posY + aabb.minY - 0.1;
            }
            if (side == Direction.UP) {
                y = posY + aabb.maxY + 0.1;
            }
            if (side == Direction.NORTH) {
                z = posZ + aabb.minZ - 0.1;
            }
            if (side == Direction.SOUTH) {
                z = posZ + aabb.maxZ + 0.1;
            }
            if (side == Direction.WEST) {
                x = posX + aabb.minX - 0.1;
            }
            if (side == Direction.EAST) {
                x = posX + aabb.maxX + 0.1;
            }
            TerrainParticle diggingParticle = new TerrainParticle(world, x, y, z, 0, 0, 0, state);
            Minecraft.getInstance().particleEngine.add(ParticleUtil.updateSprite(diggingParticle, state, pos).setPower(0.2f).scale(0.6f));
        }
    }

    public BlockGarageKit(Identifier id) {
        super(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, id)).sound(SoundType.MUD).strength(1, 2).noOcclusion());
    }

    @Environment(EnvType.CLIENT)
    public static void fillItemCategory(CreativeModeTab.Output items) {
        // 原注释声称依赖被排除的 ServerCustomPackLoader——假前提：origin 用的就是客户端 CustomPackLoader（假 TODO，2026-07-20 还原）
        for (String modelId : CustomPackLoader.MAID_MODELS.getModelIdSet()) {
            ItemStack stack = new ItemStack(InitBlocks.GARAGE_KIT);
            CustomData customData = stack.get(InitDataComponent.MAID_INFO);
            CompoundTag data;
            if (customData == null) {
                data = new CompoundTag();
            } else {
                data = customData.copyTag();
            }
            data.putString(InitDataComponent.ENTITY_ID_TAG_NAME, Objects.requireNonNull(BuiltInRegistries.ENTITY_TYPE.getKey(InitEntities.MAID)).toString());
            data.putString(MODEL_ID_TAG_NAME, modelId);
            // 创造模式物品栏数据需要强制指定 YSM 渲染为空
            data.putBoolean(EntityMaid.IS_YSM_MODEL_TAG, false);
            stack.set(InitDataComponent.MAID_INFO, CustomData.of(data));
            items.accept(stack);
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityGarageKit(pos, state);
    }

    // 1.21.11: BlockBehaviour.onRemove(5参) 移除 → 掉落逻辑迁至 TileEntityGarageKit.preRemoveSideEffects
    //（BE 尚存活可读 extraData；同 BlockAltar/TileEntityAltar 架构）。此处不再需要覆盖。

    @Override
    public void setPlacedBy(Level worldIn, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        this.getGarageKit(worldIn, pos).ifPresent(te -> {
            Direction facing = Direction.SOUTH;
            if (placer != null) {
                facing = placer.getDirection().getOpposite();
            }
            te.setData(facing, ItemGarageKit.getMaidData(stack).copyTag());
        });
    }

    // 1.21.11: getCloneItemStack 增加 boolean includeData 参数（手办始终携带自身数据，与 includeData 无关）
    @Override
    public ItemStack getCloneItemStack(LevelReader world, BlockPos pos, BlockState state, boolean includeData) {
        return getGarageKitFromWorld(world, pos);
    }

    @Override
    public InteractionResult useItemOn(ItemStack itemStack, BlockState state, Level worldIn, BlockPos pos, Player playerIn, InteractionHand hand, BlockHitResult hit) {
        ItemStack stack = playerIn.getItemInHand(hand);
        if (!(worldIn instanceof ServerLevel) || !(stack.getItem() instanceof SpawnEggItem)) {
            return InteractionResult.PASS;
        }
        BlockEntity tile = worldIn.getBlockEntity(pos);
        if (!(tile instanceof TileEntityGarageKit garageKit)) {
            return InteractionResult.PASS;
        }
        EntityType<?> type = ((SpawnEggItem) stack.getItem()).getType(stack);
        Identifier key = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        if (key.equals(BuiltInRegistries.ENTITY_TYPE.getDefaultKey())) {
            return InteractionResult.PASS;
        }

        String id = key.toString();
        CompoundTag data = new CompoundTag();
        data.putString("id", id);

        // 1.21.11: EntityType.create(Level) -> create(Level, EntitySpawnReason)
        Entity entity = type.create(worldIn, EntitySpawnReason.SPAWN_ITEM_USE);
        if (entity instanceof Mob mobEntity) {
            mobEntity.finalizeSpawn((ServerLevel) worldIn, ((ServerLevel) worldIn).getCurrentDifficultyAt(pos), EntitySpawnReason.SPAWN_ITEM_USE, null);
            // 1.21.11: CustomData.loadInto(Entity) 移除；此处 data 仅含 "id"（mob 已由 type.create 生成为该 type）→ loadInto 本就冗余，移除。
            // HEAD 用 mobEntity.addAdditionalSaveData(data) 捕获 mob 数据，但该方法在 Mob 现为 protected（跨包不可调）→
            // 改用 public 的 saveWithoutId(ValueOutput)（捕获 id/pos/附加数据的超集；手办为静态展示，多余字段无害），桥接 merge 回 data。
            TagValueOutput valueOutput = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, ((ServerLevel) worldIn).registryAccess());
            mobEntity.saveWithoutId(valueOutput);
            data.merge(valueOutput.buildResult());
        }

        garageKit.setData(garageKit.getFacing(), data);
        return InteractionResult.SUCCESS_SERVER;
    }

    private ItemStack getGarageKitFromWorld(BlockGetter world, BlockPos pos) {
        ItemStack stack = new ItemStack(InitBlocks.GARAGE_KIT);
        getGarageKit(world, pos).ifPresent(te -> stack.set(InitDataComponent.MAID_INFO, CustomData.of(te.getExtraData())));
        return stack;
    }

    private Optional<TileEntityGarageKit> getGarageKit(BlockGetter world, BlockPos pos) {
        BlockEntity te = world.getBlockEntity(pos);
        if (te instanceof TileEntityGarageKit) {
            return Optional.of((TileEntityGarageKit) te);
        }
        return Optional.empty();
    }

    @Nullable
    public EntityType<?> getType(@Nullable CompoundTag nbt) {
        // 1.21.11: getCompound/getString 现返 Optional
        if (nbt != null) {
            Optional<CompoundTag> entityTag = nbt.getCompound("EntityTag");
            if (entityTag.isPresent()) {
                Optional<String> id = entityTag.get().getString("id");
                if (id.isPresent()) {
                    return EntityType.byString(id.get()).orElse(null);
                }
            }
        }
        return null;
    }

    // 1.21.11: RenderShape.ENTITYBLOCK_ANIMATED 已移除（仅剩 INVISIBLE/MODEL）。
    // 与本仓库既定处理一致（BlockJoy/MaidBed/PicnicMat/SnackCabinet）：移除该覆盖，回落默认 MODEL。

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
        return BLOCK_AABB;
    }
}
