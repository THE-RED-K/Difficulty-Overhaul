package dev.difficultyoverhaul.config;

import dev.difficultyoverhaul.DifficultyLevel;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 模组通用（COMMON，服务器权威）配置，固定生成在 {@code .minecraft/config} 的
 * {@code difficulty_overhaul-common.toml}，便于整合包作者直接编辑。
 * <p>
 * 包含：当前模式、每个模式的伤害/血量/护甲倍率、怪物索敌视野与特殊事件倍率下限、
 * 三个全局统一倍率开关与对应的全局倍率、模式随进度自动演进的整套开关与生物列表、
 * 小僵尸生成概率，以及僵尸鸡骑士生成概率。
 * <p>
 * 说明：采用 COMMON 而非 SERVER——SERVER 会按世界写入存档 serverconfig、且在 config
 * 目录不残留模板；COMMON 让配置固定落在 config 目录。代价是 COMMON 不会自动同步到
 * 联机客户端（各自读本地文件）。
 */
public final class DifficultyOverhaulConfig {
    public static final ModConfigSpec SPEC;

    /** 当前生效的模式（字符串键） */
    public static final ModConfigSpec.ConfigValue<String> MODE;

    private static final Map<DifficultyLevel, ModConfigSpec.ConfigValue<Double>> DAMAGE_MULTS = new EnumMap<>(DifficultyLevel.class);
    private static final Map<DifficultyLevel, ModConfigSpec.ConfigValue<Double>> HEALTH_MULTS = new EnumMap<>(DifficultyLevel.class);
    private static final Map<DifficultyLevel, ModConfigSpec.ConfigValue<Double>> ARMOR_MULTS = new EnumMap<>(DifficultyLevel.class);
    private static final Map<DifficultyLevel, ModConfigSpec.ConfigValue<Double>> FOLLOW_RANGES = new EnumMap<>(DifficultyLevel.class);
    private static final Map<DifficultyLevel, ModConfigSpec.ConfigValue<Double>> SPECIAL_MULTIPLIER_FLOORS = new EnumMap<>(DifficultyLevel.class);

    /** 三个大开关：开启后所有模式统一使用下方全局倍率 */
    public static final ModConfigSpec.BooleanValue GLOBAL_DAMAGE_ENABLED;
    public static final ModConfigSpec.BooleanValue GLOBAL_HEALTH_ENABLED;
    public static final ModConfigSpec.BooleanValue GLOBAL_ARMOR_ENABLED;

    public static final ModConfigSpec.DoubleValue GLOBAL_DAMAGE_MULT;
    public static final ModConfigSpec.DoubleValue GLOBAL_HEALTH_MULT;
    public static final ModConfigSpec.DoubleValue GLOBAL_ARMOR_MULT;

    /**
     * 模式随进度自动演进总开关（默认关闭）。关闭后本小节以下所有配置项全部失效，
     * 即不会监听 Boss 击杀、不考虑 forge:bosses 标签与自定义生物列表，也不锁定难度。
     */
    public static final ModConfigSpec.BooleanValue AUTO_PROGRESSION_ENABLED;
    /**
     * 击杀拥有 forge:bosses 标签的生物是否触发难度演进（默认开启）。
     * 该标签由 NeoForge 预置并在数据包中的 data/neoforge/tags/entity_types/bosses.json 定义，
     * 通常包含原版的凋灵/末影龙；整合包可在其中追加监守者与远古守卫者等。
     * 关闭本开关后，只有下方 {@link #AUTO_PROGRESSION_BOSSES} 列表中的生物能触发演进。
     */
    public static final ModConfigSpec.BooleanValue AUTO_PROGRESSION_USE_FORGE_BOSSES_TAG;
    /**
     * 触发难度演进的额外生物注册 ID 列表（默认空）。击杀列表中任一生物都会应用自动演进，
     * 不受 forge:bosses 标签开关影响。填写形如 minecraft:wither / modid:boss 的注册 ID。
     */
    public static final ModConfigSpec.ConfigValue<List<? extends String>> AUTO_PROGRESSION_BOSSES;
    /**
     * 自动演进期间锁定难度（默认开启）：生效时玩家无法手动调整模式
     * （相当于原版的难度锁按钮开启，命令与按钮都会被拦截），只能由击杀 Boss 自动演进。
     */
    public static final ModConfigSpec.BooleanValue AUTO_PROGRESSION_LOCK_DIFFICULTY;

    /**
     * 小僵尸生成概率（默认 0.05，即原版 5%）。范围 0.0~1.0；
     * 配置为 0.0 时小僵尸完全无法生成（原版每只僵尸生成时独立掷骰）。
     * 可供整合包作者调高或关闭该概率。
     */
    public static final ModConfigSpec.DoubleValue BABY_ZOMBIE_CHANCE;

