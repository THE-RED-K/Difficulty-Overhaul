package dev.difficultyoverhaul;

import net.minecraft.network.chat.Component;

/**
 * 本模组提供的全部难度模式。
 * <p>
 * 倍率含义：伤害/血量/护甲相对于“普通模式”的倍率。
 * 所有模式（除和平外）在游戏内一律判定为困难模式，以兼容其他模组
 * 在困难模式下提供的特殊技能、掉落等内容。
 * <p>
 * 索敌视野含义：敌对生物（Monster）生成的索敌距离（格）。
 * 默认值与原版一致为 16（即不增强），进阶档起每提升一级 +4；
 * 实际生效值以 {@code difficulty_overhaul-common.toml} 中
 * mode_multipliers.&lt;模式&gt;.follow_range 配置项为准，玩家改大即生效。
 */
public enum DifficultyLevel {
    PEACEFUL("peaceful", 1.0D, 1.0D, 1.0D, 16.0D, 0.0D),
    CASUAL("casual", 0.5D, 1.0D, 1.0D, 16.0D, 0.0D),
    EASY("easy", 0.75D, 1.0D, 1.0D, 16.0D, 0.125D),
    NORMAL("normal", 1.0D, 1.0D, 1.0D, 16.0D, 0.25D),
    ADVANCED("advanced", 1.25D, 1.25D, 1.25D, 20.0D, 0.375D),
    HARD("hard", 1.5D, 1.5D, 1.5D, 24.0D, 0.5D),
    EXPERT("expert", 1.75D, 1.75D, 1.75D, 28.0D, 0.625D),
    MASTER("master", 2.0D, 2.0D, 2.0D, 32.0D, 0.75D),
    GRANDMASTER("grandmaster", 2.25D, 2.25D, 2.25D, 36.0D, 0.875D),
    GOD("god", 2.5D, 2.5D, 2.5D, 40.0D, 1.0D);

    private final String key;
    private final double defaultDamageMult;
    private final double defaultHealthMult;
    private final double defaultArmorMult;
    private final double defaultFollowRange;
    private final double defaultSpecialMultiplierFloor;

    DifficultyLevel(String key, double defaultDamageMult, double defaultHealthMult, double defaultArmorMult,
                    double defaultFollowRange, double defaultSpecialMultiplierFloor) {
        this.key = key;
        this.defaultDamageMult = defaultDamageMult;
        this.defaultHealthMult = defaultHealthMult;
        this.defaultArmorMult = defaultArmorMult;
        this.defaultFollowRange = defaultFollowRange;
        this.defaultSpecialMultiplierFloor = defaultSpecialMultiplierFloor;
    }

    public String getKey() {
        return key;
    }

    public double getDefaultDamageMult() {
        return defaultDamageMult;
    }

    public double getDefaultHealthMult() {
        return defaultHealthMult;
    }

    public double getDefaultArmorMult() {
        return defaultArmorMult;
    }

    public double getDefaultFollowRange() {
        return defaultFollowRange;
    }

    public double getDefaultSpecialMultiplierFloor() {
        return defaultSpecialMultiplierFloor;
    }

    public Component getDisplayName() {
        return Component.translatable("difficulty_overhaul.mode." + key);
    }

    /**
     * 原版风格的模式介绍（对应原版 Difficulty#getInfo 的 options.difficulty.<key>.info）。
     */
    public Component getInfo() {
        return Component.translatable("difficulty_overhaul.mode." + key + ".info");
    }

    public static DifficultyLevel[] buttonOrder() {
        return new DifficultyLevel[] {
                NORMAL, ADVANCED, HARD, EXPERT, MASTER, GRANDMASTER, GOD, PEACEFUL, CASUAL, EASY
        };
    }

    /**
     * 通过配置/命令中的字符串键查找模式，找不到时返回 null。
     */
    public static DifficultyLevel byKey(String key) {
        for (DifficultyLevel level : values()) {
            if (level.key.equals(key)) {
                return level;
            }
        }
        return null;
    }
}