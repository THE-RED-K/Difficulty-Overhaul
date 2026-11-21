package dev.difficultyoverhaul.mixin;

import dev.difficultyoverhaul.DifficultyLevel;
import dev.difficultyoverhaul.api.MixinPriorities;
import dev.difficultyoverhaul.config.DifficultyOverhaulConfig;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 将难度实例的基础难度强制为“困难”（和平模式除外），并统一保证
 * 特殊事件倍率与依赖有效难度的概率项不低于当前模式的下限。
 * <p>
 * 这使游戏内所有按难度分支的逻辑（特殊技能、装备掉落、刷怪等，含其他模组的困难内容）
 * 一律按困难模式判定。有效难度、特殊倍率等字段均由基础难度派生，因此一处修改全局生效。
 * <p>
 * 本类含接管型 {@code @Overwrite}（{@code calculateDifficulty}），按 {@link MixinPriorities}
 * 惯例取最低优先级 {@code TAKE_OVER_FINAL}：接管体最先写入，其它模组的注入仍可叠加其上；
 * 本类的 {@code @Inject} / {@code @ModifyVariable} 也随之最先应用，取消式注入先执行，
 * 其它模组更高优先级的注入仍可在其返回值上继续改写（语义不受影响）。
 * <p>
 * 1.21.1 中 {@link DifficultyInstance} 构造函数签名由 1.20.1 的
 * {@code (Difficulty, Difficulty, long, long, float)} 简化为 {@code (Difficulty, long, long, float)}，
 * 第一个参数仍为基础难度（base），因此 ordinal = 0 的注入点不变。
 */
@Mixin(value = DifficultyInstance.class, priority = MixinPriorities.TAKE_OVER_FINAL)
public abstract class DifficultyInstanceMixin {

    @ModifyVariable(method = "<init>", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private static Difficulty difficultyOverhaul$forceHard(Difficulty base) {
        if (DifficultyOverhaulConfig.getMode() == DifficultyLevel.PEACEFUL) {
            return base;
        }
        return Difficulty.HARD;
    }

    /**
     * 原版 calculateDifficulty 存在两处“拍脑袋”分支（HARD 起步 0.75/其他 1.0、
     * EASY 额外 *0.5），这些分支依赖不可信的世界难度与本地当年月相，会让新版
     * 世界开通时难度漂移。此处移除这两处分支并使本地难度与进区块时间解耦。
     */
    @Overwrite
    private float calculateDifficulty(Difficulty difficulty, long levelTime, long chunkInhabitedTime, float moonPhaseFactor) {
        if (difficulty == Difficulty.PEACEFUL) {
            return 0.0F;
        } else {
            float f = 0.75F;
            float f1 = Mth.clamp(((float)levelTime + -72000.0F) / 1440000.0F, 0.0F, 1.0F) * 0.25F;
            f += f1;
            float f2 = 0.0F;
            f2 += Mth.clamp((float)chunkInhabitedTime / 3600000.0F, 0.0F, 1.0F) * 0.75F;
            f2 += Mth.clamp(moonPhaseFactor * 0.25F, 0.0F, f1);
            f += f2;
            return (float)difficulty.getId() * f;
        }
    }

    /**
     * 特殊事件倍率下限：原版 getSpecialMultiplier() 在有效难度 eff 位于
     * [2.0, 4.0] 时线性插值到 [0.0, 1.0]，低于 2.0 直接归零、高于 4.0 封顶 1.0。
     * 由于基础难度统一为 HARD，原版开局 eff ≥ 2.25，因此特殊倍率天然不低于 0.125；
     * 这里的保底进一步把“按模式配置的下限”与原值取 max——GOD 恒为 1.0（概率项恒满），
     * CASUAL/PEACEFUL 保底为 0.0（与无保底等效，概率完全随区块驻留时间浮动）。
     */
    @Inject(method = "getSpecialMultiplier", at = @At("HEAD"), cancellable = true)
    private void difficultyOverhaul$applySpecialMultiplierFloor(CallbackInfoReturnable<Float> cir) {
        float floor = (float) DifficultyOverhaulConfig.specialMultiplierFloor(DifficultyOverhaulConfig.getMode());
        if (floor <= 0.0F) {
            return;
        }
        float eff = ((DifficultyInstance) (Object) this).getEffectiveDifficulty();
        float vanilla = eff < 2.0F ? 0.0F : (eff > 4.0F ? 1.0F : (eff - 2.0F) / 2.0F);
        cir.setReturnValue(Math.max(vanilla, floor));
    }

    /**
     * 方案A：有效难度统一线性保底。
     * <p>
     * 原版 {@link DifficultyInstance#getEffectiveDifficulty()} 直接返回构造函数算好的
     * {@code effectiveDifficulty} 字段。由于基础难度统一为 HARD，该值天然不低于
     * 2.25，但随相对进度（世界时间/区块驻留/月相）仍会大范围浮动。这里在该返回值处做
     * 统一的线性映射：将当前模式配置的“特殊事件倍率下限 floor”换算成等价的有效难度
     * {@code 2.0 + 2.0*floor} 后取 max（因 getSpecialMultiplier 在 eff∈[2,4] 的区间
     * 恰好线性映射到 [0,1]）。这样全部 3 处直接读取有效难度的逻辑：
     * <ul>
     *   <li>僵尸点燃（点燃概率 eff*0.3、点燃秒数 (int)eff*2）</li>
     *   <li>尸壳饥饿（饥饿时长 (int)eff*140）</li>
     *   <li>骷髅陷阱马（雷暴中生成概率 eff*0.01）</li>
     * </ul>
     * 与 getSpecialMultiplier() 的保底语义完全对齐——GOD 模式下 floor=1.0，保底后
     * eff 恒 ≥ 4.0，上述概率项与特殊倍率项全部恒满；CASUAL/PEACEFUL 保底为 0.0
     * （等效于不处理，掉率、饥饿时长等完全随相对进度自然浮动）。
     */
    @Inject(method = "getEffectiveDifficulty", at = @At("RETURN"), cancellable = true)
    private void difficultyOverhaul$applyEffectiveDifficultyFloor(CallbackInfoReturnable<Float> cir) {
        float floor = (float) DifficultyOverhaulConfig.specialMultiplierFloor(DifficultyOverhaulConfig.getMode());
        if (floor <= 0.0F) {
            return;
        }
        cir.setReturnValue(Math.max(cir.getReturnValueF(), 2.0F + 2.0F * floor));
    }
}