    /**
     * 僵尸鸡骑士生成概率（全局统一，默认 0.05 即原版 5%）。范围 0.0~1.0。
     * 原版在 Zombie#finalizeSpawn 中对“骑乘附近已有鸡”与“生成新鸡”分别独立
     * 掷骰的概率均为 5%；此处将两处统一替换为该配置值。设为 0.0 时鸡骑士彻底
     * 不再生成，但小僵尸本身不受影响。
     */
    public static final ModConfigSpec.DoubleValue ZOMBIE_CHICKEN_JOCKEY_CHANCE;

    /** 客户端配置：记住客户端当前选择的模式（创建世界界面与选项界面的初始值） */
    public static final ModConfigSpec CLIENT_SPEC;

    /** 客户端记住的模式（字符串键） */
    public static final ModConfigSpec.ConfigValue<String> CLIENT_MODE;

    /** 创建世界界面游戏规则默认值功能总开关（默认开启） */
    public static final ModConfigSpec.BooleanValue WORLD_CREATION_DEFAULTS_ENABLED;
    /** 保留背包 keepInventory 默认值（默认 true） */
    public static final ModConfigSpec.BooleanValue WORLD_CREATION_KEEP_INVENTORY;
    /** 生物破坏 mobGriefing 默认值（默认 false） */
    public static final ModConfigSpec.BooleanValue WORLD_CREATION_MOB_GRIEFING;
    /** 无差别愤怒 universalAnger 默认值（默认 false，即维持原版） */
    public static final ModConfigSpec.BooleanValue WORLD_CREATION_UNIVERSAL_ANGER;
    /** 宽恕死亡玩家 forgiveDeadPlayers 默认值（默认 true，即维持原版） */
    public static final ModConfigSpec.BooleanValue WORLD_CREATION_FORGIVE_DEAD_PLAYERS;
    /** 钱包模组 keepWallet 默认值（默认 true；需可选前置 lightmanscurrency） */
    public static final ModConfigSpec.BooleanValue WORLD_CREATION_KEEP_WALLET;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        MODE = builder
                .comment("当前生效的模式（world difficulty preset）",
                        "可选值: " + modeNames())
                .define("mode", DifficultyLevel.NORMAL.getKey());

