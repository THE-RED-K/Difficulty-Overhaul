package dev.difficultyoverhaul.client;

import dev.difficultyoverhaul.DifficultyLevel;
import dev.difficultyoverhaul.config.DifficultyOverhaulConfig;
import dev.difficultyoverhaul.server.DifficultyOverhaulServer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

/**
 * 客户端侧逻辑：构建用于替换原版难度按钮的循环按钮。
 * <p>
 * 原版难度按钮只在单机（含局域网）显示，因此点击后可直接操作集成服务器；
 * 多人联机时玩家通过 /difficulty 命令（需要权限 2）切换模式。
 */
public final class DifficultyOverhaulClient {
    private DifficultyOverhaulClient() {
    }

    /**
     * 构建覆盖全部模式的循环按钮，悬停时显示原版风格的模式介绍。
     */
    public static CycleButton<DifficultyLevel> createDifficultyButton(int x, int y, Minecraft minecraft) {
        DifficultyLevel current = DifficultyOverhaulConfig.getMode();
        CycleButton<DifficultyLevel> button = CycleButton.builder(DifficultyLevel::getDisplayName)
                .withValues(DifficultyLevel.buttonOrder())
                .withInitialValue(current)
                .create(x, y, 150, 20, Component.translatable("options.difficulty"),
                        (clicked, mode) -> {
                            clicked.setTooltip(Tooltip.create(mode.getInfo()));
                            selectMode(minecraft, mode);
                        });
        button.setTooltip(Tooltip.create(current.getInfo()));
        return button;
    }

    private static void selectMode(Minecraft minecraft, DifficultyLevel mode) {
        // 同步客户端记忆（创建世界界面的初始值）
        DifficultyOverhaulConfig.setClientMode(mode);
        MinecraftServer server = minecraft.getSingleplayerServer();
        if (server != null) {
            // 模式写入配置与修改世界难度都必须在服务器线程执行
            server.execute(() -> DifficultyOverhaulServer.setMode(server, mode));
        }
    }
}
