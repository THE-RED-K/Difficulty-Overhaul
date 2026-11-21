package dev.difficultyoverhaul.mixin;

import dev.difficultyoverhaul.config.DifficultyOverhaulConfig;
import net.minecraft.world.entity.monster.Zombie;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * 僵尸鸡骑士生成概率配置化。
 * <p>
 * 实证反编译：1.20.1 原版 {@code Zombie#finalizeSpawn} 中仅在两处出现字面量
 * {@code 0.05D}，分别是：
 * <ul>
 *   <li>“骑乘附近已有鸡”（{@code if (nextFloat() < 0.05D)}）</li>
 *   <li>“生成新鸡并骑乘”（{@code else if (nextFloat() < 0.05D)}）</li>
 * </ul>
 * 两处均为独立、无难度耦合的掷骰。此处用 {@code @ModifyConstant} 统一替换为配置项
 * {@code zombie.chicken_jockey_chance}；配置为 0.0 时两处恒为 false，小僵尸照常生成
 * 但鸡骑士彻底消失。
 * <p>
 * 注意：1.20.1 Forge 已内置小僵尸概率配置 {@code zombieBabyChance}
 * （{@code ForgeConfig.SERVER.zombieBabyChance}），原版 {@code getSpawnAsBabyOdds}
 * 直接读取它，因此小僵尸概率无需本模组重复配置。
 */
@Mixin(Zombie.class)
public abstract class ZombieMixin {

    @ModifyConstant(method = "finalizeSpawn", constant = @Constant(doubleValue = 0.05D))
    private double difficultyOverhaul$applyChickenJockeyChance(double original) {
        return DifficultyOverhaulConfig.zombieChickenJockeyChance();
    }
}