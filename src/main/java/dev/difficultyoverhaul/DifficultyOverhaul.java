package dev.difficultyoverhaul;

import dev.difficultyoverhaul.config.DifficultyOverhaulConfig;
import dev.difficultyoverhaul.server.DifficultyOverhaulServer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;

// 此值须与 gradle.properties 中的 mod_id 及 templates/META-INF/neoforge.mods.toml 中的 modId 一致
@Mod(DifficultyOverhaul.MOD_ID)
public class DifficultyOverhaul {
    // 在公共位置定义 mod id 供全项目引用
    public static final String MOD_ID = "difficulty_overhaul";

    // 1.21.1 NeoForge 中 ModLoadingContext#registerConfig 已弃用，
    // 改由构造器注入的 ModContainer#registerConfig 完成；总线注册用 NeoForge.EVENT_BUS。
    public DifficultyOverhaul(IEventBus modEventBus, ModContainer modContainer) {
        // 通用（服务器权威）配置：所有倍率与当前模式均可配置。
        // 采用 COMMON 而非 SERVER：使其固定生成在 .minecraft/config（-common.toml），
        // 便于整合包作者直接编辑；代价是与联机客户端各自读本地文件、不再自动同步。
        modContainer.registerConfig(ModConfig.Type.COMMON, DifficultyOverhaulConfig.SPEC);
        // 客户端配置：记住创建世界界面/选项界面选择的模式
        modContainer.registerConfig(ModConfig.Type.CLIENT, DifficultyOverhaulConfig.CLIENT_SPEC);

        // 注册服务器生命周期事件（启动时强制将世界难度设为困难）
        NeoForge.EVENT_BUS.register(DifficultyOverhaulServer.class);
    }
}