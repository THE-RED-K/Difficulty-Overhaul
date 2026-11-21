package dev.difficultyoverhaul.mixin;

import dev.difficultyoverhaul.config.DifficultyOverhaulConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.level.GameRules;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;

/**
 * 创建世界（或导入旧世界）时，在“选项 / 游戏规则”界面把若干游戏规则默认值设为对应值，
 * 让玩家进世界前就能看到并可修改（不剥夺选择权）。
 *
 * <p>依据：1.21.1 新建/导入世界的 gamerules 真相源是 {@link WorldCreationUiState} 的 {@code gameRules} 字段，
 * 构造时由 {@code new GameRules(GameRules.DEFAULT)} 初始化（独立副本，可安全替换）。
 * 仅当仍为原版默认（mobGriefing=true）时套用，避免覆盖导入旧世界的既有设定；
 * 导入旧世界会在构造后调用 setGameRules 覆盖，不受影响。
 *
 * <p>配置（见 {@link DifficultyOverhaulConfig}，均为客户端配置）：
 * <ul>
 *     <li>world_creation_defaults.enabled（默认 true）总开关。</li>
 *     <li>world_creation_defaults.keep_inventory（保留背包，默认 true）。</li>
 *     <li>world_creation_defaults.mob_griefing（生物破坏，默认 false）。</li>
 *     <li>world_creation_defaults.universal_anger（无差别愤怒，默认 false）。</li>
 *     <li>world_creation_defaults.forgive_dead_players（宽恕死亡玩家，默认 false）。</li>
 *     <li>world_creation_defaults.keep_wallet（钱包模组 keepWallet，默认 true；可选前置 lightmanscurrency）。</li>
 * </ul>
 *
 * <p>注意：仅客户端（创建界面）生效，故本 mixin 配置声明 {@code "side": "CLIENT"}。
 * 单人与整合服务端均经由客户端 LevelSettings 传递，新世界落地即为设定值；
 * 专用服务端无创建界面时维持各自默认（符合预期）。
 */
@Mixin(WorldCreationUiState.class)
public abstract class WorldCreationDefaultsMixin {

    @Shadow
    private GameRules gameRules;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void applyDefaultGameRules(CallbackInfo ci) {
        // 可选内容开关：关掉即回到原版新建世界默认值
        if (!DifficultyOverhaulConfig.WORLD_CREATION_DEFAULTS_ENABLED.get()) {
            return;
        }
        GameRules gr = this.gameRules;
        // 仍为原版默认（mobGriefing 默认 true）才套用，避免覆盖导入旧世界的既有设定。
        // gameRules 字段是构造时 new GameRules()（或 assignFrom）得到的独立实例，可安全原地修改。
        if (gr.getRule(GameRules.RULE_MOBGRIEFING).get()) {
            // 每条游戏规则默认值均可单独配置
            gr.getRule(GameRules.RULE_KEEPINVENTORY).set(DifficultyOverhaulConfig.WORLD_CREATION_KEEP_INVENTORY.get(), null);
            gr.getRule(GameRules.RULE_MOBGRIEFING).set(DifficultyOverhaulConfig.WORLD_CREATION_MOB_GRIEFING.get(), null);
            // 无差别愤怒：默认由配置决定（默认 false）
            gr.getRule(GameRules.RULE_UNIVERSAL_ANGER).set(DifficultyOverhaulConfig.WORLD_CREATION_UNIVERSAL_ANGER.get(), null);
            // 宽恕死亡玩家：默认由配置决定（默认 false）
            gr.getRule(GameRules.RULE_FORGIVE_DEAD_PLAYERS).set(DifficultyOverhaulConfig.WORLD_CREATION_FORGIVE_DEAD_PLAYERS.get(), null);
            // 钱包模组 keepWallet：lightmanscurrency 为可选前置，未加载时保持默认。
            // 注意：这里必须用反射访问 ModGameRules.KEEP_WALLET，不能出现对 lightmanscurrency
            // 的字节码级静态引用——Mixin 在织入阶段解析 handler 字节码中的该字段引用时，
            // 若 lightmanscurrency 未安装（fieldOwner == null）会直接抛 NPE 导致整个 mixin
            // 应用失败，创建世界界面随之卡死，try/catch 无法捕获（该异常发生在织入期而非运行期）。
            try {
                Class<?> modGameRulesClass = Class.forName("io.github.lightman314.lightmanscurrency.common.gamerule.ModGameRules");
                Object keepWalletKey = modGameRulesClass.getField("KEEP_WALLET").get(null);
                if (keepWalletKey instanceof GameRules.Key<?>) {
                    @SuppressWarnings({"unchecked", "rawtypes"})
                    Object ruleValue = gr.getRule((GameRules.Key) keepWalletKey);
                    if (ruleValue instanceof GameRules.BooleanValue rule) {
                        rule.set(DifficultyOverhaulConfig.WORLD_CREATION_KEEP_WALLET.get(), null);
                    }
                }
            } catch (Throwable ignored) {
                // lightmanscurrency 未加载或无此规则：保持默认即可
            }
        }
    }
}