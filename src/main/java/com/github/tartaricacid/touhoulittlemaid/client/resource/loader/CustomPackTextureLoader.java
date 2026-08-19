package com.github.tartaricacid.touhoulittlemaid.client.resource.loader;

import com.github.tartaricacid.touhoulittlemaid.client.renderer.texture.CustomPackTexture;
import com.github.tartaricacid.touhoulittlemaid.client.resource.accessor.ResourceAccessor;
import com.google.common.collect.Sets;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

import java.util.Set;

final class CustomPackTextureLoader {
    /**
     * 用于标记已经注册过的材质，避免反复注册同一个材质
     */
    private static final Set<Identifier> TMP_REGISTER_TEXTURE = Sets.newHashSet();

    static void clear() {
        TMP_REGISTER_TEXTURE.clear();
    }

    static void register(ResourceAccessor accessor, Identifier texturePath) {
        if (!TMP_REGISTER_TEXTURE.contains(texturePath)) {
            CustomPackTexture texture = new CustomPackTexture(accessor, texturePath);
            if (texture.isExist()) {
                // registerAndLoad 会当场 load 并上传 GPU（26.1.2 字节码：apply → doLoad →
                // RenderSystem.getDevice()），而**游戏内下载走的是下载线程**：
                // ClientPackDownloadManager.downloadPack 的 CompletableFuture.thenRun 在完成线程上
                // 直接调 reloadPack → CustomPackLoader.readModelFromZipFile → ... → 本方法，
                // 不经那条已经分派过的 asyncReload。GL 上下文是线程绑定的，必须分派回渲染线程。
                // ⚠️ 26.1.2 与 1.21.11 的差别：本版上传路径里**没有** assertOnRenderThread
                // （javap 实查 GlDevice/GlTexture 均为 0 处），所以它不会像 1.21.11 那样当场断言，
                // 失败形态更安静——这使它更该被显式分派，而不是更不该。
                Minecraft minecraft = Minecraft.getInstance();
                minecraft.execute(() -> minecraft.getTextureManager().registerAndLoad(texturePath, texture));
                TMP_REGISTER_TEXTURE.add(texturePath);
            }
        }
    }
}
