package dev.difficultyoverhaul;

import dev.difficultyoverhaul.config.DifficultyOverhaulConfig;
import dev.difficultyoverhaul.server.DifficultyOverhaulServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

// 此值须与 gradle.properties 中的 mod_id 及 META-INF/mods.toml 中的 modId 一致
@Mod(DifficultyOverhaul.MOD_ID)
public class DifficultyOverhaul {
    // 在公共位置定义 mod id 供全项目引用
    public static final String MOD_ID = "difficulty_overhaul";

    // ModLoadingContext.get() 是 1.20.1 Forge 的标准配置注册方式；
    // 该 API 自 1.21.1 起才弃用（届时改用 ModContainer#registerConfig），故 IDE 提示在此版本可安全忽略
    @SuppressWarnings({"deprecation", "removal"})
    public DifficultyOverhaul() {
        // 通用（服务器权威）配置：所有倍率与当前模式均可配置。
        // 采用 COMMON 而非 SERVER：使其固定生成在 .minecraft/config（-common.toml），
        // 便于整合包作者直接编辑；代价是与联机客户端各自读本地文件、不再自动同步。
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, DifficultyOverhaulConfig.SPEC);
        // 客户端配置：记住创建世界界面/选项界面选择的模式
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, DifficultyOverhaulConfig.CLIENT_SPEC);

        // 注册服务器生命周期事件（启动时强制将世界难度设为困难）
        MinecraftForge.EVENT_BUS.register(DifficultyOverhaulServer.class);
    }
}