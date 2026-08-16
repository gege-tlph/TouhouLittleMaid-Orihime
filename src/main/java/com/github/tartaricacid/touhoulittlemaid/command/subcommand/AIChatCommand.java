package com.github.tartaricacid.touhoulittlemaid.command.subcommand;

import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.github.tartaricacid.touhoulittlemaid.ai.agent.context.ContextCategory;
import com.github.tartaricacid.touhoulittlemaid.ai.agent.context.GameContextRegister;
import com.github.tartaricacid.touhoulittlemaid.ai.agent.skill.SkillInstance;
import com.github.tartaricacid.touhoulittlemaid.ai.agent.skill.SkillLoader;
import com.github.tartaricacid.touhoulittlemaid.ai.agent.tool.ToolRegister;
import com.github.tartaricacid.touhoulittlemaid.ai.manager.site.AvailableSites;
import com.github.tartaricacid.touhoulittlemaid.ai.service.Site;
import com.github.tartaricacid.touhoulittlemaid.config.AiServerRuleConfig;
import com.github.tartaricacid.touhoulittlemaid.config.DefaultAiSnapshot;
import com.github.tartaricacid.touhoulittlemaid.config.ServerRuleConfig;
import com.github.tartaricacid.touhoulittlemaid.config.subconfig.AIConfig;
import com.github.tartaricacid.touhoulittlemaid.network.message.config.SyncServerRulesPacket;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import com.github.tartaricacid.touhoulittlemaid.network.message.ai.OpenAIConfigPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import com.github.tartaricacid.touhoulittlemaid.network.message.ai.SyncAISitesPacket;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static net.minecraft.ChatFormatting.*;

public class AIChatCommand {
    private static final String ROOT_NAME = "ai_chat";
    private static final String RELOAD_NAME = "reload";
    private static final String STATUS_NAME = "status";
    private static final String SKILL_NAME = "skill";
    private static final String TOOL_NAME = "tool";
    private static final String CONTEXT_NAME = "context";

    public static LiteralArgumentBuilder<CommandSourceStack> get() {
        LiteralArgumentBuilder<CommandSourceStack> root = LiteralArgumentBuilder.literal(ROOT_NAME);
        LiteralArgumentBuilder<CommandSourceStack> reload = LiteralArgumentBuilder.literal(RELOAD_NAME);
        LiteralArgumentBuilder<CommandSourceStack> status = LiteralArgumentBuilder.literal(STATUS_NAME);
        LiteralArgumentBuilder<CommandSourceStack> skill = LiteralArgumentBuilder.literal(SKILL_NAME);
        LiteralArgumentBuilder<CommandSourceStack> tool = LiteralArgumentBuilder.literal(TOOL_NAME);
        LiteralArgumentBuilder<CommandSourceStack> context = LiteralArgumentBuilder.literal(CONTEXT_NAME);
        LiteralArgumentBuilder<CommandSourceStack> tokens = ChatTokensCommand.get();

        // AI 侧的唯一重载入口，**不碰世界规则**（那是 `/tlm config reload` 的事）。
        // 不带参数 = all，保持老用法可用。
        // 粒度参数（all|site|skills）已砍（用户 2026-07-28）：站点在界面里即存即生效，reload 只服务
        // 手改文件的人；各部分逐文件成败互不连坐，全量重载毫秒级——粒度只是让管理员多做一个
        // 无意义的决定，还要多养几条接线（这里已经修过三个接线 bug）。
        reload.executes(AIChatCommand::reloadEverything);
        root.then(reload);
        root.then(status.executes(AIChatCommand::showStatus));
        // 站点是实例全局的、不属于任何一只女仆，管理员不该为了配一个 LLM 走到女仆跟前；
        // 而 Cloth 是可选依赖，没装它的服主没有别的入口。故给一条命令。
        root.then(LiteralArgumentBuilder.<CommandSourceStack>literal("sites")
                .executes(AIChatCommand::openSites));
        root.then(skill.executes(AIChatCommand::showSkills));
        root.then(tool.executes(AIChatCommand::showTools));
        root.then(context.executes(AIChatCommand::showContexts));
        root.then(tokens);

        // 开发环境专用的对话驱动器：自动化载体不能点击与按键，而「多轮后才出现」的缺陷
        // 需要精确控制轮数与语种。发布产物里不注册，见 AiChatDevCommand 的类注释。
        if (TouhouLittleMaid.DEBUG) {
            root.then(AiChatDevCommand.get());
        }

        return root;
    }

