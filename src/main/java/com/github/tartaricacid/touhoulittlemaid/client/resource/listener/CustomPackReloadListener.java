package com.github.tartaricacid.touhoulittlemaid.client.resource.listener;

import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.github.tartaricacid.touhoulittlemaid.client.animation.inner.InnerAnimation;
import com.github.tartaricacid.touhoulittlemaid.client.resource.bedrock.GeckoContainerBuilder;
import com.github.tartaricacid.touhoulittlemaid.client.resource.loader.CustomPackLoader;
import com.github.tartaricacid.touhoulittlemaid.client.resource.models.PlayerMaidModels;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.apache.commons.lang3.time.StopWatch;

import java.util.concurrent.TimeUnit;

/**
 * Reloads every client-side model source that depends on the active resource packs.
 * The order intentionally matches the 1.21.1 behavior baseline.
 */
public final class CustomPackReloadListener extends SimplePreparableReloadListener<Void> {
    public static void asyncReload() {
        Minecraft.getInstance().execute(CustomPackReloadListener::reloadCustomPacks);
    }

    private static void reloadCustomPacks() {
        StopWatch watch = StopWatch.createStarted();
        GeckoContainerBuilder.reload();
        InnerAnimation.init();
        CustomPackLoader.reloadPacks();
        PlayerMaidModels.reload();
        watch.stop();

        double time = watch.getTime(TimeUnit.MICROSECONDS) / 1000.0;
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.displayClientMessage(
                    Component.translatable("message.touhou_little_maid.reload.tip", time), false);
        }
        TouhouLittleMaid.LOGGER.info("Custom pack loading time: {} ms", time);
    }

    @Override
    protected Void prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        return null;
    }

    @Override
    protected void apply(Void unused, ResourceManager resourceManager, ProfilerFiller profiler) {
        reloadCustomPacks();
    }
}
