package com.github.tartaricacid.touhoulittlemaid.client.init;

import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.github.tartaricacid.touhoulittlemaid.util.IdentifierUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;

/**
 * 把随模组内置的「TLM Legacy Pack」注册进资源包列表 —— 可选、默认关，开了就换回旧版模型与贴图。
 *
 * <p><b>为什么这里与行为基准写法完全不同</b>：基准是自己造了一条 Forge 风格的事件总线
 * （{@code AddPackFindersEvent} + {@code PackRepositoryExtension} + 三个 mixin），
 * 由 {@code LegacyPackRepositorySource} 手工 new 一个 {@code Pack} 塞进 {@code PackRepository}。
 * 那整条链在本树的宿主上已经不存在，而 26.1.2 的 Fabric 有原生落点，
 * 于是按「写法对新基、行为对基准」重做成一次注册调用。</p>
 *
 * <p><b>包体位置由 API 决定，不是随便放的</b>：{@code ResourceLoader.registerBuiltinPack} 的方法体是
 * {@code "resourcepacks/" + id.getPath()}（javap -c 实证），故包必须落在
 * {@code src/main/resources/resourcepacks/legacy_pack/}。改 id 就必须同步改目录名，
 * 两者对不上时**不会报错，只是包从列表里消失**，故有契约测试盯着。</p>
 *
 * <p><b>两个 lang 键因此复活</b>：宿主删掉整包时把
 * {@code pack.touhou_little_maid.legacy_resources_pack.title/desc} 两个键留在了 lang 文件里（14 种语言）。
 * 标题走这里的 {@code Component.translatable}，描述走包内 {@code pack.mcmeta} 的
 * {@code description} 文本组件——两个键各自有了真实消费者。</p>
 */
@Environment(EnvType.CLIENT)
public final class InitLegacyResourcePack {
    /** 与 {@code src/main/resources/resourcepacks/legacy_pack/} 这个目录名严格对应。 */
    public static final String PACK_DIR_NAME = "legacy_pack";
    public static final String TITLE_KEY = "pack.touhou_little_maid.legacy_resources_pack.title";

    private InitLegacyResourcePack() {
    }

    public static void register() {
        FabricLoader.getInstance().getModContainer(TouhouLittleMaid.MOD_ID).ifPresent(container ->
                ResourceLoader.registerBuiltinPack(
                        IdentifierUtil.modLoc(PACK_DIR_NAME),
                        container,
                        Component.translatable(TITLE_KEY),
                        // 与基准的 PackSelectionConfig(required=false, TOP, fixed=false) 对应：
                        // 玩家可自由开关，默认关
                        PackActivationType.NORMAL));
    }
}