    /**
     * 汇报三条 AI 服务链路当前的真实可用状态。
     *
     * <p>这条命令是一次实机事故的直接补丁：一份写坏的 {@code stt.json} 让站点表整批放弃载入，
     * 玩家侧只看得到一句「服务端不提供该服务」，那句话把三轮排查全部引向了从未损坏的请求链路。
     * 管理员当时没有任何途径看出「服务开关是开着的，但一个可用站点都没有」——补的就是它。</p>
     */
    private static int showStatus(CommandContext<CommandSourceStack> context) {
        sendSuccess(context, "status.header");
        showService(context, "llm", ServerRuleConfig.get(AIConfig.LLM_ENABLED), AvailableSites.LLM_SITES);
        showService(context, "tts", ServerRuleConfig.get(AIConfig.TTS_ENABLED), AvailableSites.TTS_SITES);
        showSttService(context);
        context.getSource().sendSuccess(() -> component("status.assets",
                Component.literal(String.valueOf(SkillLoader.getAllSkills().size())).withStyle(YELLOW),
                Component.literal(String.valueOf(ToolRegister.getAllTools().size())).withStyle(YELLOW)), false);
        return Command.SINGLE_SUCCESS;
    }

    /**
     * 输出单条服务的状态行。
     *
     * <p>区分三种状态，因为它们的处理方式完全不同：开关本身关着（管理员的选择）、
     * 开关开着但没有任何启用的站点（服务实际不可用，玩家会收到「服务端不提供」）、
     * 以及正常。中间那种标红，它就是事故当天缺失的那个信号。</p>
     */
    /**
     * STT 的状态行不能与 LLM/TTS 同构，但理由已经变了。
     *
     * <p>「服务器提供 STT」整套功能于 2026-07-27 撤除，**服务端不再参与语音识别的任何一环**：
     * 站点、凭据、调用全在玩家客户端。所以这一行既不该报站点数（服务端没有 STT 站点），
     * 也不该报「未提供」（那个开关已经不存在了）——它只需说明这件事归谁管，
     * 免得管理员为了玩家的一句「语音输入用不了」在服务端配置里空找。</p>
     */
    private static void showSttService(CommandContext<CommandSourceStack> context) {
        MutableComponent label = component("status.service.stt").withStyle(AQUA);
        MutableComponent detail = component("status.stt.client_side").withStyle(GRAY);
        context.getSource().sendSuccess(() -> component("status.entry", label, detail), false);
    }

    private static void showService(CommandContext<CommandSourceStack> context, String service,
                                    boolean serviceEnabled, Map<String, ? extends Site> sites) {
        long enabled = sites.values().stream().filter(Site::enabled).count();
        MutableComponent label = component("status.service." + service).withStyle(AQUA);
        MutableComponent detail;
        if (!serviceEnabled) {
            detail = component("status.disabled").withStyle(GRAY);
        } else if (enabled == 0) {
            detail = component("status.no_usable_site", sites.size()).withStyle(RED);
        } else {
            detail = component("status.sites", enabled, sites.size()).withStyle(GREEN);
        }
        context.getSource().sendSuccess(() -> component("status.entry", label, detail), false);
    }

    /**
     * 一条命令重载实例级 AI 的全部：**规则 + 站点 + 技能**（§17 v2）。
     *
     * <p>规则重载走 {@code AiServerRuleConfig}（实例级 -ai-server.toml），成功后广播合流快照并做
     * 默认值变更告知——它与界面保存路径是同一对告知双路，契约测试钉着两处。
     * 规则失败**不放大**：站点与技能照常重载（与「坏一个站点文件不连坐技能」同一条纪律），
     * 只是回执降级为失败并单独指名规则那半。</p>
     */
    private static int reloadEverything(CommandContext<CommandSourceStack> context) {
        MinecraftServer server = context.getSource().getServer();
        DefaultAiSnapshot defaults = DefaultAiSnapshot.capture();
        boolean rulesOk = AiServerRuleConfig.reloadFromDisk();
        if (rulesOk) {
            SyncServerRulesPacket.syncToAll(server);
            defaults.diffAndNotify(server);
        } else {
            context.getSource().sendFailure(Component.translatable(
                    "commands.touhou_little_maid.ai_chat.reload_rules_failed"));
        }
        boolean sitesOk = reloadAll(server);
        return report(context, rulesOk && sitesOk);
    }

    private static int report(CommandContext<CommandSourceStack> context, boolean clean) {
        if (!clean) {
            context.getSource().sendFailure(Component.translatable(
                    "commands.touhou_little_maid.ai_chat.reload_failed"));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.translatable(
                "message.touhou_little_maid.ai_chat.reload_success"), true);
        return Command.SINGLE_SUCCESS;
    }

    /**
     * 重载 AI 站点并同步给编辑器；**站点保存路径用的就是这一条**。
     *
     * <p>返回值如实反映站点是否干净加载，管理员的失败回执不丢。</p>
     *
     * <p><b>顺带重载人设模板</b>：`AvailableSites.init()` 内部会调 `SettingReader.reloadSettings()`。
     * 那是既有耦合，本次拆分没有解开它——写在这里是为了让「reload site 到底重载了什么」有据可查，
     * 而不是留给下一个人去翻实现。</p>
     */
    public static boolean reloadSites(MinecraftServer server) {
        boolean sitesComplete = AvailableSites.init();
        SyncAISitesPacket.syncToSiteEditors(server);
        return sitesComplete;
    }

