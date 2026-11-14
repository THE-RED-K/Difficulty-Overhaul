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
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Difficulty;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 服务器侧逻辑：模式切换、难度强制与 /difficulty 命令注册。
 */
public final class DifficultyOverhaulServer {
    private static final SimpleCommandExceptionType ERROR_INVALID_MODE =
            new SimpleCommandExceptionType(Component.translatable("difficulty_overhaul.commands.invalid_mode"));

    /**
     * 集成服务器新建世界时待应用的模式。
     * 由创建世界界面点击"创建"时记录（客户端线程），服务器线程启动时消费后清空。
     * 注意：不能依赖"服务器配置是否已持久化"来判断新建世界——
     * Forge 的 SERVER 配置是全局共享的，配置文件一旦存在（任何旧世界创建过），
     * 之后新建世界就会沿用旧模式、忽略创建界面选择的模式。
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
     * 两种分支都统一走 {@link #setMode}，确保客户端记忆（CLIENT_MODE）同步，
     * 否则创建世界界面会显示与选项界面/当前生效模式不一致的旧难度。
     */
    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        MinecraftServer server = event.getServer();
        if (!server.isDedicatedServer() && pendingNewWorldMode != null) {
            // 单机/局域网且刚由创建世界界面创建：采用创建界面选择的模式并持久化
            DifficultyLevel mode = pendingNewWorldMode;
            pendingNewWorldMode = null;
            setMode(server, mode);
            return;
        }
        // 加载已有世界/专用服务器：沿用服务器配置中的模式（顺带同步客户端记忆）
        setMode(server, DifficultyOverhaulConfig.getMode());
    }

    /**
     * 切换模式（由按钮或命令调用，必须在服务器线程）。
     * 会写入配置持久化，并同步调整世界难度。
     * <p>
     * 集成服务器（单机/局域网）上还会同步客户端记忆 CLIENT_MODE，
     * 保证创建世界界面/选项界面显示的难度与当前生效模式始终一致
     * （修复 /difficulty 指令、加载已有世界后创建世界界面显示旧难度的问题）。
     */
    public static void setMode(MinecraftServer server, DifficultyLevel mode) {
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
        setMode(context.getSource().getServer(), mode);
        context.getSource().sendSuccess(
                () -> Component.translatable("commands.difficulty.success", mode.getDisplayName()), true);
        return mode.ordinal();
    }
}
