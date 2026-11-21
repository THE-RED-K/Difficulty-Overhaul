package dev.difficultyoverhaul;

import dev.difficultyoverhaul.mixin.NearestAttackableTargetGoalMixin;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;

import java.util.ArrayList;

/**
 * 索敌判定刷新工具（对应 Farsighted Mobs 的 FixFollowRange / AttributeChanger，LGPL）。
 * <p>
 * 该辅助类必须为普通类而非 Mixin：交叉引用 Mixin 接口类型的强转（CHECKCAST）
 * 在普通类中不会被 Mixin 处理器重写，Mixin 处理器随后将接口注入到目标类
 * {@code NearestAttackableTargetGoal} 上，运行时 {@code instanceof} / 强转天然成立；
 * 而在 Mixin 类方法体内这样做则会触发 "unable to find corresponding type" 崩溃。
 */
public final class FollowRangeHelper {

    private FollowRangeHelper() {
    }

    /** 按新索敌距离重建该选择器中全部 NearestAttackableTargetGoal 的目标判定。 */
    public static void refresh(GoalSelector targetSelector, double followRange) {
        for (WrappedGoal wrapped : new ArrayList<>(targetSelector.getAvailableGoals())) {
            Goal goal = wrapped.getGoal();
            if (goal instanceof NearestAttackableTargetGoal<?> targetGoal) {
                NearestAttackableTargetGoalMixin accessor = (NearestAttackableTargetGoalMixin) (Object) targetGoal;
                accessor.setTargetConditions(accessor.getTargetConditions().range(followRange));
            }
        }
    }
}