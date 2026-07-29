package com.github.tartaricacid.touhoulittlemaid.client.renderer.item;

import net.fabricmc.fabric.api.client.model.loading.v1.wrapper.WrapperBakedItemModel;
import net.fabricmc.fabric.api.client.model.loading.v1.wrapper.WrapperUnbakedItemModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

/** Selects a replacement item model at render time so config toggles remain live. */
public final class ReplaceableBakedModel extends WrapperBakedItemModel {
    private final ItemModel replacement;
    private final Supplier<Boolean> replace;

    private ReplaceableBakedModel(ItemModel original, ItemModel replacement, Supplier<Boolean> replace) {
        super(original);
        this.replacement = replacement;
        this.replace = replace;
    }

    @Override
    public void update(ItemStackRenderState state, ItemStack stack, ItemModelResolver resolver,
                       ItemDisplayContext displayContext, @Nullable ClientLevel level,
                       @Nullable ItemOwner owner, int seed) {
        (replace.get() ? replacement : wrapped).update(state, stack, resolver, displayContext, level, owner, seed);
    }

    public static final class Unbaked extends WrapperUnbakedItemModel {
        private final ItemModel.Unbaked replacement;
        private final Supplier<Boolean> replace;

        public Unbaked(ItemModel.Unbaked original, ItemModel.Unbaked replacement, Supplier<Boolean> replace) {
            super(original);
            this.replacement = replacement;
            this.replace = replace;
        }

        @Override
        public void resolveDependencies(ResolvableModel.Resolver resolver) {
            super.resolveDependencies(resolver);
            replacement.resolveDependencies(resolver);
        }

        @Override
        public ItemModel bake(ItemModel.BakingContext context) {
            return new ReplaceableBakedModel(wrapped.bake(context), replacement.bake(context), replace);
        }
    }
}
