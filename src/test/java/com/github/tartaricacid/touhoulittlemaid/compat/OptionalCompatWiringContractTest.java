package com.github.tartaricacid.touhoulittlemaid.compat;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 可选第三方兼容的接线契约。
 *
 * <p><b>为什么不是行为判据</b>：这些兼容按定义只在装了对方模组时才跑，而对方是
 * {@code compileOnly}、不在 {@code runGametest} 的运行时里。行为基准那边有一条
 * {@code isModLoaded} 守着的 GameTest——模组不在时它整条不执行、照样绿，
 * 正是本仓库反复栽过的「零覆盖恒绿」形态，故本树不搬那条，改成始终有效的接线断言。
 * 真正「装上模组后表现对不对」只有实机能验。</p>
 */
class OptionalCompatWiringContractTest {
    /** 测试的 workingDir 是 build/test-working，回两级才是项目根。 */
    private static final Path PROJECT_ROOT = Path.of("..", "..");
    private static final Path MAIN_JAVA = PROJECT_ROOT.resolve("src/main/java");
    private static final Path TLM = MAIN_JAVA.resolve("com/github/tartaricacid/touhoulittlemaid");
    private static final Path FABRIC_MOD_JSON = PROJECT_ROOT.resolve("src/main/resources/fabric.mod.json");
    private static final Path TAVERN_COMPAT = TLM.resolve("compat/kaleidoscopetavern");
    private static final Path MENU_INTEGRATION = TLM.resolve("compat/cloth/MenuIntegration.java");
    private static final Path TAC_COMPAT = TLM.resolve("compat/gun/tacz/TacCompat.java");

    private static final Path REFURBISHED_COMPAT =
            TLM.resolve("compat/refurbishedfurniture/RefurbishedFurnitureCompat.java");
    private static final Path COMPAT_REGISTRY = TLM.resolve("init/registry/CompatRegistry.java");
    private static final Path TAG_BLOCK = TLM.resolve("datagen/tag/TagBlock.java");
    private static final Path GENERATED_TAGS =
            PROJECT_ROOT.resolve("src/main/generated/data/touhou_little_maid/tags/block");
    /** 家具重制的桌面标签。选它当判据是因为它是该模组<b>唯一</b>的方块标签，语义不会漂。 */
    private static final String REFURBISHED_TUCKABLE = "refurbished_furniture:tuckable";
    private static final Path MAID_AMMO_SOURCE = TLM.resolve("compat/gun/tacz/MaidAmmoSource.java");
    private static final Path MIXIN_ROOT = MAIN_JAVA.resolve("cn/sh1rocu/touhoulittlemaid/mixin");
    private static final Path FABRIC_MIXINS =
            PROJECT_ROOT.resolve("src/main/resources/touhou_little_maid_fabric.mixins.json");
    /** 饰品栏那一行的识别依据：选配置键而不是按钮文案——文案会改，键不会。 */
    private static final String CURIOS_MENU_ROW = "MaidConfig.ENABLE_MAID_CURIOS";

    /** 第三方模组的包前缀 → 只允许出现在哪个兼容包里。 */
    private static final List<String[]> FOREIGN_PACKAGE_CONTAINMENT = List.of(
            new String[]{"com.github.ysbbbbbb.kaleidoscopetavern", "compat/kaleidoscopetavern"},
            new String[]{"com.github.ysbbbbbb.kaleidoscopecookery", "compat/kaleidoscope"});

    /** {@link com.github.tartaricacid.touhoulittlemaid.entity.task.crop.SpecialCropManager} 的急切登记 API。 */
    private static final List<String> REGISTRATION_CALLS = List.of("addSeed(", "addCrop(", ".add(");

