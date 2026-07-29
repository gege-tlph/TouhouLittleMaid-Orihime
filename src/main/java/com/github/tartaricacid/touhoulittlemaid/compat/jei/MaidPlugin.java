package com.github.tartaricacid.touhoulittlemaid.compat.jei;

import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.github.tartaricacid.touhoulittlemaid.client.gui.entity.maid.AbstractMaidContainerGui;
import com.github.tartaricacid.touhoulittlemaid.client.gui.entity.maid.backpack.CraftingTableBackpackContainerScreen;
import com.github.tartaricacid.touhoulittlemaid.client.gui.entity.maid.backpack.FurnaceBackpackContainerScreen;
import com.github.tartaricacid.touhoulittlemaid.compat.jei.altar.AltarRecipeCategory;
import com.github.tartaricacid.touhoulittlemaid.compat.jei.altar.AltarRecipeMaker;
import com.github.tartaricacid.touhoulittlemaid.init.InitDataComponent;
import com.github.tartaricacid.touhoulittlemaid.init.InitItems;
import com.github.tartaricacid.touhoulittlemaid.inventory.container.backpack.CraftingTableBackpackContainer;
import com.github.tartaricacid.touhoulittlemaid.inventory.container.backpack.FurnaceBackpackContainer;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.registration.*;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;

@JeiPlugin
public class MaidPlugin implements IModPlugin {
    private static final Identifier UID = Identifier.fromNamespaceAndPath(TouhouLittleMaid.MOD_ID, "jei");
    private static IJeiRuntime runtime;
    private static boolean altarRecipesRegistered;

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new AltarRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        List<com.github.tartaricacid.touhoulittlemaid.compat.jei.altar.AltarRecipeWrapper> altarRecipes =
                AltarRecipeMaker.getInstance().getAltarRecipes();
        registration.addRecipes(AltarRecipeCategory.ALTAR, altarRecipes);
        altarRecipesRegistered = !altarRecipes.isEmpty();
        TouhouLittleMaid.LOGGER.info("Registered {} altar recipes with JEI", altarRecipes.size());
        registration.addIngredientInfo(InitItems.GARAGE_KIT.getDefaultInstance(), VanillaTypes.ITEM_STACK, Component.translatable("jei.touhou_little_maid.garage_kit.info"));
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
        addSyncedAltarRecipesIfNeeded();
    }

    @Override
    public void onRuntimeUnavailable() {
        runtime = null;
        altarRecipesRegistered = false;
    }

    public static void onSyncedAltarRecipesAvailable() {
        addSyncedAltarRecipesIfNeeded();
    }

    private static void addSyncedAltarRecipesIfNeeded() {
        if (runtime == null || altarRecipesRegistered) {
            return;
        }
        List<com.github.tartaricacid.touhoulittlemaid.compat.jei.altar.AltarRecipeWrapper> altarRecipes =
                AltarRecipeMaker.getInstance().getAltarRecipes();
        if (!altarRecipes.isEmpty()) {
            runtime.getRecipeManager().addRecipes(AltarRecipeCategory.ALTAR, altarRecipes);
            altarRecipesRegistered = true;
            TouhouLittleMaid.LOGGER.info("Added {} synchronized altar recipes to the active JEI runtime", altarRecipes.size());
        }
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(InitItems.HAKUREI_GOHEI.getDefaultInstance(), AltarRecipeCategory.ALTAR);
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        // JEI 27：改用官方组件区分通道（旧 interpreter 注册在 27 上不生效，门实例日志实证「2 duplicate items」）。
        // 坐垫/手办为超出 origin 的显示层补注册：无它们 JEI 列表把全部变体折叠为 1（用户以 JEI 为主要浏览入口）。
        registration.registerFromDataComponentTypes(InitItems.ENTITY_PLACEHOLDER, InitDataComponent.RECIPES_ID_TAG);
        registration.registerFromDataComponentTypes(InitItems.CHAIR, InitDataComponent.MODEL_ID_TAG);
        registration.registerFromDataComponentTypes(InitItems.GARAGE_KIT, InitDataComponent.MAID_INFO);
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        registration.addRecipeTransferHandler(CraftingTableBackpackContainer.class, CraftingTableBackpackContainer.TYPE, RecipeTypes.CRAFTING, 62, 9, 0, 61);
        registration.addRecipeTransferHandler(FurnaceBackpackContainer.class, FurnaceBackpackContainer.TYPE, RecipeTypes.SMELTING, 61, 1, 0, 61);
        registration.addRecipeTransferHandler(FurnaceBackpackContainer.class, FurnaceBackpackContainer.TYPE, RecipeTypes.SMELTING_FUEL, 62, 1, 0, 61);
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        // 过期 TODO 已清：背包容器屏 B2-2 已恢复编译
        registration.addRecipeClickArea(CraftingTableBackpackContainerScreen.class, 213, 121, 13, 12, RecipeTypes.CRAFTING);
        registration.addRecipeClickArea(FurnaceBackpackContainerScreen.class, 183, 118, 28, 24, RecipeTypes.SMELTING, RecipeTypes.SMELTING_FUEL);
        registerTaskListArea(registration);
    }

    private void registerTaskListArea(IGuiHandlerRegistration registration) {
        registration.addGenericGuiContainerHandler(AbstractMaidContainerGui.class, new IGuiContainerHandler<AbstractMaidContainerGui<?>>() {
            @Override
            public List<Rect2i> getGuiExtraAreas(AbstractMaidContainerGui<?> containerScreen) {
                return containerScreen.getExclusionArea();
            }
        });
    }

    @Override
    public Identifier getPluginUid() {
        return UID;
    }
}
