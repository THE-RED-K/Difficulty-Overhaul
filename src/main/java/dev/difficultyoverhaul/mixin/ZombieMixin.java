package dev.difficultyoverhaul.mixin;

import dev.difficultyoverhaul.config.DifficultyOverhaulConfig;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.monster.Zombie;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 小僵尸生成概率拦截：原版 {@link Zombie#getSpawnAsBabyOdds(RandomSource)}
 * 固定返回掷骰 {@code random.nextFloat() < 0.05F}（即 5%），这里改为读取配置
 * {@code baby_zombie_chance} 并按该概率掷骰。
 * <p>
 * 该静态方法在 {@code Zombie#finalizeSpawn} 中被调用（见原版反编译），
 * 是原版决定"这只僵尸是否是小僵尸"的唯一入口；
 * 配置为 0.0 时 {@code nextFloat() < 0.0F} 恒为 false，小僵尸完全无法生成。
 * 在 HEAD 处直接替换返回值，副作用与原版一致（仍消费一次随机掷骰）。
 * <p>
 * 僵尸鸡骑士生成概率拦截：原版 {@code Zombie#finalizeSpawn} 中“骑乘附近已有鸡”
 * 与“生成新鸡”两处分别独立掷骰的概率均为 {@code random.nextFloat() < 0.05D}
 * （见原版反编译），这里用 {@code @ModifyConstant} 将两处 0.05D 统一替换为配置
 * {@code zombie.chicken_jockey_chance}；配置为 0.0 时鸡骑士彻底不再生成。
 */
@Mixin(Zombie.class)
public abstract class ZombieMixin {

    @Inject(method = "getSpawnAsBabyOdds", at = @At("HEAD"), cancellable = true)
    private static void difficultyOverhaul$applyBabySpawnChance(
            RandomSource random, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(random.nextFloat() < (float) DifficultyOverhaulConfig.babyZombieChance());
    }

    @ModifyConstant(method = "finalizeSpawn", constant = @Constant(doubleValue = 0.05D))
    private double difficultyOverhaul$applyChickenJockeyChance(double original) {
        return DifficultyOverhaulConfig.zombieChickenJockeyChance();
    }
}