    /**
     * 重载 AI 侧的全部：站点 + 技能。**不碰世界规则。**
     *
     * <p><b>技能必须在站点之外独立重载。</b>站点读的是 {@code sites/*.json}，技能读的是
     * {@code skills/}，本就是两件互不相干的事。原实现在站点加载不完整时直接 return，
     * 于是一份写坏的 {@code stt.json} 会连带让技能不重载——正是「局部失败被放大成整体放弃」的又一例。</p>
     *
     * <p><b>这里原本还会调 `SyncServerRulesPacket.syncToAll`</b>——AI 重载去广播世界规则，方向是反的。
     * 那是 `/tlm config reload` 曾经走这条路留下的痕迹；命令拆分后规则同步已回到
     * {@code ConfigCommand}，本方法不再触碰世界规则的任何一环。</p>
     */
    public static boolean reloadAll(MinecraftServer server) {
        boolean sitesComplete = AvailableSites.init();
        SkillLoader.init();
        SyncAISitesPacket.syncToSiteEditors(server);
        return sitesComplete;
    }

    /**
     * 打开 AI 服务配置界面。走的是已有的 {@code OpenAIConfigPacket} 回路——
     * 服务端据 {@code canEditSite} 决定发什么，客户端据此决定侧栏有几栏，
     * 权限判定仍然只有服务端那一处，命令没有另开一条判定。
     */
    private static int openSites(CommandContext<CommandSourceStack> context) {
        ServerPlayer player;
        try {
            player = context.getSource().getPlayerOrException();
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException exception) {
            context.getSource().sendFailure(Component.translatable(
                    "commands.touhou_little_maid.ai_chat.sites_needs_player"));
            return 0;
        }
        OpenAIConfigPacket.sendSitesTo(player);
        return Command.SINGLE_SUCCESS;
    }

    private static int showSkills(CommandContext<CommandSourceStack> context) {
        Map<String, SkillInstance> skills = SkillLoader.getAllSkills();
        if (skills.isEmpty()) {
            sendSuccess(context, "skill.empty");
            return Command.SINGLE_SUCCESS;
        }

        sendSuccess(context, "skill.header");
        skills.forEach((name, skill) -> context.getSource().sendSuccess(() -> {
            MutableComponent nameComp = Component.literal(name).withStyle(YELLOW);
            MutableComponent descComp = Component.literal(skill.description()).withStyle(GRAY, ITALIC);
            if (skill.isKnowledgeType()) {
                return component("skill.entry.knowledge", nameComp, descComp);
            }
            return component("skill.entry", nameComp, descComp);
        }, false));

        return Command.SINGLE_SUCCESS;
    }

    private static int showTools(CommandContext<CommandSourceStack> context) {
        Map<String, ?> tools = ToolRegister.getAllTools();
        if (tools.isEmpty()) {
            sendSuccess(context, "tool.empty");
            return Command.SINGLE_SUCCESS;
        }

        sendSuccess(context, "tool.header");
        tools.keySet().forEach(tool -> context.getSource().sendSuccess(() -> {
            MutableComponent nameComp = Component.literal(tool).withStyle(YELLOW);
            return component("tool.entry", nameComp);
        }, false));
        return Command.SINGLE_SUCCESS;
    }

    private static int showContexts(CommandContext<CommandSourceStack> context) {
        List<ContextCategory> toolCategories = GameContextRegister.allToolCategories().stream()
                .sorted(Comparator.comparing(ContextCategory::id))
                .toList();
        List<ContextCategory> promptCategories = GameContextRegister.allPromptCategories().stream()
                .sorted(Comparator.comparing(ContextCategory::id))
                .toList();
        if (toolCategories.isEmpty() && promptCategories.isEmpty()) {
            sendSuccess(context, "context.empty");
            return Command.SINGLE_SUCCESS;
        }

        sendSuccess(context, "context.tool.header");
        showContextCategoryList(context, toolCategories);

        sendSuccess(context, "context.prompt.header");
        showContextCategoryList(context, promptCategories);
        return Command.SINGLE_SUCCESS;
    }

    private static void showContextCategoryList(CommandContext<CommandSourceStack> context, List<ContextCategory> categories) {
        if (categories.isEmpty()) {
            sendSuccess(context, "context.group.empty");
            return;
        }

        categories.forEach(category -> context.getSource().sendSuccess(() -> {
            String contextIds = GameContextRegister.getContextKeys(category.id()).stream()
                    .sorted()
                    .collect(Collectors.joining(", "));
            MutableComponent categoryComp = Component.literal(category.id()).withStyle(AQUA);
            MutableComponent idsComp = Component.literal(contextIds).withStyle(YELLOW);
            return component("context.entry", categoryComp, idsComp);
        }, false));
    }

    private static void sendSuccess(CommandContext<CommandSourceStack> context, String key) {
        context.getSource().sendSuccess(() -> component(key), false);
    }

    private static MutableComponent component(String key, Object... args) {
        return Component.translatable("commands.touhou_little_maid.ai_chat." + key, args);
    }
}