    /**
     * {@code little_maid_extension} 是**第十处静默注册面**：兼容类写好、编译通过、打包正常，
     * 不登记就永远不会被 {@code AnnotatedInstanceUtil.getModExtensions()} 发现，功能从不执行。
     */
    @Test
    void tavernCompatIsRegisteredAsALittleMaidExtension() throws IOException {
        String json = Files.readString(FABRIC_MOD_JSON, StandardCharsets.UTF_8);
        assertTrue(json.contains("\"little_maid_extension\""),
                "fabric.mod.json 缺少 little_maid_extension entrypoint——兼容类不会被发现");
        assertTrue(json.contains("compat.kaleidoscopetavern.TavernCompat"),
                "TavernCompat 没有登记进 little_maid_extension");
    }

    /**
     * 兼容的每个扩展点实现都必须被 {@code isModLoaded} 守着。
     *
     * <p>没有这道守卫，未装该模组的玩家一样会拿到那些行为对象，而它们的方法体里引用着
     * 不存在的类——触发时才 {@code NoClassDefFoundError}，且触发点在 AI tick 上。</p>
     */
    @Test
    void everyTavernExtensionPointIsGuardedByIsModLoaded() throws IOException {
        Path compatEntry = TAVERN_COMPAT.resolve("TavernCompat.java");
        String source = stripComments(Files.readString(compatEntry, StandardCharsets.UTF_8));
        int overrides = 0;
        List<String> unguarded = new ArrayList<>();
        for (String method : source.split("@Override")) {
            if (!method.contains("public void ")) {
                continue;
            }
            overrides++;
            if (!method.contains("isModLoaded(MOD_ID)")) {
                unguarded.add(method.strip().split("\\(")[0]);
            }
        }
        assertTrue(overrides >= 2,
                "只认出 " + overrides + " 个扩展点实现，识别依据可能已失效");
        assertEquals(List.of(), unguarded, "这些扩展点实现没有 isModLoaded 守卫");
    }

    /**
     * 两个为兼容而加的核心钩子必须真的**有人消费**——「加了扩展点」与「扩展点被接进主流程」
     * 是两件事，后者才是行为发生的地方（本仓库多次栽在「init 被调 ≠ init 有内容」上）。
     */
    @Test
    void compatHooksAreConsumedByCoreLogic() throws IOException {
        assertConsumed("ExtraMaidBrainManager.canClimbBlock(",
                TLM.resolve("entity/ai/navigation/MaidNodeEvaluator.java"),
                "攀爬排除钩子没有被寻路消费，附属再怎么否决都不会生效");
        assertConsumed("MaidMealManager.isWorkMealExcluded(",
                TLM.resolve("entity/task/meal/DefaultMaidWorkMeal.java"),
                "工作餐排除钩子没有被默认工作餐消费，登记的排除项永远不起作用");
    }

