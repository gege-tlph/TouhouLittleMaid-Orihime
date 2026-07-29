package cn.sh1rocu.touhoulittlemaid.mixin.client;

import cn.sh1rocu.touhoulittlemaid.api.event.AddPackFindersEvent;
import cn.sh1rocu.touhoulittlemaid.api.mixin.PackRepositoryExtension;
import com.llamalad7.mixinextras.sugar.Local;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.PackRepository;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Environment(EnvType.CLIENT)
@Mixin(CreateWorldScreen.class)
public class CreateWorldScreenMixin {
    // 1.21.11：建世界入口重构——openFresh(Minecraft,Screen) → openFresh(Minecraft,Runnable[,CreateWorldCallback])
    // 且实际建库逻辑下沉到 openCreateWorldScreen（直接 new PackRepository，不走 ServerPacksSource.createPackRepository，
    // 故注册的 ServerPacksSourceMixin 覆盖不到，此钩子仍必需）。
    // 原 getDataPackSelectionSettings 钩子已冗余：该方法现走 ServerPacksSource.createPackRepository，
    // ServerPacksSourceMixin 的 RETURN 注入已覆盖。
    @Inject(method = "openCreateWorldScreen", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/worldselection/CreateWorldScreen;createDefaultLoadConfig(Lnet/minecraft/server/packs/repository/PackRepository;Lnet/minecraft/world/level/WorldDataConfiguration;)Lnet/minecraft/server/WorldLoader$InitConfig;"))
    private static void tlm$addPacks(CallbackInfo ci, @Local PackRepository repository) {
        AddPackFindersEvent event = new AddPackFindersEvent(PackType.SERVER_DATA, ((PackRepositoryExtension) repository)::tlm$addPackFinder, false);
        AddPackFindersEvent.CALLBACK.invoker().onAddPackFinders(event);
    }
}