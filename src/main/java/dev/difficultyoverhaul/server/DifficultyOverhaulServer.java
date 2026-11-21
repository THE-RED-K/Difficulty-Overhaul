package dev.difficultyoverhaul.server;

import dev.difficultyoverhaul.DifficultyLevel;
import dev.difficultyoverhaul.config.DifficultyOverhaulConfig;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * 服务器侧逻辑：模式切换、难度强制、/difficulty 命令注册与模式随进度自动演进。
 */
public final class DifficultyOverhaulServer {
    private static final SimpleCommandExceptionType ERROR_INVALID_MODE =
            new SimpleCommandExceptionType(Component.translatable("difficulty_overhaul.commands.invalid_mode"));
    private static final SimpleCommandExceptionType ERROR_DIFFICULTY_LOCKED =
            new SimpleCommandExceptionType(Component.translatable("difficulty_overhaul.commands.difficulty_locked"));

    /**
     * 集成服务器新建世界时待应用的模式。
     * 由创建世界界面点击"创建"时记录（客户端线程），服务器线程启动时消费后清空。
     * 注意：不能依赖"模式配置是否已持久化"来判断新建世界——
     * Forge 的 COMMON 配置在 config/ 目录下为全局共享，一旦生成即存在，
     * 之后新建世界就会沿用已有模式、忽略创建界面选择的模式。
     */
    private static volatile DifficultyLevel pendingNewWorldMode;

    private DifficultyOverhaulServer() {
    }

    /** 记录即将创建的新世界要使用的模式（仅客户端创建世界界面调用）。 */
    public static void setPendingNewWorldMode(DifficultyLevel mode) {
        pendingNewWorldMode = mode;
    }