    /** 第三方类型只许出现在它自己的兼容包里——否则没装那个模组的玩家会在核心路径上崩。 */
    @Test
    void foreignTypesStayInsideTheirCompatPackage() throws IOException {
        List<String> leaks = new ArrayList<>();
        int inspected = 0;
        try (Stream<Path> files = Files.walk(MAIN_JAVA)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                inspected++;
                String source = stripComments(Files.readString(file, StandardCharsets.UTF_8));
                String normalized = file.toString().replace('\\', '/');
                for (String[] rule : FOREIGN_PACKAGE_CONTAINMENT) {
                    if (source.contains(rule[0]) && !normalized.contains(rule[1])) {
                        leaks.add(PROJECT_ROOT.relativize(file) + " 引用了 " + rule[0]);
                    }
                }
            }
        }
        assertTrue(inspected >= 400, "只走过 " + inspected + " 个源文件，扫描范围可能已失效");
        assertEquals(List.of(), leaks, "第三方类型泄漏到了兼容包之外");
    }

    /**
     * 可选兼容不得在 init 期急切求值第三方符号。
     *
     * <p><b>成因</b>：Fabric 对可选兼容没有初始化先后保证。这些登记方法跑在 TLM 的
     * mod initializer 里，读一个第三方常量就会连带触发它整个宿主类的静态初始化；
     * 若对方的效果注册尚未运行，它的食物组件会**永久**捕获空的效果 Holder，
     * 随后任何遍历该创造页的代码（原版创造背包、JEI、REI）都在 hashCode 上 NPE。</p>
     *
     * <p><b>判据按成因写，不按症状写</b>：不去断言「没有空 Holder」（那要装上对方模组才测得到），
     * 而是断言**登记通道**——凡形参是 {@code SpecialCropManager} 的方法，体内每一处登记
     * 都必须走延迟通道。这样调用点天然正确，不必逐个靠自觉。2026-08-19 实机崩溃即此因：
     * 多装一个模组改变了加载顺序，把原先侥幸正确的次序打翻了。</p>
     */
    @Test
    void cropRegistrationsFromCompatUseTheDeferredChannel() throws IOException {
        List<String> eager = new ArrayList<>();
        int hooksInspected = 0;
        int filesScanned = 0;
        try (Stream<Path> files = Files.walk(TLM.resolve("compat"))) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                filesScanned++;
                String source = stripComments(Files.readString(file, StandardCharsets.UTF_8));
                String body = methodBodyTaking(source, "SpecialCropManager");
                if (body == null) {
                    continue;
                }
                hooksInspected++;
                for (String call : REGISTRATION_CALLS) {
                    if (body.contains(call)) {
                        eager.add(file.getFileName() + " 用了急切登记 " + call);
                    }
                }
            }
        }
        assertTrue(filesScanned >= 6,
                "只走过 " + filesScanned + " 个兼容源文件，扫描范围可能已失效");
        assertTrue(hooksInspected >= 1,
                "一个吃 SpecialCropManager 的登记方法都没认出来，识别依据可能已失效");
        assertEquals(List.of(), eager,
                "可选兼容在 init 期急切求值了第三方符号——会提前触发对方静态初始化");
    }

    /**
     * 延迟登记必须真的被解析，否则等于把水稻兼容悄悄删掉。
     *
     * <p>「登记了」与「登记项被消费」是两件事——本仓库反复栽在「init 被调 ≠ init 有内容」上。
     * 解析点必须晚于全部 mod initializer、且早于任何女仆农作行为查表，故钉在服务器启动那一刻。</p>
     */
    @Test
    void deferredCropHandlersAreResolvedAtServerStarting() throws IOException {
        Path manager = TLM.resolve("entity/task/crop/SpecialCropManager.java");
        String source = stripComments(Files.readString(manager, StandardCharsets.UTF_8));
        assertTrue(source.contains("addLazySeed") && source.contains("addLazyCrop"),
                "SpecialCropManager 没有提供延迟登记通道，兼容侧无从延迟");
        String init = methodBodyOf(source, "public static void init()");
        assertTrue(init != null && init.contains("SERVER_STARTING"),
                "init() 没有在 SERVER_STARTING 上挂解析——延迟登记的项永远不会落表");
        assertTrue(init != null && init.contains("resolveDeferredHandlers()"),
                "SERVER_STARTING 上挂的不是解析动作，延迟项不会被消费");
    }

    /** 形参类型精确到那个管理器的登记方法体；找不到返回 null。 */
    private static String methodBodyTaking(String source, String parameterType) {
        int at = source.indexOf(parameterType + " ");
        while (at >= 0) {
            int paren = source.indexOf(')', at);
            int brace = source.indexOf('{', at);
            if (paren >= 0 && brace > paren) {
                return braceBlockAt(source, brace);
            }
            at = source.indexOf(parameterType + " ", at + 1);
        }
        return null;
    }

    /** 按签名取方法体——断言方法内部关系时必须缩到方法体，否则同名调用会让断言失去意义。 */
    private static String methodBodyOf(String source, String signature) {
        int at = source.indexOf(signature);
        if (at < 0) {
            return null;
        }
        int brace = source.indexOf('{', at);
        return brace < 0 ? null : braceBlockAt(source, brace);
    }

    private static String braceBlockAt(String source, int open) {
        int depth = 0;
        for (int i = open; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return source.substring(open, i + 1);
                }
            }
        }
        return null;
    }

    /**
     * 模组专属的菜单行，其**显示判据**必须与它的**生效判据**同源。
     *
     * <p>违反它的形态是「能设置却什么都改变不了的开关」：饰品栏那一行原先按
     * {@code isModLoaded(TRINKETS)} 显示，而它全部 13 个消费点读的是
     * {@code CuriosCompat.isLoadedOrEnable()}——那个闩只在 {@code CuriosCompat.init()} 里置真，
     * 而代码宿主把它的登记行注释掉了（四树对照过，不是我们的回归）。
     * 于是装了 Trinkets 的玩家能看见、能改、存得下，却什么都不会发生。</p>
     *
     * <p><b>判据钉的是「同源」这一维</b>，不是某个字面写法：门控表达式必须提到持有那个闩的类，
     * 且不得再拿模组 id 当判据。这样将来谁接上 {@code checkModLoad(TRINKETS, CuriosCompat::init)}，
     * 开关自己就回来了——不需要有人记得同时改菜单。</p>
     */
    @Test
    void modGatedMenuRowsGateOnTheSameLatchTheirConsumersRead() throws IOException {
        String menu = stripComments(Files.readString(MENU_INTEGRATION, StandardCharsets.UTF_8));

        int row = menu.indexOf(CURIOS_MENU_ROW);
        assertTrue(row > 0, "菜单里找不到饰品栏那一行（" + CURIOS_MENU_ROW + "），判据的识别依据可能已失效");
        // 缩到看守这一行的那个 if 条件本身——只在整份文件里找会被别处的同名门控蒙混过去
        int guardOpen = menu.lastIndexOf("if (", row);
        assertTrue(guardOpen > 0, "饰品栏那一行没有任何 if 门控");
        String guard = menu.substring(guardOpen, menu.indexOf(')', guardOpen) + 1);

        assertTrue(guard.contains("CuriosCompat.isLoaded()"),
                "饰品栏菜单行的门控必须是消费点读的那个闩 CuriosCompat.isLoaded()，实际是：" + guard);
        assertTrue(!guard.contains("TRINKETS"),
                "不得用 isModLoaded(TRINKETS) 门控：模组在场不等于兼容在场，"
                        + "登记行注释掉时会露出一个改不动任何东西的开关。实际是：" + guard);

        // 活性：被镜像的那个闩必须真的有一批消费点，否则这条断言是在守一个没人用的写法
        int consumers = 0;
        try (Stream<Path> walk = Files.walk(MAIN_JAVA)) {
            for (Path java : walk.filter(p -> p.toString().endsWith(".java")).toList()) {
                if (stripComments(Files.readString(java, StandardCharsets.UTF_8))
                        .contains("CuriosCompat.isLoadedOrEnable()")) {
                    consumers++;
                }
            }
        }
        assertTrue(consumers >= 10,
                "只找到 " + consumers + " 个 isLoadedOrEnable 消费点，识别依据可能已失效");
    }

    /**
     * TaCZ 弹药来源走官方 API，且**不得再留任何 tacz mixin**。
     *
     * <p>{@code 26.1.2_R2} 起上游提供了 {@code AmmoSource} / {@code AmmoSourceProvider} /
     * {@code AmmoSourceRegistry}，同时把我们四个 mixin 的注入锚点**全部移除**
     * （R1/R2 双 jar javap 实证：{@code tacz$getItemHandler} 由 5 处变 0，
     * {@code lambda$hasAmmoToConsume$0} 由 1 处变 0）。而
     * {@code touhou_little_maid_fabric.mixins.json} 是 {@code "required": true}，
     * 所以留着旧 mixin 不是「兼容退化」而是**启动崩溃**——两者互斥，没有并存写法。</p>
     *
     * <p>这道闸同时看守两件事：provider 登记还在（没登记＝女仆背包里的弹药 TaCZ 看不见，
     * 会静默回落到 {@code ENTITY_INVENTORY}，表现为「有弹药却换不了弹」而不报任何错），
     * 与 mixin 没有复活。</p>
     */
    @Test
    void taczAmmoGoesThroughTheOfficialApiAndNoTaczMixinSurvives() throws IOException {
        String compat = stripComments(Files.readString(TAC_COMPAT, StandardCharsets.UTF_8));
        String init = methodBodyOf(compat, "public static boolean init()");
        assertTrue(init != null, "TacCompat.init() 找不到，判据的识别依据已失效");
        assertTrue(init.contains("AmmoSourceRegistry.EVENT.register"),
                "TacCompat.init() 没有登记 AmmoSourceProvider：女仆背包的弹药 TaCZ 将完全看不见，"
                        + "且会静默回落到实体自身物品栏，不报任何错");

        String mixins = Files.readString(FABRIC_MIXINS, StandardCharsets.UTF_8);
        assertTrue(!mixins.contains("compat.tacz."),
                "mixins.json 仍登记着 tacz mixin：R2 已移除全部注入锚点，required:true 下会启动崩溃");
        try (Stream<Path> walk = Files.walk(MIXIN_ROOT)) {
            List<Path> left = walk.filter(p -> p.toString().replace('\\', '/').contains("/mixin/compat/tacz/"))
                    .filter(p -> p.toString().endsWith(".java")).toList();
            assertTrue(left.isEmpty(), "还留着 tacz mixin 源文件：" + left);
        }

        // provider 的两侧判据必须一致：hasAmmo 说有、consumeAmmo 抠不到，会让换弹动画播了却不上弹
        String source = stripComments(Files.readString(MAID_AMMO_SOURCE, StandardCharsets.UTF_8));
        String has = methodBodyOf(source, "public boolean hasAmmo(");
        assertTrue(has != null, "MaidAmmoSource.hasAmmo 找不到");
        assertTrue(has.contains("IAmmo") && has.contains("IAmmoBox"),
                "hasAmmo 的判据必须与抠除侧一样覆盖散装弹药与弹药盒两种，实际：" + has);
        assertTrue(!has.contains("extractItem") && !has.contains("setStackInSlot"),
                "hasAmmo 必须只读——上游明写它要与 consumeAmmo 一致且不得有副作用");
    }

    /**
     * 家具重制的注册表同步必须真的被调用，且被 {@code isModLoaded} 守着。
     *
     * <p>写好一个 {@code init()} 却没人调，是本仓库反复栽的「纸面接口」形态：编译、打包、
     * 启动一路正常，功能从不执行。这里钉的是<b>那条调用存在且经过模组守卫</b>。</p>
     *
     * <p>守卫不可省：{@code RegistryAttribute.SYNCED} 一旦加上就<b>对所有玩家生效</b>——
     * 服务端持有客户端没有的配方序列化器时会显式踢人。没装家具重制的人不该承担这个代价。</p>
     */
    @Test
    void refurbishedRegistrySyncIsWiredBehindTheModGate() throws IOException {
        String registry = stripComments(Files.readString(COMPAT_REGISTRY, StandardCharsets.UTF_8));

        assertTrue(registry.contains("RefurbishedFurnitureCompat::init"),
                "CompatRegistry 没有调用 RefurbishedFurnitureCompat::init——注册表同步永远不会发生");

        String enqueue = methodBodyOf(registry, "public static void onEnqueue()");
        assertNotNull(enqueue, "CompatRegistry.onEnqueue() 不见了，兼容接线的落点没了");
        assertTrue(enqueue.contains("checkModLoad(REFURBISHED_FURNITURE, RefurbishedFurnitureCompat::init)"),
                "家具重制的 init 必须经 checkModLoad 守卫；无条件调用会让没装该模组的玩家也承担"
                        + "注册表同步的断线代价");
    }

    /**
     * 同步的目标必须正是<b>配方序列化器</b>那张表。
     *
     * <p>缺陷类是「一张会上网的表没有被同步」：26.1.2 字节码实证 {@code Recipe.STREAM_CODEC}
     * 走 {@code ByteBufCodecs.registry(Registries.RECIPE_SERIALIZER)} 再 dispatch，
     * 即序列化器按<b>数字 id</b> 上线。换成别的注册表就修不到这个缺陷，而代码依旧「看起来对」。</p>
     */
    @Test
    void refurbishedSyncsExactlyTheRecipeSerializerRegistry() throws IOException {
        String source = stripComments(Files.readString(REFURBISHED_COMPAT, StandardCharsets.UTF_8));
        String init = methodBodyOf(source, "public static void init()");
        assertNotNull(init, "RefurbishedFurnitureCompat.init() 不见了");

        assertTrue(init.contains("Registries.RECIPE_SERIALIZER"),
                "同步的必须是 RECIPE_SERIALIZER 那张表——换成别的表就修不到这个缺陷");
        assertTrue(init.contains("RegistryAttribute.SYNCED"),
                "必须加的属性是 SYNCED；OPTIONAL 只覆盖「整张表缺席」，不覆盖「表里缺条目」");
    }

    /**
     * 家具的桌面 / 坐具必须真的进了避让与禁跳两张标签，而且是 optional 条目。
     *
     * <p>判据同时看<b>源码</b>与<b>datagen 产物</b>：只看源码会漏掉「改了 TagBlock 但忘了重跑
     * datagen」，而 json 才是真正装进 jar 的东西。</p>
     *
     * <p>{@code required: false} 那一维不能省——写成必需项时，没装该模组的世界会因为标签
     * 指向不存在的方块而报错。</p>
     */
    @Test
    void refurbishedFurnitureLandsInTheGeneratedTagsAsOptionalEntries() throws IOException {
        String tagBlock = stripComments(Files.readString(TAG_BLOCK, StandardCharsets.UTF_8));
        assertTrue(tagBlock.contains("addRefurbishedFurniture(MAID_AVOID_BLOCK)"),
                "TagBlock 没把家具重制接进 MAID_AVOID_BLOCK");
        assertTrue(tagBlock.contains("addRefurbishedFurniture(MAID_JUMP_FORBIDDEN_BLOCK)"),
                "TagBlock 没把家具重制接进 MAID_JUMP_FORBIDDEN_BLOCK");

        int checked = 0;
        for (String tag : new String[]{"maid_avoid_block", "maid_jump_forbidden_block",
                "maid_snack_stand_block"}) {
            String json = Files.readString(GENERATED_TAGS.resolve(tag + ".json"), StandardCharsets.UTF_8);
            assertTrue(json.contains(REFURBISHED_TUCKABLE),
                    "datagen 产物 " + tag + ".json 里没有 " + REFURBISHED_TUCKABLE
                            + "——改了 TagBlock 但没重跑 runDatagen？");
            assertFalse(json.contains("\"required\": true"),
                    tag + ".json 出现了必需条目：未装该模组的世界会因标签指向不存在的方块而报错");
            checked++;
        }
        // 活性断言：与上面的结论正交。没有它，「产物目录改名了、一个文件都没读到」
        // 与「读到了且都合格」在输出上完全一样。
        assertEquals(3, checked, "应当检查 3 张标签产物");
    }
    private static void assertConsumed(String call, Path consumer, String message) throws IOException {
        String source = stripComments(Files.readString(consumer, StandardCharsets.UTF_8));
        assertTrue(source.contains(call), message + "（期望在 " + consumer.getFileName() + " 里找到 " + call + "）");
    }

    /** 去掉块注释与行注释——否则只在 javadoc 里提到的接线会被判为「存在」。 */
    private static String stripComments(String source) {
        return source.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("(?m)//.*$", "");
    }
}
