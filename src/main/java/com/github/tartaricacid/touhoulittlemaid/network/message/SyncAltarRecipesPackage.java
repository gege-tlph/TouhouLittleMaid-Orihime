package com.github.tartaricacid.touhoulittlemaid.network.message;

import com.github.tartaricacid.touhoulittlemaid.crafting.AltarRecipe;
import com.github.tartaricacid.touhoulittlemaid.init.InitItems;
import com.github.tartaricacid.touhoulittlemaid.init.InitRecipes;
import com.github.tartaricacid.touhoulittlemaid.item.ItemEntityPlaceholder;
import com.github.tartaricacid.touhoulittlemaid.network.client.ClientAltarRecipeCache;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;

import java.util.ArrayList;
import java.util.List;

import static com.github.tartaricacid.touhoulittlemaid.util.IdentifierUtil.modLoc;

/**
 * Sends only the display data that the 1.21.1 client used from its RecipeManager.
 * Minecraft 1.21.11 no longer synchronizes custom recipe types to clients, so this
 * stays on Fabric's standard custom-payload channel instead of altering vanilla packets.
 */
public record SyncAltarRecipesPackage(List<AltarRecipeSummary> recipes) implements CustomPacketPayload {
    public static final Type<SyncAltarRecipesPackage> TYPE = new Type<>(modLoc("sync_altar_recipes"));

    private static final StreamCodec<RegistryFriendlyByteBuf, List<ItemStack>> ITEM_STACK_LIST_CODEC =
            ByteBufCodecs.collection(ArrayList::new, ItemStack.STREAM_CODEC, 4096);
    private static final StreamCodec<RegistryFriendlyByteBuf, List<List<ItemStack>>> INGREDIENT_LIST_CODEC =
            ByteBufCodecs.collection(ArrayList::new, ITEM_STACK_LIST_CODEC, 64);

    public static final StreamCodec<RegistryFriendlyByteBuf, AltarRecipeSummary> SUMMARY_STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, AltarRecipeSummary::recipeId,
                    ByteBufCodecs.STRING_UTF8, AltarRecipeSummary::recipeString,
                    INGREDIENT_LIST_CODEC, AltarRecipeSummary::inputs,
                    ItemStack.STREAM_CODEC, AltarRecipeSummary::output,
                    ByteBufCodecs.FLOAT, AltarRecipeSummary::powerCost,
                    ByteBufCodecs.STRING_UTF8, AltarRecipeSummary::langKey,
                    ByteBufCodecs.STRING_UTF8, AltarRecipeSummary::entityType,
                    AltarRecipeSummary::new);

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncAltarRecipesPackage> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.collection(ArrayList::new, SUMMARY_STREAM_CODEC, 1024),
                    SyncAltarRecipesPackage::recipes,
                    SyncAltarRecipesPackage::new);

    public static SyncAltarRecipesPackage from(RecipeManager recipeManager) {
        List<AltarRecipeSummary> recipes = recipeManager.getRecipes().stream()
                .filter(holder -> holder.value().getType() == InitRecipes.ALTAR_CRAFTING)
                .map(holder -> AltarRecipeSummary.from(holder.id().identifier(), (AltarRecipe) holder.value()))
                .toList();
        return new SyncAltarRecipesPackage(recipes);
    }

    public static void handle(SyncAltarRecipesPackage message, ClientPlayNetworking.Context context) {
        context.client().execute(() -> ClientAltarRecipeCache.replace(message.recipes()));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record AltarRecipeSummary(String recipeId, String recipeString, List<List<ItemStack>> inputs,
                                     ItemStack output, float powerCost, String langKey, String entityType) {
        private static AltarRecipeSummary from(Identifier recipeId, AltarRecipe recipe) {
            List<List<ItemStack>> inputs = recipe.getIngredients().stream()
                    .filter(ingredient -> !ingredient.isEmpty())
                    .map(ingredient -> ingredient.items().map(ItemStack::new).toList())
                    .toList();
            ItemStack output = recipe.getResult().copy();
            if (!recipe.isItemCraft()) {
                output = InitItems.ENTITY_PLACEHOLDER.getDefaultInstance();
                ItemEntityPlaceholder.setRecipeId(output, recipe.getRecipeString());
            }
            return new AltarRecipeSummary(recipeId.toString(), recipe.getRecipeString(), inputs, output,
                    recipe.getPower(), recipe.getLangKey(), recipe.getEntityType().toString());
        }

        public AltarRecipeSummary copy() {
            return new AltarRecipeSummary(recipeId, recipeString,
                    inputs.stream().map(items -> items.stream().map(ItemStack::copy).toList()).toList(),
                    output.copy(), powerCost, langKey, entityType);
        }
    }
}
