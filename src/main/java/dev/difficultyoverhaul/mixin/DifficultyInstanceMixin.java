package dev.difficultyoverhaul.mixin;

import dev.difficultyoverhaul.DifficultyLevel;
import dev.difficultyoverhaul.config.DifficultyOverhaulConfig;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * 将难度实例的基础难度强制为“困难”（和平模式除外）。
 * <p>
 * 这使游戏内所有按难度分支的逻辑（特殊技能、装备掉落、刷怪等，含其他模组的困难内容）
 * 一律按困难模式判定。有效难度、特殊倍率等字段均由基础难度派生，因此一处修改全局生效。
 */
@Mixin(DifficultyInstance.class)
public abstract class DifficultyInstanceMixin {

    @ModifyVariable(method = "<init>", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private static Difficulty difficultyOverhaul$forceHard(Difficulty base) {
        if (DifficultyOverhaulConfig.getMode() == DifficultyLevel.PEACEFUL) {
            return base;
        }
        return Difficulty.HARD;
    }
}
