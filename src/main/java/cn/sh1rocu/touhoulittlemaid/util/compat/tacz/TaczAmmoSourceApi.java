package cn.sh1rocu.touhoulittlemaid.util.compat.tacz;

import net.fabricmc.loader.api.FabricLoader;

/**
 * 判定装着的这份 TaCZ 有没有官方的弹药来源 API，**两条实现路径共用这一个判据**。
 *
 * <p>TaCZ Refabricated 在 {@code 1.21.11_R2}（2026-08-16，上游 PR #48）加入
 * {@code AmmoSourceRegistry}，同时把我们四个 mixin 的注入锚点**全部拿掉**。
 * 双 jar javap 实证（R1 → R2）：{@code tacz$getItemHandler} 在
 * {@code AbstractGunItem} 2→0 · {@code LivingEntityShoot} 1→0 ·
 * {@code ModernKineticGunScriptAPI} 2→0 · {@code GunAnimationStateContext} 1→0；
 * 而 {@code GunAnimationStateContextMixin} 盯的 {@code lambda$hasAmmoToConsume$8}
 * 在 R2 里连方法本身都没了（提成了具名的 {@code hasAmmoToConsumeInEntity}）。
 * {@code touhou_little_maid_fabric.mixins.json} 是 {@code "required": true} 且
 * {@code injectors.defaultRequire = 1}，所以**在 R2 上留着 mixin 不是功能退化，是启动崩溃**。</p>
 *
 * <p>反过来，在 R1 上碰 {@code AmmoSource} 会 {@code NoClassDefFoundError}。
 * 两条路互斥，必须按运行期实际装的那份 jar 分流。</p>
 *
 * <h2>为什么按「类在不在 jar 里」判，而不是按版本号</h2>
 * 两版的 mod 版本是 {@code 1.1.8+fabric.1.21.11.R1} 与 {@code ...R2}，
 * **只差 {@code +} 之后的构建元数据**，而 semver 比较忽略那一段——
 * {@code fabric.mod.json} 的 {@code depends}/{@code breaks} 版本区间分不开这两版，
 * 字符串裁剪版本号又会被上游改个 tag 名就打翻。
 * 直接问「这份 jar 里有没有那个类」是特征检测，与版本号怎么写无关。
 *
 * <p><b>用 {@code findPath} 而不是 {@code Class.forName}</b>：本类会被
 * {@code MixinPlugin.shouldApplyMixin} 在 PREPARE 阶段调用，那时候去加载一个 TaCZ 的类
 * 会让它在自己的 mixin 生效之前就被类加载。{@code findPath} 只读 jar 内容，不触发类加载。</p>
 */
public final class TaczAmmoSourceApi {
    public static final String TACZ_ID = "tacz";

    /** {@code com.tacz.guns.api.item.ammo.AmmoSourceRegistry}，R2 起才有。 */
    private static final String AMMO_SOURCE_REGISTRY = "com/tacz/guns/api/item/ammo/AmmoSourceRegistry.class";

    /** 只探一次：mixin 阶段与 {@code TacCompat.init()} 都要问，且运行期内答案不会变。 */
    private static final boolean PRESENT = probe();

    private TaczAmmoSourceApi() {
    }

    /**
     * @return 装着的 TaCZ 提供官方 {@code AmmoSource} API（R2 及以后）为 true；
     * 未装 TaCZ、或装的是 R1 那种老版本为 false。
     */
    public static boolean isPresent() {
        return PRESENT;
    }

    private static boolean probe() {
        return FabricLoader.getInstance()
                .getModContainer(TACZ_ID)
                .map(container -> container.findPath(AMMO_SOURCE_REGISTRY).isPresent())
                .orElse(false);
    }
}
