package cn.sh1rocu.touhoulittlemaid.util.itemhandler;

import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ItemStackHandler implements IItemHandler, IItemHandlerModifiable, INBTSerializable<CompoundTag> {
    protected NonNullList<ItemStack> stacks;

    public ItemStackHandler() {
        this(1);
    }

    public ItemStackHandler(int size) {
        stacks = NonNullList.withSize(size, ItemStack.EMPTY);
    }

    public ItemStackHandler(NonNullList<ItemStack> stacks) {
        this.stacks = stacks;
    }

    public void setSize(int size) {
        stacks = NonNullList.withSize(size, ItemStack.EMPTY);
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        validateSlotIndex(slot);
        this.stacks.set(slot, stack);
        onContentsChanged(slot);
    }

    @Override
    public int getSlots() {
        return stacks.size();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        validateSlotIndex(slot);
        return this.stacks.get(slot);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (stack.isEmpty())
            return ItemStack.EMPTY;

        if (!isItemValid(slot, stack))
            return stack;

        validateSlotIndex(slot);

        ItemStack existing = this.stacks.get(slot);

        int limit = getStackLimit(slot, stack);

        if (!existing.isEmpty()) {
            if (!ItemStack.isSameItemSameComponents(stack, existing))
                return stack;

            limit -= existing.getCount();
        }

        if (limit <= 0)
            return stack;

        boolean reachedLimit = stack.getCount() > limit;

        if (!simulate) {
            if (existing.isEmpty()) {
                this.stacks.set(slot, reachedLimit ? stack.copyWithCount(limit) : stack);
            } else {
                existing.grow(reachedLimit ? limit : stack.getCount());
            }
            onContentsChanged(slot);
        }

        return reachedLimit ? stack.copyWithCount(stack.getCount() - limit) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (amount == 0)
            return ItemStack.EMPTY;

        validateSlotIndex(slot);

        ItemStack existing = this.stacks.get(slot);

        if (existing.isEmpty())
            return ItemStack.EMPTY;

        int toExtract = Math.min(amount, existing.getMaxStackSize());

        if (existing.getCount() <= toExtract) {
            if (!simulate) {
                this.stacks.set(slot, ItemStack.EMPTY);
                onContentsChanged(slot);
                return existing;
            } else {
                return existing.copy();
            }
        } else {
            if (!simulate) {
                this.stacks.set(slot, existing.copyWithCount(existing.getCount() - toExtract));
                onContentsChanged(slot);
            }

            return existing.copyWithCount(toExtract);
        }
    }

    @Override
    public int getSlotLimit(int slot) {
        return Item.ABSOLUTE_MAX_STACK_SIZE;
    }

    protected int getStackLimit(int slot, ItemStack stack) {
        return Math.min(getSlotLimit(slot), stack.getMaxStackSize());
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return true;
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        ListTag nbtTagList = new ListTag();
        for (int i = 0; i < stacks.size(); i++) {
            if (!stacks.get(i).isEmpty()) {
                CompoundTag itemTag = new CompoundTag();
                itemTag.putInt("Slot", i);
                ItemStack.CODEC.encodeStart(provider.createSerializationContext(NbtOps.INSTANCE), stacks.get(i))
                        .resultOrPartial(err -> TouhouLittleMaid.LOGGER.error("Failed to save item stack: {}", err))
                        .ifPresent(t -> {
                            if (t instanceof CompoundTag ct) {
                                nbtTagList.add(itemTag.merge(ct));
                            } else {
                                nbtTagList.add(itemTag);
                            }
                        });
            }
        }
        CompoundTag nbt = new CompoundTag();
        nbt.put("Items", nbtTagList);
        nbt.putInt("Size", stacks.size());
        return nbt;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        setSize(nbt.contains("Size") ? nbt.getInt("Size").orElse(stacks.size()) : stacks.size());
        nbt.getList("Items").ifPresent(tagList -> {
            for (int i = 0; i < tagList.size(); i++) {
                tagList.getCompound(i).ifPresent(itemTags -> {
                    int slot = itemTags.getInt("Slot").orElse(-1);

                    if (slot >= 0 && slot < stacks.size()) {
                        ItemStack.CODEC.parse(provider.createSerializationContext(NbtOps.INSTANCE), itemTags)
                                .resultOrPartial(err -> TouhouLittleMaid.LOGGER.error("Tried to load invalid item: {}", err))
                                .ifPresent(stack -> stacks.set(slot, stack));
                    }
                });
            }
        });
        onLoad();
    }

    protected void validateSlotIndex(int slot) {
        if (slot < 0 || slot >= stacks.size())
            throw new RuntimeException("Slot " + slot + " not in valid range - [0," + stacks.size() + ")");
    }

    protected void onLoad() {
    }

    protected void onContentsChanged(int slot) {
    }
}
