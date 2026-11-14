package dev.difficultyoverhaul.mixin;

import dev.difficultyoverhaul.DifficultyLevel;
import dev.difficultyoverhaul.config.DifficultyOverhaulConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 生物生成（finalizeSpawn）完成后，按当前模式应用血量/护甲倍率。
 * <p>
 * 伤害倍率已移至 {@code PlayerHurtMixin}：在玩家受伤入口按原版
 * {@code scalesWithDifficulty()} 判定统一放大（近战/远程/爆炸等全部怪物伤害），
 * 不再修改攻击力属性，避免与生物属性/生成类模组冲突。血量/护甲仍在此
 * 直接乘以对应倍率，并恢复满血。
 */
@Mixin(Mob.class)
public abstract class MobMixin {

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
    }
}
