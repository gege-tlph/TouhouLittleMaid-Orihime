package com.github.tartaricacid.touhoulittlemaid.item;

import java.util.Comparator;
import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.github.tartaricacid.touhoulittlemaid.crafting.AltarRecipe;
import com.github.tartaricacid.touhoulittlemaid.init.InitDataComponent;
import com.github.tartaricacid.touhoulittlemaid.init.InitItems;
import com.github.tartaricacid.touhoulittlemaid.init.InitRecipes;
import com.github.tartaricacid.touhoulittlemaid.network.client.ClientAltarRecipeCache;
import com.github.tartaricacid.touhoulittlemaid.network.message.SyncAltarRecipesPackage.AltarRecipeSummary;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Optional;

public class ItemEntityPlaceholder extends Item {
    public ItemEntityPlaceholder(Identifier id) {
        super(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id)).stacksTo(1));
    }

    public static ItemStack setRecipeId(ItemStack stack, String id) {
        stack.set(InitDataComponent.RECIPES_ID_TAG, id);
        return stack;
    }

    @SuppressWarnings("all")
    @Nullable
    public static Identifier getRecipeId(ItemStack stack) {
        if (stack.has(InitDataComponent.RECIPES_ID_TAG)) {
            return Identifier.fromNamespaceAndPath(TouhouLittleMaid.MOD_ID,
                    BuiltInRegistries.RECIPE_TYPE.getKey(InitRecipes.ALTAR_CRAFTING).getPath() + "/" +
                            stack.get(InitDataComponent.RECIPES_ID_TAG));
        }
        return null;
    }

    @SuppressWarnings("all")
    @Nullable
    public static Identifier getId(ItemStack stack) {
        if (stack.has(InitDataComponent.RECIPES_ID_TAG)) {
            return Identifier.fromNamespaceAndPath(TouhouLittleMaid.MOD_ID, stack.get(InitDataComponent.RECIPES_ID_TAG));
        }
        return null;
    }

    @Environment(EnvType.CLIENT)
    public static void fillItemCategory(CreativeModeTab.Output items) {
        ClientLevel world = Minecraft.getInstance().level;
        if (world == null) {
            return;
        }

        MinecraftServer server = Minecraft.getInstance().getSingleplayerServer();
        if (server == null) {
            ClientAltarRecipeCache.getRecipes().stream()
                    .filter(recipe -> recipe.output().is(InitItems.ENTITY_PLACEHOLDER))
                    .sorted(Comparator.comparing(AltarRecipeSummary::recipeString).reversed())
                    .forEach(recipe -> items.accept(setRecipeId(
                            new ItemStack(InitItems.ENTITY_PLACEHOLDER), recipe.recipeString())));
            return;
        }

        server.getRecipeManager().getRecipes().stream()
                .filter(holder -> holder.value().getType() == InitRecipes.ALTAR_CRAFTING)
                .map(holder -> holder.value() instanceof AltarRecipe altarRecipe ? altarRecipe : null)
                .filter(altarRecipe -> altarRecipe != null && !altarRecipe.isItemCraft())
                .sorted(Comparator.comparing(AltarRecipe::getRecipeString).reversed())
                .forEach(altarRecipe ->
                        items.accept(setRecipeId(new ItemStack(InitItems.ENTITY_PLACEHOLDER), altarRecipe.getRecipeString())));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getClickedFace() == Direction.UP) {
            Identifier id = getRecipeId(context.getItemInHand());
            Level world = context.getLevel();
            if (id != null && world instanceof ServerLevel serverLevel) {

                Optional<RecipeHolder<?>> recipe = serverLevel.recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, id));
                if (recipe.isPresent() && recipe.get().value() instanceof AltarRecipe altarRecipe) {
                    altarRecipe.spawnOutputEntity((ServerLevel) world, context.getClickedPos().above(), null);
                    context.getItemInHand().shrink(1);
                }
            }
        }
        return super.useOn(context);
    }

    @Override
    @Environment(EnvType.CLIENT)
    public Component getName(ItemStack stack) {
        Identifier recipeId = getId(stack);
        if (recipeId != null) {
            Path path = Paths.get(recipeId.getPath().toLowerCase(Locale.US));
            String langKey = String.format("jei.%s.altar_craft.%s.result", TouhouLittleMaid.MOD_ID, path.getFileName());
            return Component.translatable(langKey);
        }
        return Component.translatable("item.touhou_little_maid.entity_placeholder");
    }
}
