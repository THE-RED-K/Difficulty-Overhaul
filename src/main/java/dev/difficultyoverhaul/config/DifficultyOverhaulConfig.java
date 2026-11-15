package dev.difficultyoverhaul.config;

import dev.difficultyoverhaul.DifficultyLevel;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.EnumMap;
import java.util.Map;

/**
 * 模组通用（COMMON，服务器权威）配置，固定生成在 {@code .minecraft/config} 的
 * {@code difficulty_overhaul-common.toml}，便于整合包作者直接编辑。
 * <p>
 * 包含：当前模式、每个模式的伤害/血量/护甲倍率，
 * 以及三个全局统一倍率开关与对应的全局倍率。
 * <p>
 * 说明：采用 COMMON 而非 SERVER——SERVER 会按世界写入存档 serverconfig、且在 config
 * 目录不残留模板；COMMON 让配置固定落在 config 目录。代价是 COMMON 不会自动同步到
 * 联机客户端（各自读本地文件）。
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

    /** 创建世界界面游戏规则默认值功能总开关（默认开启） */
    public static final ForgeConfigSpec.BooleanValue WORLD_CREATION_DEFAULTS_ENABLED;
    /** 保留背包 keepInventory 默认值（默认 true） */
    public static final ForgeConfigSpec.BooleanValue WORLD_CREATION_KEEP_INVENTORY;
    /** 生物破坏 mobGriefing 默认值（默认 false） */
    public static final ForgeConfigSpec.BooleanValue WORLD_CREATION_MOB_GRIEFING;
    /** 无差别愤怒 universalAnger 默认值（默认 false，即维持原版） */
    public static final ForgeConfigSpec.BooleanValue WORLD_CREATION_UNIVERSAL_ANGER;
    /** 宽恕死亡玩家 forgiveDeadPlayers 默认值（默认 false，即维持原版） */
    public static final ForgeConfigSpec.BooleanValue WORLD_CREATION_FORGIVE_DEAD_PLAYERS;
    /** 钱包模组 keepWallet 默认值（默认 true；需可选前置 lightmanscurrency） */
    public static final ForgeConfigSpec.BooleanValue WORLD_CREATION_KEEP_WALLET;

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

        clientBuilder.comment("创建世界/导入旧世界时默认游戏规则的预设值，",
                        "在“选项 / 游戏规则”界面进世界前即可看到并可修改（不剥夺选择权）。")
                .push("world_creation_defaults");
        WORLD_CREATION_DEFAULTS_ENABLED = clientBuilder
                .comment("创建世界界面游戏规则预设总开关，关闭即回到原版新建世界默认值。")
                .define("enabled", true);
        WORLD_CREATION_KEEP_INVENTORY = clientBuilder
                .comment("保留背包（keepInventory）游戏规则默认值。默认 true。")
                .define("keep_inventory", true);
        WORLD_CREATION_MOB_GRIEFING = clientBuilder
                .comment("生物破坏（mobGriefing）游戏规则默认值。默认 false。")
                .define("mob_griefing", false);
        WORLD_CREATION_UNIVERSAL_ANGER = clientBuilder
                .comment("无差别愤怒（universalAnger）游戏规则默认值；",
                        "true 时敌对生物会无视激怒来源，攻击半径内任意玩家。默认 false（维持原版）。")
                .define("universal_anger", false);
        WORLD_CREATION_FORGIVE_DEAD_PLAYERS = clientBuilder
                .comment("宽恕死亡玩家（forgiveDeadPlayers）游戏规则默认值；",
                        "true 时怪物在目标玩家死亡后不再持续愤怒。默认 true（维持原版）。")
                .define("forgive_dead_players", true);
        WORLD_CREATION_KEEP_WALLET = clientBuilder
                .comment("钱包模组（Lightman's Currency）keepWallet 游戏规则默认值；",
                        "仅当 lightmanscurrency 作为可选前置装载时生效。默认 true。")
                .define("keep_wallet", true);
        clientBuilder.pop();
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