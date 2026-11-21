package dev.difficultyoverhaul.mixin;

import dev.difficultyoverhaul.DifficultyLevel;
import dev.difficultyoverhaul.FollowRangeHelper;
import dev.difficultyoverhaul.config.DifficultyOverhaulConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 生物生成（finalizeSpawn）完成后，按当前模式应用血量/护甲倍率与怪物索敌视野。
 * <p>
 * 伤害倍率已移至 {@code PlayerHurtMixin}：在玩家受伤入口按原版
 * {@code scalesWithDifficulty()} 判定统一放大（近战/远程/爆炸等全部怪物伤害），
 * 不再修改攻击力属性，避免与生物属性/生成类模组冲突。血量/护甲仍在此
 * 直接乘以对应倍率，并恢复满血。
 * <p>
 * 索敌视野移植自 Farsighted Mobs 1.1（LGPL）：仅敌对生物（Monster）按当前模式的
 * follow_range 配置上调基础索敌距离，并刷新其 NearestAttackableTargetGoal 的目标判定。
 * 判定完全以配置值为准——默认 16 与原版一致（不增强），玩家把某模式改大（如普通改 40）
 * 即对该模式生效；只升不降，尊重其他模组/生物自身设置的更高索敌值。
 */
@Mixin(Mob.class)
public abstract class MobMixin {

    @Shadow
    protected GoalSelector targetSelector;

    @Inject(method = "finalizeSpawn", at = @At("RETURN"))
    private void difficultyOverhaul$applyModeScaling(ServerLevelAccessor level, DifficultyInstance difficulty,
                                                    MobSpawnType reason, SpawnGroupData spawnData,
                                                    CompoundTag dataTag, CallbackInfoReturnable<SpawnGroupData> cir) {
        DifficultyLevel mode = DifficultyOverhaulConfig.getMode();
        if (mode == DifficultyLevel.PEACEFUL) {
            return;
        }
        Mob self = (Mob) (Object) this;

        // 血量：直接乘倍率并恢复满血
        AttributeInstance health = self.getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            double base = health.getBaseValue();
            health.setBaseValue(base * DifficultyOverhaulConfig.healthMult(mode));
            self.setHealth(self.getMaxHealth());
        }

        // 护甲：直接乘倍率
        AttributeInstance armor = self.getAttribute(Attributes.ARMOR);
        if (armor != null) {
            armor.setBaseValue(armor.getBaseValue() * DifficultyOverhaulConfig.armorMult(mode));
        }

        // 索敌视野（核心逻辑映射 Farsighted Mobs 1.1 的 EntityJoinLevelEvent 处理）：
        // 仅敌对生物（Monster）；属性值低于该模式配置值时上调基础值（setBaseValue），
        // 随后无条件刷新全部 NearestAttackableTargetGoal 的目标判定（FixFollowRange），
        // 以当前属性值重建索敌距离。判定以配置值为准——默认 16 即原版不增强，
        // 玩家把某模式配置改大（如普通改 40）即对该模式生效；只升不降，尊重
        // 其他模组给生物设置的更高索敌值/属性修饰符。
        AttributeInstance followRange = self.getAttribute(Attributes.FOLLOW_RANGE);
        if (followRange != null && self instanceof Monster) {
            double target = DifficultyOverhaulConfig.followRange(mode);
            if (followRange.getValue() < target) {
                followRange.setBaseValue(target);
            }
            FollowRangeHelper.refresh(this.targetSelector, followRange.getValue());
        }
    }
}