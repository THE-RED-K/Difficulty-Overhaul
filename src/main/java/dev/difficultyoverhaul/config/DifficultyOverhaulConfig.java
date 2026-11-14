package dev.difficultyoverhaul.config;

import dev.difficultyoverhaul.DifficultyLevel;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.EnumMap;
import java.util.Map;

/**
 * 模组服务端配置（自动从服务器同步到客户端）。
 * <p>
 * 包含：当前模式、每个模式的伤害/血量/护甲倍率，
 * 以及三个全局统一倍率开关与对应的全局倍率。
 */
public final class DifficultyOverhaulConfig {
    public static final ForgeConfigSpec SPEC;

    /** 当前生效的模式（字符串键） */
    public static final ForgeConfigSpec.ConfigValue<String> MODE;

    private static final Map<DifficultyLevel, ForgeConfigSpec.ConfigValue<Double>> DAMAGE_MULTS = new EnumMap<>(DifficultyLevel.class);
    private static final Map<DifficultyLevel, ForgeConfigSpec.ConfigValue<Double>> HEALTH_MULTS = new EnumMap<>(DifficultyLevel.class);
    private static final Map<DifficultyLevel, ForgeConfigSpec.ConfigValue<Double>> ARMOR_MULTS = new EnumMap<>(DifficultyLevel.class);

    /** 三个大开关：开启后所有模式统一使用下方全局倍率 */
    public static final ForgeConfigSpec.BooleanValue GLOBAL_DAMAGE_ENABLED;
    public static final ForgeConfigSpec.BooleanValue GLOBAL_HEALTH_ENABLED;
    public static final ForgeConfigSpec.BooleanValue GLOBAL_ARMOR_ENABLED;

    public static final ForgeConfigSpec.DoubleValue GLOBAL_DAMAGE_MULT;
    public static final ForgeConfigSpec.DoubleValue GLOBAL_HEALTH_MULT;
    public static final ForgeConfigSpec.DoubleValue GLOBAL_ARMOR_MULT;

    /** 客户端配置：记住客户端当前选择的模式（创建世界界面与选项界面的初始值） */
    public static final ForgeConfigSpec CLIENT_SPEC;

    /** 客户端记住的模式（字符串键） */
    public static final ForgeConfigSpec.ConfigValue<String> CLIENT_MODE;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        MODE = builder
                .comment("当前生效的模式（world difficulty preset）",
                        "可选值: " + modeNames())
                .define("mode", DifficultyLevel.NORMAL.getKey());

        builder.comment("各模式倍率（相对普通模式）。",
                        "伤害倍率在玩家受伤入口按原版 scalesWithDifficulty() 判定统一放大怪物伤害",
                        "（非玩家生物造成的近战/远程/爆炸，以及火焰/岩浆/仙人掌等 ALWAYS 类伤害；",
                        "玩家间伤害与摔落/溺水等 NEVER 环境伤害不缩放），普通模式即与原版普通难度一致。",
                        "血量/护甲倍率直接乘以生成时的基础属性值。")
                .push("mode_multipliers");
        for (DifficultyLevel level : DifficultyLevel.values()) {
            builder.push(level.getKey());
            DAMAGE_MULTS.put(level, builder.define("damage_multiplier", level.getDefaultDamageMult()));
            HEALTH_MULTS.put(level, builder.define("health_multiplier", level.getDefaultHealthMult()));
            ARMOR_MULTS.put(level, builder.define("armor_multiplier", level.getDefaultArmorMult()));
            builder.pop();
        }
        builder.pop();

        builder.comment("全局统一倍率开关：开启后无视当前模式，统一使用下方全局倍率（仍可配置）。")
                .push("global");
        GLOBAL_DAMAGE_ENABLED = builder.comment("开启后所有模式伤害统一按全局伤害倍率。").define("damage_enabled", false);
        GLOBAL_HEALTH_ENABLED = builder.comment("开启后所有模式血量统一按全局血量倍率。").define("health_enabled", false);
        GLOBAL_ARMOR_ENABLED = builder.comment("开启后所有模式护甲统一按全局护甲倍率。").define("armor_enabled", false);
        GLOBAL_DAMAGE_MULT = builder.comment("全局伤害倍率。").defineInRange("damage_multiplier", 1.0D, 0.0D, 100.0D);
        GLOBAL_HEALTH_MULT = builder.comment("全局血量倍率。").defineInRange("health_multiplier", 1.0D, 0.0D, 100.0D);
        GLOBAL_ARMOR_MULT = builder.comment("全局护甲倍率。").defineInRange("armor_multiplier", 1.0D, 0.0D, 100.0D);
        builder.pop();

        SPEC = builder.build();

        ForgeConfigSpec.Builder clientBuilder = new ForgeConfigSpec.Builder();
        CLIENT_MODE = clientBuilder
                .comment("客户端记住的当前模式（创建世界界面与选项界面的初始值；",
                        "集成服务器新建世界时以此作为初始模式并写入服务器配置）")
                .define("mode", DifficultyLevel.NORMAL.getKey());
        CLIENT_SPEC = clientBuilder.build();
    }

    private DifficultyOverhaulConfig() {
    }

    private static String modeNames() {
        StringBuilder sb = new StringBuilder();
        for (DifficultyLevel level : DifficultyLevel.values()) {
            sb.append(level.getKey()).append(' ');
        }
        return sb.toString().trim();
    }

    /** 当前模式，解析失败时回退到普通。 */
    public static DifficultyLevel getMode() {
        DifficultyLevel level = DifficultyLevel.byKey(MODE.get());
        return level == null ? DifficultyLevel.NORMAL : level;
    }

    /** 由服务器在模式切换时调用。 */
    public static void setMode(DifficultyLevel level) {
        MODE.set(level.getKey());
    }

    /** 某模式最终生效的伤害倍率（全局开关优先）。 */
    public static double damageMult(DifficultyLevel level) {
        return GLOBAL_DAMAGE_ENABLED.get() ? GLOBAL_DAMAGE_MULT.get() : DAMAGE_MULTS.get(level).get();
    }

    /** 某模式最终生效的血量倍率（全局开关优先）。 */
    public static double healthMult(DifficultyLevel level) {
        return GLOBAL_HEALTH_ENABLED.get() ? GLOBAL_HEALTH_MULT.get() : HEALTH_MULTS.get(level).get();
    }

    /** 某模式最终生效的护甲倍率（全局开关优先）。 */
    public static double armorMult(DifficultyLevel level) {
        return GLOBAL_ARMOR_ENABLED.get() ? GLOBAL_ARMOR_MULT.get() : ARMOR_MULTS.get(level).get();
    }

    /** 客户端记住的模式，解析失败时回退到普通。 */
    public static DifficultyLevel getClientMode() {
        DifficultyLevel level = DifficultyLevel.byKey(CLIENT_MODE.get());
        return level == null ? DifficultyLevel.NORMAL : level;
    }

    /** 客户端选择模式（创建世界界面/选项界面按钮回调）。 */
    public static void setClientMode(DifficultyLevel level) {
        CLIENT_MODE.set(level.getKey());
    }
}
