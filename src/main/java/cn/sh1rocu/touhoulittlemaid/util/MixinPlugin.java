package cn.sh1rocu.touhoulittlemaid.util;

import cn.sh1rocu.touhoulittlemaid.util.compat.tacz.TaczAmmoSourceApi;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class MixinPlugin implements IMixinConfigPlugin {
    @Override
    public void onLoad(String s) {

    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    /** {@code cn.sh1rocu.touhoulittlemaid.mixin.compat.<modid>.XxxMixin}——第 6 段（下标 5）就是 modid。 */
    private static final String COMPAT_PREFIX = "cn.sh1rocu.touhoulittlemaid.mixin.compat.";

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (!mixinClassName.startsWith(COMPAT_PREFIX)) {
            return true;
        }
        String modId = mixinClassName.split("\\.")[5];
        if (!FabricLoader.getInstance().isModLoaded(modId)) {
            return false;
        }
        // 唯一一条例外：TaCZ 1.21.11_R2 起提供了官方 AmmoSource API，并把这四个 mixin 的注入锚点全部删掉。
        // 本配置是 required:true + injectors.defaultRequire=1，锚点没了不是「注入落空」而是启动崩溃，
        // 所以装了新版就必须让它们退场，改由 MaidAmmoSource 走官方 API。判据见 TaczAmmoSourceApi。
        if (TaczAmmoSourceApi.TACZ_ID.equals(modId)) {
            return !TaczAmmoSourceApi.isPresent();
        }
        return true;
    }

    @Override
    public void acceptTargets(Set<String> set, Set<String> set1) {

    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String s, ClassNode classNode, String s1, IMixinInfo iMixinInfo) {

    }

    @Override
    public void postApply(String s, ClassNode classNode, String s1, IMixinInfo iMixinInfo) {

    }
}