    /**
     * 服务器启动时确保世界难度与当前模式一致：
     * 非和平模式一律强制为困难，以兼容其他模组在困难模式下的特殊内容。
     * 新建世界（创建世界界面刚创建）时以创建界面选择的模式为初始模式；
     * 其余情况（加载已有世界/局域网/专用服务器）沿用服务器配置中的模式。
     * <p>
     * 两种分支都统一走 {@link #applyMode}，确保客户端记忆（CLIENT_MODE）同步，
     * 否则创建世界界面会显示与选项界面/当前生效模式不一致的旧难度。
     */
    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        MinecraftServer server = event.getServer();
        if (!server.isDedicatedServer() && pendingNewWorldMode != null) {
            // 单机/局域网且刚由创建世界界面创建：采用创建界面选择的模式并持久化
            DifficultyLevel mode = pendingNewWorldMode;
            pendingNewWorldMode = null;
            applyMode(server, mode);
            return;
        }
        // 加载已有世界/专用服务器：沿用服务器配置中的模式（顺带同步客户端记忆）
        applyMode(server, DifficultyOverhaulConfig.getMode());
    }

    /**
     * 模式随进度自动演进：监听生物死亡事件，击杀来源为玩家且被杀生物属于触发源时，
     * 将模式沿难度方向提升一档（Enum 顺序）。
     * 和平模式自动禁用演进（见 {@link DifficultyOverhaulConfig#autoProgressionActive}），
     * 最高档（神明）封顶不再提升。
     */
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!DifficultyOverhaulConfig.autoProgressionActive()) {
            return;
        }
        LivingEntity killed = event.getEntity();
        if (killed.level().isClientSide) {
            return;
        }
        // 仅统计"玩家击败"的击杀，避免环境伤害/指令误杀导致难度失控
        Entity killer = event.getSource().getEntity();
        if (!(killer instanceof Player)) {
            return;
        }
        if (!isProgressionTrigger(killed)) {
            return;
        }
        MinecraftServer server = killed.level().getServer();
        if (server == null) {
            return;
        }
        DifficultyLevel current = DifficultyOverhaulConfig.getMode();
        DifficultyLevel next = nextProgressionLevel(current);
        if (next == null) {
            // 已处于最高档，仅提醒
            server.getPlayerList().broadcastSystemMessage(
                    Component.translatable("difficulty_overhaul.progression.max"), false);
            return;
        }
        applyMode(server, next);
        server.getPlayerList().broadcastSystemMessage(
                Component.translatable("difficulty_overhaul.progression.level_up", next.getDisplayName()), false);
    }

    /**
     * 判定被杀生物是否属于自动演进的触发源：
     * forge:bosses 标签（受开关控制，含整合包追加的监守者/远古守卫者等）
     * 或配置列表中的注册 ID（不受标签开关影响，填注册 ID）。
     */
    private static boolean isProgressionTrigger(LivingEntity killed) {
        return (DifficultyOverhaulConfig.useForgeBossesTag()
                && killed.getType().is(Tags.EntityTypes.BOSSES))
                || autoProgressionBossListContains(killed.getType());
    }

    private static boolean autoProgressionBossListContains(EntityType<?> type) {
        ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(type);
        return key != null && DifficultyOverhaulConfig.autoProgressionBosses().contains(key.toString());
    }

    /**
     * 沿 Enum 顺序取下一档模式；已是最高档（神明）时返回 null。
     * <p>
     * 注意：演进沿 {@link DifficultyLevel#values()} 的声明顺序（和平最低、神明最高）。
     * 神明（GOD）作为最高档被显式封顶：继续击杀 Boss 不再提升模式，也不会产生越界。
     */
    private static DifficultyLevel nextProgressionLevel(DifficultyLevel current) {
        DifficultyLevel[] values = DifficultyLevel.values();
        // 防御：即使枚举扩展（在神明之后新增），取名 logical last 也视为最高档
        int lastIndex = values.length - 1;
        if (current.ordinal() >= lastIndex) {
            return null;
        }
        return values[current.ordinal() + 1];
    }

    /**
     * 切换模式（由按钮或命令调用，必须在服务器线程）。
     * 自动演进开启且锁定难度时直接忽略手动调整（命令与按钮均不生效）。
     * <p>
     * 实际写入由 {@link #applyMode} 完成。
     */
    public static void setMode(MinecraftServer server, DifficultyLevel mode) {
        if (DifficultyOverhaulConfig.autoProgressionLockDifficulty()) {
            return;
        }
        applyMode(server, mode);
    }

    /**
     * 应用模式的内部实现：写入配置、同步客户端记忆并调整世界难度。
     * 自动演进的步进与服务器启动沿用配置均从此经过，不受难度锁限制。
     */
    private static void applyMode(MinecraftServer server, DifficultyLevel mode) {
        DifficultyOverhaulConfig.setMode(mode);
        if (!server.isDedicatedServer()) {
            DifficultyOverhaulConfig.setClientMode(mode);
        }
        applyDifficulty(server, mode);
    }

    private static void applyDifficulty(MinecraftServer server, DifficultyLevel mode) {
        Difficulty target = mode == DifficultyLevel.PEACEFUL ? Difficulty.PEACEFUL : Difficulty.HARD;
        if (server.getWorldData().getDifficulty() != target) {
            // forced = true：绕过“锁定难度”，保证模组模式切换始终生效
            server.setDifficulty(target, true);
        }
    }

    /**
     * 重写原版 /difficulty 命令：支持全部模式名（含原版四个名称）。
     */
    public static void registerDifficultyCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("difficulty")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("difficulty", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            for (DifficultyLevel level : DifficultyLevel.values()) {
                                builder.suggest(level.getKey());
                            }
                            return builder.buildFuture();
                        })
                        .executes(DifficultyOverhaulServer::setModeCommand))
                .executes(DifficultyOverhaulServer::showDifficulty));
    }

    private static int showDifficulty(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        DifficultyLevel mode = DifficultyOverhaulConfig.getMode();
        source.sendSuccess(() -> Component.translatable("commands.difficulty.query", mode.getDisplayName()), false);
        return mode.ordinal();
    }

    private static int setModeCommand(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String name = StringArgumentType.getString(context, "difficulty");
        DifficultyLevel mode = DifficultyLevel.byKey(name);
        if (mode == null) {
            throw ERROR_INVALID_MODE.create();
        }
        if (DifficultyOverhaulConfig.autoProgressionLockDifficulty()) {
            throw ERROR_DIFFICULTY_LOCKED.create();
        }
        setMode(context.getSource().getServer(), mode);
        context.getSource().sendSuccess(
                () -> Component.translatable("commands.difficulty.success", mode.getDisplayName()), true);
        return mode.ordinal();
    }
}