        builder.comment("各模式倍率（相对普通模式）。",
                        "伤害倍率在玩家受伤入口按原版 scalesWithDifficulty() 判定统一放大怪物伤害",
                        "（非玩家生物造成的近战/远程/爆炸，以及火焰/岩浆/仙人掌等 ALWAYS 类伤害；",
                        "玩家间伤害与摔落/溺水等 NEVER 环境伤害不缩放），普通模式即与原版普通难度一致。",
                        "血量/护甲倍率直接乘以生成时的基础属性值。",
                        "索敌视野 follow_range 为敌对生物（Monster）生成时的基础索敌距离（格）；",
                        "默认 16 即与原版一致（不增强），改大即生效，如普通模式改为 40 则普通难度的生物也会按 40 格索敌。")
                .push("mode_multipliers");
        for (DifficultyLevel level : DifficultyLevel.values()) {
            builder.push(level.getKey());
            DAMAGE_MULTS.put(level, builder.define("damage_multiplier", level.getDefaultDamageMult()));
            HEALTH_MULTS.put(level, builder.define("health_multiplier", level.getDefaultHealthMult()));
            ARMOR_MULTS.put(level, builder.define("armor_multiplier", level.getDefaultArmorMult()));
            FOLLOW_RANGES.put(level, builder.define("follow_range", level.getDefaultFollowRange()));
            SPECIAL_MULTIPLIER_FLOORS.put(level, builder.comment("特殊事件倍率下限（0.0~1.0）。",
                            "所有按原版 getSpecialMultiplier() 缩放的概率值（如僵尸拾取战利品、",
                            "点燃/饥饿时长、骷髅陷阱马生成等）最终取值不低于该下限；GOD 设为 1.0 即恒满。")
                    .defineInRange("special_multiplier_floor", level.getDefaultSpecialMultiplierFloor(), 0.0D, 1.0D));
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

        builder.comment("模式随进度自动演进：击败 Boss 或列表中的生物后自动提升模式难度。",
                        "总开关关闭后，本小节其余配置项全部失效。",
                        "注意：当前模式为和平时自动禁用演进（击杀 Boss 不会提升难度，难度锁同时失效，",
                        "以便玩家手动切回其他模式）；神明（最高档）封顶不再提升。")
                .push("auto_progression");
        AUTO_PROGRESSION_ENABLED = builder
                .comment("模式随进度自动演进总开关。默认关闭；关闭后本小节其余配置全部失效。")
                .define("enabled", false);
        AUTO_PROGRESSION_USE_FORGE_BOSSES_TAG = builder
                .comment("击杀拥有 forge:bosses 标签的生物是否触发难度演进。默认开启。",
                        "NeoForge 预置该标签，整合包可在 data/neoforge/tags/entity_types/bosses.json 中追加",
                        "监守者/远古守卫者等自有 Boss；关闭后仅下方 boss_list 中的生物能触发演进。")
                .define("use_forge_bosses_tag", true);
        AUTO_PROGRESSION_BOSSES = builder
                .comment("触发难度演进的生物注册 ID 列表（默认空）。",
                        "填写形如 minecraft:wither / modid:boss 的注册 ID，击杀列表内生物即应用自动演进，",
                        "不受 use_forge_bosses_tag 开关影响。")
                .defineList("boss_list", new ArrayList<>(), element -> element instanceof String);
        AUTO_PROGRESSION_LOCK_DIFFICULTY = builder
                .comment("自动演进期间锁定难度：玩家无法手动调整模式（相当于原版的难度锁按钮开启），",
                        "命令与按钮都会被拦截，只能由击杀 Boss 自动演进。默认开启。")
                .define("lock_difficulty", true);
        builder.pop();

        BABY_ZOMBIE_CHANCE = builder
                .comment("小僵尸生成概率（0.0~1.0，默认 0.05 即原版 5%）。",
                        "每只僵尸在生成时按该概率独立掷骰决定是否为小僵尸；",
                        "配置为 0.0 时小僵尸完全无法生成（仅影响原版掷骰入口，兼容小僵尸变体）。")
                .defineInRange("baby_zombie_chance", 0.05D, 0.0D, 1.0D);

        builder.comment("僵尸鸡骑士生成概率（所有模式统一）。",
                        "原版在 Zombie#finalizeSpawn 中对“骑乘附近已有鸡”与“生成新鸡”分别独立掷骰的概率均为 5%",
                        "（连续两次 nextFloat() < 0.05D）；此处将两处统一替换为该配置值。",
                        "设为 0.0 时鸡骑士彻底不再生成，但小僵尸本身不受影响。")
                .push("zombie");
        ZOMBIE_CHICKEN_JOCKEY_CHANCE = builder.comment("僵尸鸡骑士生成概率（0.0~1.0，同时作用于骑乘已有鸡/生成新鸡两处掷骰）。")
                .defineInRange("chicken_jockey_chance", 0.05D, 0.0D, 1.0D);
        builder.pop();

        SPEC = builder.build();

        ModConfigSpec.Builder clientBuilder = new ModConfigSpec.Builder();
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

    /**
     * 某模式生效的怪物索敌视野（格）。
     * <p>
     * 判定以配置值为准：默认 16 即与原版一致（不增强），玩家改大即增强——
     * 例如把普通模式的 follow_range 从 16 改为 40，普通难度的敌对生物也会
     * 按 40 格索敌。不提供全局开关，避免破坏逐模式语义。
     */
    public static double followRange(DifficultyLevel level) {
        return FOLLOW_RANGES.get(level).get();
    }

    /**
     * 某模式的特殊事件倍率下限（0.0~1.0）。
     * <p>
     * 本模组在 {@code DifficultyInstanceMixin} 中将原版 getSpecialMultiplier() 结果与该下限取
     * max，并将等价的有效难度 2.0+2.0*floor 施加到 getEffectiveDifficulty() 的返回值上，
     * 从而保证即使区块没有被长期驻留，各类按结局有效难度的概率项（僵尸点燃/尸壳饥饿/
     * 骷髅陷阱马）也不会低于该值（GOD 默认 1.0 即概率恒满；CASUAL/PEACEFUL 默认 0.0 即无保底）。
     */
    public static double specialMultiplierFloor(DifficultyLevel level) {
        return SPECIAL_MULTIPLIER_FLOORS.get(level).get();
    }

    /** 僵尸鸡骑士生成概率（0.0~1.0，全局统一）。 */
    public static double zombieChickenJockeyChance() {
        return ZOMBIE_CHICKEN_JOCKEY_CHANCE.get();
    }

    /** 模式随进度自动演进总开关是否开启。 */
    public static boolean autoProgressionEnabled() {
        return AUTO_PROGRESSION_ENABLED.get();
    }

    /**
     * 自动演进当前是否实际生效：总开关开启且当前模式不是和平。
     * 和平模式下自动禁用演进（不会因击杀 Boss 提升难度），
     * 同时难度锁同步失效，允许玩家手动切换回其他模式。
     */
    public static boolean autoProgressionActive() {
        return autoProgressionEnabled() && getMode() != DifficultyLevel.PEACEFUL;
    }

    /** 是否采用 forge:bosses 标签判定触发演进（未激活时恒为 false）。 */
    public static boolean useForgeBossesTag() {
        return autoProgressionActive() && AUTO_PROGRESSION_USE_FORGE_BOSSES_TAG.get();
    }

    /** 触发演进的额外生物注册 ID 列表（未激活时为空列表）。 */
    public static List<? extends String> autoProgressionBosses() {
        if (!autoProgressionActive()) {
            return List.of();
        }
        return AUTO_PROGRESSION_BOSSES.get();
    }

    /** 自动演进期间是否锁定手动调整难度（未激活时恒为 false）。 */
    public static boolean autoProgressionLockDifficulty() {
        return autoProgressionActive() && AUTO_PROGRESSION_LOCK_DIFFICULTY.get();
    }

    /** 小僵尸生成概率（0.0~1.0，0.0 表示小僵尸完全无法生成）。 */
    public static double babyZombieChance() {
        return BABY_ZOMBIE_CHANCE.get();
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