package dev.difficultyoverhaul.mixin;

import dev.difficultyoverhaul.config.DifficultyOverhaulConfig;
import dev.difficultyoverhaul.server.DifficultyOverhaulServer;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 在"创建世界"按钮真正创建世界之前，记录创建界面当前选择的模式，
 * 供集成服务器启动时作为新世界的初始模式。
 * <p>
 * 背景：Forge 的 SERVER 类型配置是全局共享的（config/ 目录下），
 * 无法通过"配置文件是否已存在"判断世界是否新建，导致第二次及以后
 * 创建世界时创建界面选择的模式会被忽略、继续沿用旧模式。
 * <p>
 * 注意：1.20.1 中创建世界界面的创建入口是私有方法 onCreate()（点击
 * "创建"按钮时在主线程调用，之后才通过 WorldOpenFlows 异步真正建档），
 * 并没有 1.19 那种无参的 createNewWorld()；名为 createNewWorld 的方法
 * 是三个参数的私有重载，在异步线程中执行。因此这里必须注入 onCreate()V。
 */
@Mixin(CreateWorldScreen.class)
public abstract class CreateWorldScreenMixin {

    @Inject(method = "onCreate()V", at = @At("HEAD"))
    private void difficultyOverhaul$markPendingNewWorldMode(CallbackInfo ci) {
        // 此时难度按钮回调已把所选模式写入客户端配置，直接取其当前值。
        // 未手动选择时即为当前生效模式（setMode 会同步 CLIENT_MODE），两者一致。
        DifficultyOverhaulServer.setPendingNewWorldMode(DifficultyOverhaulConfig.getClientMode());
    }
}
