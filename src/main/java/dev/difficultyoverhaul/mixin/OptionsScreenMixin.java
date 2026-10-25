package dev.difficultyoverhaul.mixin;

import dev.difficultyoverhaul.client.DifficultyOverhaulClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.OptionsScreen;
import net.minecraft.world.Difficulty;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 用覆盖全部模式的循环按钮替换原版难度按钮。
 * <p>
 * 原版 createDifficultyButton 由选项界面的 createOnlineButton（单机时）调用，
 * 替换后点击按钮即可在全部模式间循环切换。
 */
@Mixin(OptionsScreen.class)
public abstract class OptionsScreenMixin {

    @Inject(method = "createDifficultyButton", at = @At("HEAD"), cancellable = true)
    private static void difficultyOverhaul$createDifficultyButton(int pX, int pY, String pTranslationKey,
                                                                 Minecraft pMinecraft,
                                                                 CallbackInfoReturnable<CycleButton<Difficulty>> cir) {
        @SuppressWarnings("unchecked")
        CycleButton<Difficulty> button = (CycleButton<Difficulty>) (Object)
                DifficultyOverhaulClient.createDifficultyButton(pX, pY, pMinecraft);
        cir.setReturnValue(button);
    }
}
