package dev.difficultyoverhaul.mixin;

import dev.difficultyoverhaul.server.DifficultyOverhaulServer;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.commands.DifficultyCommand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 重写 /difficulty 命令的注册逻辑，使其支持全部模式名称。
 * <p>
 * 原版命令仅注册 peaceful/easy/normal/hard 四个子命令；这里整体替换为
 * 支持全部模式的实现（含原版四个名称，它们映射到对应的本模组模式）。
 */
@Mixin(DifficultyCommand.class)
public abstract class DifficultyCommandMixin {

    @Inject(method = "register", at = @At("HEAD"), cancellable = true)
    private static void difficultyOverhaul$register(CommandDispatcher<CommandSourceStack> dispatcher, CallbackInfo ci) {
        DifficultyOverhaulServer.registerDifficultyCommand(dispatcher);
        ci.cancel();
    }
}
