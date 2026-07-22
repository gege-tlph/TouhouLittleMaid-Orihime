package com.github.tartaricacid.touhoulittlemaid.tileentity;

import com.github.tartaricacid.touhoulittlemaid.item.ItemModelSwitcher;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import cn.sh1rocu.touhoulittlemaid.api.extension.IBlockEntityPersistentData;
import com.github.tartaricacid.touhoulittlemaid.init.InitBlocks;
import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

public class TileEntityModelSwitcher extends BlockEntity implements IBlockEntityPersistentData {
    public static final BlockEntityType<TileEntityModelSwitcher> TYPE = FabricBlockEntityTypeBuilder.create(TileEntityModelSwitcher::new, InitBlocks.MODEL_SWITCHER).build();
    public static final String INFO_LIST = "info_list";
    public static final String ENTITY_UUID = "entity_uuid";
    public static final String LIST_INDEX = "list_index";
    private List<ModeInfo> infoList = Lists.newArrayList();
    private boolean isPowered;
    private UUID uuid;
    private int index;

    public TileEntityModelSwitcher(BlockPos pWorldPosition, BlockState pBlockState) {
        super(TYPE, pWorldPosition, pBlockState);
    }

    @Override
    protected void saveAdditional(ValueOutput output){
        ListTag listTag = new ListTag();
        for (ModeInfo info : infoList) {
            listTag.add(info.serialize());
        }
        tlm$getPersistentData().put(INFO_LIST, listTag);
        if (this.uuid != null) {
            tlm$getPersistentData().putIntArray(ENTITY_UUID, UUIDUtil.uuidToIntArray(this.uuid));
        }
        tlm$getPersistentData().putInt(LIST_INDEX, this.index);
        super.saveAdditional(output);
    }

    @Override
    public void loadAdditional(ValueInput input){
        super.loadAdditional(input);
        infoList.clear();
        ListTag listTag = tlm$getPersistentData().getListOrEmpty(INFO_LIST);
        for (int i = 0; i < listTag.size(); i++) {
            ModeInfo info = new ModeInfo();
            info.deserialize(listTag.getCompoundOrEmpty(i));
            infoList.add(info);
        }
        tlm$getPersistentData().getIntArray(ENTITY_UUID)
                .map(UUIDUtil::uuidFromIntArray).ifPresent(u -> this.uuid = u);
        this.index = tlm$getPersistentData().getIntOr(LIST_INDEX, 0);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider pRegistries) {
        return this.saveWithoutMetadata(pRegistries);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Nullable
    public UUID getUuid() {
        return uuid;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
        this.refresh();
    }

    public boolean isPowered() {
        return isPowered;
    }

    public void setPowered(boolean powered) {
        isPowered = powered;
    }

    @Nullable
    public ModeInfo getModelInfo() {
        if (0 <= index && index < infoList.size()) {
            return infoList.get(this.index);
        }
        return null;
    }

    public List<ModeInfo> getInfoList() {
        return infoList;
    }

    public void setInfoList(List<ModeInfo> infoList) {
        this.infoList = infoList;
        this.refresh();
    }

    public void refresh() {
        this.setChanged();
        if (level != null) {
            BlockState state = level.getBlockState(worldPosition);
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_ALL);
        }
    }

    public static class ModeInfo {
        public static final StreamCodec<ByteBuf, ModeInfo> MODE_INFO_STREAM_CODEC = StreamCodec.composite(
                Identifier.STREAM_CODEC,
                ModeInfo::getModelId,
                ByteBufCodecs.STRING_UTF8,
                ModeInfo::getText,
                Direction.STREAM_CODEC,
                ModeInfo::getDirection,
                ModeInfo::new
        );
        private Identifier modelId;
        private String text;
        private Direction direction;

        public ModeInfo() {
        }

        public ModeInfo(Identifier modelId, String text, Direction direction) {
            this.modelId = modelId;
            this.text = text;
            this.direction = direction;
        }

        public static ModeInfo fromBuf(FriendlyByteBuf buf) {
            return new ModeInfo(buf.readIdentifier(), buf.readUtf(), Direction.from2DDataValue(buf.readVarInt()));
        }

        public Identifier getModelId() {
            return modelId;
        }

        public void setModelId(Identifier modelId) {
            this.modelId = modelId;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public Direction getDirection() {
            return direction;
        }

        public void setDirection(Direction direction) {
            this.direction = direction;
        }

        public void toBuf(FriendlyByteBuf buf) {
            buf.writeIdentifier(this.modelId);
            buf.writeUtf(this.text);
            buf.writeVarInt(this.direction.get2DDataValue());
        }

        public CompoundTag serialize() {
            CompoundTag tag = new CompoundTag();
            tag.putString("model_id", this.modelId.toString());
            tag.putString("text", this.text);
            tag.putInt("direction", this.direction.get2DDataValue());
            return tag;
        }

        public void deserialize(CompoundTag nbt) {
            this.modelId = Identifier.parse(nbt.getStringOr("model_id", ""));
            this.text = nbt.getStringOr("text", "");
            this.direction = Direction.from2DDataValue(nbt.getIntOr("direction", 0));
        }
    }


    /**
     * 1.21.2+ 方块移除重设计：{@code Block.onRemove(state, Level, pos, newState, isMoving)} 已完全移除。
     * 掉落改由 {@code BlockEntity.preRemoveSideEffects} 负责（LevelChunk 在 BE 尚存活时调用；
     * {@code Block.affectNeighborsAfterRemoval} 只管邻居更新）。其默认实现仅对 {@code implements Container}
     * 的 BE 自动掉落——本类是 {@code extends BlockEntity} + 自研 handler，**不会**被自动处理，
     * 故在此显式恢复原本位于 Block.onRemove 的逻辑。
     */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (this.level == null || this.level.isClientSide()) {
            return;
        }
        Block.popResource(this.level, pos,
                ItemModelSwitcher.tileEntityToItemStack(this.level.registryAccess(), this));
    }

}
