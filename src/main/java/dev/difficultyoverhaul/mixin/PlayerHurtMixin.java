package dev.difficultyoverhaul.mixin;

import dev.difficultyoverhaul.DifficultyLevel;
import dev.difficultyoverhaul.config.DifficultyOverhaulConfig;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 玩家受伤入口统一处理两件事：难度判定与怪物伤害倍率。
 * <p>
 * <b>1. 难度判定按“普通模式”执行（和平模式除外）：</b><br>
 * 原版 {@link Player#hurt} 会按世界难度硬编码缩放伤害（和平归零、简单 ÷2+1、
 * 困难 ×1.5），而本模组所有非和平模式在游戏内一律按困难判定（世界难度与
 * DifficultyInstance 均强制为困难），若再叠加入口缩放会导致伤害双重计算。
 * 这里把 {@code Player.hurt} 内的难度读取重定向为普通，使伤害完全由
 * “原始伤害 × 当前模式倍率”单一来源决定（普通模式即与原版普通难度一致）。
 * 生成装备、附魔、刷怪等其余逻辑仍由 {@code DifficultyInstanceMixin} 强制为困难，
 * 不影响本模组“游戏内一律困难判定”的设计。
 * <p>
 * <b>2. 怪物伤害倍率：</b><br>
 * 修改 {@code hurt} 内对 {@link net.minecraft.world.entity.LivingEntity#hurt} 调用的伤害参数
 * （Forge patch 版已将护甲减伤逻辑内联进 {@code LivingEntity.hurt}，故以
 * {@code invokespecial LivingEntity.hurt} 为注入点，直接修改其伤害参数）。
 * 注意：{@code @ModifyArg} 的 handler 参数列表必须与目标调用参数
 * {@code (DamageSource, float)} 完全一致（顺序不可颠倒），新值通过返回值传回。
 * 放大判定分两部分：
 * <ul>
 *   <li>原版 {@link DamageSource#scalesWithDifficulty()} 为真的伤害：非玩家生物
 *       直接造成的伤害（近战、魔法、荆棘反伤等）与 ALWAYS 类（爆炸、声波）；</li>
 *   <li>补充修复原版缺陷：伤害造成者（{@link DamageSource#getEntity()}，即弹射物的
 *       发射者/施法者）为非玩家生物的伤害。1.20.1 的 {@code scalesWithDifficulty()}
 *       只检查直接实体（directEntity）是否为生物，而怪物发射的弹射物（箭、火球、
 *       凋灵头颅、女巫药水等）其 directEntity 是弹射物本身（非生物），导致这些
 *       伤害即使由怪物发射也不随难度缩放。</li>
 * </ul>
 * 天然排除玩家间伤害（玩家攻击、玩家射出的箭因造成者为玩家不放大）与摔落/溺水/
 * 窒息/虚空/饥饿等无造成者的环境伤害；TNT 自爆（无造成者）按 ALWAYS 保留原版放大。
 * 和平模式不放大，免疫由重定向后保留的原版和平判定保证。
 */
@Mixin(Player.class)
public abstract class PlayerHurtMixin {

    @Redirect(method = "hurt",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;getDifficulty()Lnet/minecraft/world/Difficulty;"))
    private Difficulty difficultyOverhaul$forceNormalForPlayerHurt(Level level) {
        if (DifficultyOverhaulConfig.getMode() == DifficultyLevel.PEACEFUL) {
            // 和平模式：保留原版判定，让伤害按和平规则归零
            return level.getDifficulty();
        }
        return Difficulty.NORMAL;
    }

    @ModifyArg(method = "hurt",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"),
            index = 1)
    private float difficultyOverhaul$scaleMonsterDamage(DamageSource source, float amount) {
        DifficultyLevel mode = DifficultyOverhaulConfig.getMode();
        if (mode == DifficultyLevel.PEACEFUL) {
            return amount;
        }
        // 修复原版缺陷：scalesWithDifficulty() 只看直接实体（directEntity）是否为生物，
        // 怪物发射的弹射物（directEntity 是弹射物本身）被漏判。补充判定——
        // 伤害造成者（source.getEntity()，即弹射物发射者/施法者）为非玩家生物时同样放大。
        // 玩家射出的箭：造成者是玩家 → 不放大；怪物射出的箭/火球/凋灵头颅/药水：放大。
        if (source.scalesWithDifficulty()
                || (source.getEntity() instanceof LivingEntity causing && !(causing instanceof Player))) {
            return amount * (float) DifficultyOverhaulConfig.damageMult(mode);
        }
        return amount;
    }
}
