package dev.difficultyoverhaul;

import net.minecraft.network.chat.Component;

/**
 * 本模组提供的全部难度模式。
 * <p>
 * 倍率含义：伤害/血量/护甲相对于“普通模式”的倍率。
 * 所有模式（除和平外）在游戏内一律判定为困难模式，以兼容其他模组
 * 在困难模式下提供的特殊技能、掉落等内容。
 */
public enum DifficultyLevel {
    PEACEFUL("peaceful", 1.0D, 1.0D, 1.0D),
    CASUAL("casual", 0.5D, 1.0D, 1.0D),
    EASY("easy", 0.75D, 1.0D, 1.0D),
    NORMAL("normal", 1.0D, 1.0D, 1.0D),
    ADVANCED("advanced", 1.25D, 1.25D, 1.25D),
    HARD("hard", 1.5D, 1.5D, 1.5D),
    EXPERT("expert", 1.75D, 1.75D, 1.75D),
    MASTER("master", 2.0D, 2.0D, 2.0D),
    GRANDMASTER("grandmaster", 2.25D, 2.25D, 2.25D),
    GOD("god", 2.5D, 2.5D, 2.5D);

    private final String key;
    private final double defaultDamageMult;
    private final double defaultHealthMult;
    private final double defaultArmorMult;

    DifficultyLevel(String key, double defaultDamageMult, double defaultHealthMult, double defaultArmorMult) {
        this.key = key;
        this.defaultDamageMult = defaultDamageMult;
        this.defaultHealthMult = defaultHealthMult;
        this.defaultArmorMult = defaultArmorMult;
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
