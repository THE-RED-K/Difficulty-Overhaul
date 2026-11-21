package dev.difficultyoverhaul.mixin;

import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 索敌判定访问器（参考 Farsighted Mobs 3.0.2 的 @Accessor 实现，LGPL）。
 * <p>
 * 原版 {@code NearestAttackableTargetGoal} 在构造时把 {@code getFollowDistance()} 读到的
 * 索敌距离一次性写入 targetConditions，之后不再跟随 FOLLOW_RANGE 属性变化；
 * 上调属性后必须手动按新距离重建该判定，生物才会真正按新距离索敌。
 * <p>
 * 依赖注入：
 * 无法在另一个 Mixin 类的方法体内直接强转此 Mixin 类型——Mixin 处理器会将
 * {@code CHECKCAST} 目标在目标类的继承树中解析，而 {@code NearestAttackableTargetGoal}
 * 不在 {@code Mob} 的层级内，会抛 {@code Resolution error}。因此遍历与重建逻辑
 * 放在普通工具类 {@code FollowRangeHelper} 中，仅实现此访问器接口。
 */
@Mixin(NearestAttackableTargetGoal.class)
public interface NearestAttackableTargetGoalMixin {

    @Accessor("targetConditions")
    TargetingConditions getTargetConditions();

    @Accessor("targetConditions")
    void setTargetConditions(TargetingConditions targetConditions);
}