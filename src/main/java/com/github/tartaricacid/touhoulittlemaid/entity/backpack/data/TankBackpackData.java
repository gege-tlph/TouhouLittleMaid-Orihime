package com.github.tartaricacid.touhoulittlemaid.entity.backpack.data;

import com.github.tartaricacid.touhoulittlemaid.api.backpack.IBackpackData;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.network.message.SyncFluidAmountPackage;
import com.github.tartaricacid.touhoulittlemaid.util.MaidFluidUtil;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.base.SingleFluidStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import org.jetbrains.annotations.NotNull;

/**
 * 液体背包的数据与流体操作，自行为基准搬入。
 *
 * <p>26.1.2 侧的漂移：Fabric transfer-api 8.0.x 把 {@code SingleFluidStorage} 的
 * {@code writeData/readData} 实例方法换成了静态
 * {@code SingleVariantStorage.writeValue/readValue}（javap 实查；写的仍是
 * {@code variant}+{@code amount} 两键，与基准存档格式兼容）；
 * 女仆背包侧从 Forge 形态的 {@code CombinedInvWrapper} 换成宿主 transfer 的
 * {@code getAvailableInv}（{@code SlottedStorage<ItemVariant>}）。</p>
 */
public class TankBackpackData extends SimpleContainer implements IBackpackData {
    public static final long CAPACITY = 10 * FluidConstants.BUCKET;
    private static final int INPUT_INDEX = 0;
    private static final int OUTPUT_INDEX = 1;
    private final EntityMaid maid;
    private final SingleFluidStorage tank = SingleFluidStorage.withFixedCapacity(CAPACITY, () -> {
        // amount改变时发包同步客户端流体amount
        if (TankBackpackData.this.maid.getOwner() instanceof ServerPlayer serverPlayer) {
            ServerPlayNetworking.send(serverPlayer, new SyncFluidAmountPackage(this.getTank().amount));
        }
    });
    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            if (index == 0) {
                return (int) TankBackpackData.this.tankFluidCount;
            }
            return 0;
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) {
                TankBackpackData.this.tankFluidCount = value;
            }
        }

        @Override
        public int getCount() {
            return 1;
        }
    };
    private long tankFluidCount = 0;

    public TankBackpackData(EntityMaid maid) {
        super(2);
        this.maid = maid;
    }

    @Override
    public void setItem(int index, @NotNull ItemStack stack) {
        if (!this.maid.level.isClientSide()) {
            var availableInv = this.maid.getAvailableInv(false);
            if (index == INPUT_INDEX) {
                MaidFluidUtil.bucketToTank(stack, tank, availableInv);
            }
            if (index == OUTPUT_INDEX) {
                MaidFluidUtil.tankToBucket(stack, tank, availableInv);
            }
            this.tankFluidCount = tank.amount;
            Identifier key = BuiltInRegistries.FLUID.getKey(tank.getResource().getFluid());
            maid.setBackpackFluid(key.toString());
        }
        super.setItem(index, stack);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public ContainerData getDataAccess() {
        return dataAccess;
    }

    @Override
    public void load(CompoundTag tag, EntityMaid maid) {
        this.loadTank(tag.getCompoundOrEmpty("Tanks"), maid);
        this.fromItemList(TagValueInput.create(ProblemReporter.DISCARDING, this.maid.registryAccess(), tag).listOrEmpty("Items", ItemStack.CODEC));
    }

    @Override
    public void save(CompoundTag tag, EntityMaid maid) {
        // 8.0.x：writeData(ValueOutput) → 静态 SingleVariantStorage.writeValue（同写 variant+amount，格式兼容）
        TagValueOutput tankOut = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, maid.registryAccess());
        SingleVariantStorage.writeValue(tank, FluidVariant.CODEC, tankOut);
        tag.put("Tanks", tankOut.buildResult());
        TagValueOutput itemsOutput = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, maid.registryAccess());
        this.storeAsItemList(itemsOutput.list("Items", ItemStack.CODEC));
        tag.put("Items", itemsOutput.buildResult().getListOrEmpty("Items"));
    }

    @Override
    public void serverTick(EntityMaid maid) {
    }

    public SingleFluidStorage getTank() {
        return tank;
    }

    public void loadTank(CompoundTag nbt, EntityMaid maid) {
        // 8.0.x：readData(ValueInput) → 静态 SingleVariantStorage.readValue（读 variant+amount，格式兼容）
        SingleVariantStorage.readValue(tank, FluidVariant.CODEC, FluidVariant::blank,
                TagValueInput.create(ProblemReporter.DISCARDING, this.maid.registryAccess(), nbt));
        this.tankFluidCount = tank.getAmount();
        Identifier key = BuiltInRegistries.FLUID.getKey(tank.getResource().getFluid());
        maid.setBackpackFluid(key.toString());
    }
}
