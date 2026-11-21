# Difficulty Overhaul

一个面向 **Minecraft Forge 1.20.1**（Java 17）的难度系统扩展模组，用一套可精细配置的多档模式取代原版只有 4 档（和平 / 简单 / 普通 / 困难）的难度体系，并修复原版难度缩放的若干缺陷。

> 另有同一仓库的 [1.21.1 NeoForge 分支](https://github.com/THE-RED-K/Difficulty-Overhaul/tree/1.21.1-neoforge)：`Difficulty-Overhaul-1.21.1-NeoForge`，功能完全对齐，唯一差异见文末。
> mod_id：`difficulty_overhaul` ｜ 许可证：MIT ｜ 构建：ForgeGradle 6.0.54 + MixinGradle 0.7.38 + Parchment 映射

## 功能特性

- **10 种可配置难度模式**：在普通、困难之间补充了休闲、进阶、专家、大师、宗师、神明等档位，每档的伤害 / 血量 / 护甲倍率、怪物索敌视野、特殊事件倍率下限均可在配置文件中单独调整。
- **所有非和平模式一律判定为困难**：游戏内世界难度与 `DifficultyInstance` 都被强制为困难，使其他模组在困难模式下提供的特殊技能、装备掉落、刷怪等内容全部生效。
- **修复原版难度缩放缺陷**：原版 `DamageSource#scalesWithDifficulty()` 只检测直接实体是否为生物，导致怪物发射的弹射物（箭、火球、凋灵头颅、女巫药水等）伤害不随难度缩放。本模组补充判定“伤害造成者（施法者 / 发射者）为非玩家生物”时也放大，并在玩家受伤入口统一应用倍率。
- **伤害判定还原为普通、避免双重计算**：玩家受伤时，世界难度读取被重定为普通，使伤害完全由“原始伤害 × 当前模式倍率”单一来源决定（普通模式即与原版普通难度一致）。
- **特殊事件倍率下限（保底）**：按原版 `DifficultyInstance#getSpecialMultiplier()` 缩放的概率值（生物拾取战利品、僵尸点燃、骷髅陷阱马生成等）最终取值不低于按模式配置的下限——神明档恒满，休闲 / 和平档不做保底（随区块驻留时间自然浮动）。
- **有效难度直算项保底**：对 `getEffectiveDifficulty()` 返回值做统一的线性保底（`max(原值, 2.0 + 2.0×下限)`），使直接读取有效难度的 3 处逻辑——僵尸点燃（概率 / 秒数）、尸壳饥饿时长、骷髅陷阱马触发——与特殊事件倍率保持完全一致的下限语义，无需逐项修改。
- **僵尸概率配置化**：小僵尸概率（Forge 内置 `zombieBabyChance`，模组直接沿用）与僵尸鸡骑士概率（`zombie.chicken_jockey_chance`，替换原版 `finalizeSpawn` 中两处独立的 5% 掷骰）均可配置，可单独关闭。
- **覆盖式难度按钮**：替换原版选项界面与创建世界界面中的难度按钮，提供覆盖全部模式的循环按钮；极限模式（Hardcore）下仅暴露困难及以上的模式。
- **创建世界游戏规则预设**：新建 / 导入旧世界时自动为若干游戏规则设定默认值（保留背包、生物破坏、无差别愤怒等），进世界前即可在“游戏规则”界面查看并修改。
- **模式随进度自动演进**：可选地在击杀 Boss 后自动提升模式档位（和平自动禁用、神明封顶、难度锁定可选）。
- **重写 `/difficulty` 命令**：支持全部模式名称（含原版 4 个名称），并带补全提示（需权限 2）。
- **客户端记忆**：客户端记住创建世界 / 选项界面选择的模式，作为新建世界的初始模式。

## 难度模式

| 模式 | 键（key） | 默认伤害倍率 | 默认血量倍率 | 默认护甲倍率 | 默认索敌视野 | 默认特殊倍率下限 |
| --- | --- | --- | --- | --- | --- | --- |
| 和平 | `peaceful` | 1.0 | 1.0 | 1.0（不生成敌对生物） | 16 | 0.0 |
| 休闲 | `casual` | 0.5 | 1.0 | 1.0 | 16 | 0.0 |
| 简单 | `easy` | 0.75 | 1.0 | 1.0 | 16 | 0.125 |
| 普通 | `normal` | 1.0 | 1.0 | 1.0 | 16 | 0.25 |
| 进阶 | `advanced` | 1.25 | 1.25 | 1.25 | 20 | 0.375 |
| 困难 | `hard` | 1.5 | 1.5 | 1.5 | 24 | 0.5 |
| 专家 | `expert` | 1.75 | 1.75 | 1.75 | 28 | 0.625 |
| 大师 | `master` | 2.0 | 2.0 | 2.0 | 32 | 0.75 |
| 宗师 | `grandmaster` | 2.25 | 2.25 | 2.25 | 36 | 0.875 |
| 神明 | `god` | 2.5 | 2.5 | 2.5 | 40 | 1.0 |

各模式的悬停介绍（原版风格）已内置中英文（`zh_cn` / `en_us`）。

## 缩放原理

- **伤害倍率**：在 `Player#hurt` 入口处、以 `invokespecial LivingEntity.hurt` 为注入点放大伤害。判定按原版 `scalesWithDifficulty()` 为真（非玩家生物造成的近战 / 魔法 / 荆棘反伤，以及爆炸、声波等 `ALWAYS` 类）再加上“造成者为非玩家生物”的弹射物修复。天然排除玩家间伤害、玩家射出的弹射物、以及摔落 / 溺水 / 窒息 / 虚空 / 饥饿等无造成者的环境伤害；TNT 自爆（无造成者）按 `ALWAYS` 保留原版放大。
- **血量与护甲倍率**：在生物 `finalizeSpawn` 完成后，直接乘以当前模式的倍率（血量随后恢复满血）。和平模式跳过，避免影响中立生物。
- **怪物索敌视野**：仅敌对生物（Monster）按当前模式的 `follow_range` 配置上调索敌距离（默认 16 即原版不增强），并刷新其 `NearestAttackableTargetGoal` 目标判定。只升不降，尊重其他模组设置的更高索敌值。
- **特殊事件倍率下限**：原版 `getSpecialMultiplier()` 在有效难度 `eff ∈ [2.0, 4.0]` 时线性映射到 `[0.0, 1.0]`。本模组在注入点对所有返回值取“原版值”与“按模式配置的下限”的较大者——神明档下限 1.0，所有概率项恒满。
- **有效难度直算项保底**：对 `getEffectiveDifficulty()` 返回值做 `max(原值, 2.0 + 2.0×下限)` 的线性映射。因为特殊倍率公式恰好把 `eff ∈ [2, 4]` 映射到 `[0, 1]`，这一保底把全部直接读取有效难度的逻辑（僵尸点燃 `eff*0.3` / `(int)eff*2` 秒、尸壳饥饿 `(int)eff*140` 条、骷髅陷阱马 `eff*0.01`）统一对齐到与特殊倍率一致的下限语义。

## 安装与构建

本仓库已包含完整构建配置，克隆后直接构建即可：

```bash
# 生成 IDE 运行配置（IntelliJ IDEA）
./gradlew genIntellijRuns --refresh-dependencies

# 打包可发布 jar（reobf 后位于 build/libs/）
./gradlew build
```

将 `build/libs/difficulty_overhaul-1.0.0-1.20.1.jar` 放入整合包 / 客户端的 `mods/` 目录即可。

> 国内网络：构建依赖已配置腾讯云镜像（`gradle/wrapper/gradle-wrapper.properties`）。首次构建需联网下载 Forge / Parchment / Mixin 依赖。

## 配置

配置文件位于 `config/difficulty_overhaul-common.toml`（通用，服务器权威，便于整合包作者直接编辑）与 `config/difficulty_overhaul-client.toml`（客户端记忆 / 创建世界预设）。

- `mode`：当前生效的模式（字符串键，如 `hard`）。
- `mode_multipliers.<模式>.{damage,health,armor}_multiplier`：各模式的默认倍率，可覆盖上表默认值。
- `mode_multipliers.<模式>.follow_range`：各模式的怪物索敌视野（格），默认 16 即原版不增强，改大即生效。
- `mode_multipliers.<模式>.special_multiplier_floor`：各模式特殊事件倍率下限（0.0–1.0），所有按 `getSpecialMultiplier()` / `getEffectiveDifficulty()` 缩放的概率值最终取值不低于该下限；神明档设为 1.0 即恒满。
- `global.{damage,health,armor}_enabled`：开启后无视当前模式，统一使用下方全局倍率。
- `global.{damage,health,armor}_multiplier`：全局倍率（范围 0.0–100.0），仅在对应开关开启时生效。
- `zombie.chicken_jockey_chance`：僵尸鸡骑士生成概率（0.0–1.0，默认 0.05），同时作用于原版 `finalizeSpawn` 中“骑乘已有鸡”与“生成新鸡”两处掷骰；设为 0.0 时鸡骑士彻底不再生成。
- `auto_progression.enabled`：模式随进度自动演进总开关（默认关）。
- `auto_progression.use_forge_bosses_tag`：击杀拥有 `forge:bosses` 标签的生物是否触发演进（默认开）。
- `auto_progression.boss_list`：额外触发演进的生物注册 ID 列表（默认空）。
- `auto_progression.lock_difficulty`：演进期间锁定难度，玩家无法手动调整（默认开）。
- 小僵尸概率（`zombieBabyChance`）：由 Forge 内置配置提供，模组不重复定义。

客户端配置文件 `config/difficulty_overhaul-client.toml`：

- `mode`：客户端记住的模式（创建世界 / 选项界面的初始值，单机新建世界时作为初始模式）。
- `world_creation_defaults.enabled`：创建世界游戏规则预设总开关（默认开）。
- `world_creation_defaults.{keep_inventory,mob_griefing,universal_anger,forgive_dead_players,keep_wallet}`：新建世界时对应游戏规则的默认值（默认保留背包开、生物破坏关等；`keep_wallet` 仅在可选前置 Lightman's Currency 装载时生效）。

## 命令

```
/difficulty            # 查询当前模式
/difficulty <模式键>   # 切换到指定模式（如 /difficulty god）
```

支持补全，可用键即上表的 10 个 `key`。需要权限等级 2（默认仅管理员 / 局域网 OP）。

## 切换方式

- **单机 / 局域网**：选项界面（Esc → 选项）与创建世界界面（“游戏”标签页）中的难度按钮可循环切换全部模式；创建世界时选择的模式会作为新世界的初始模式。
- **多人联机**：玩家通过 `/difficulty` 命令（需权限 2）由服务器统一切换。

## 技术实现

包结构（源码 `src/main/java/dev/difficultyoverhaul/`）：

- `DifficultyOverhaul`：主类，注册通用 / 客户端配置与服务器事件。
- `DifficultyLevel`：10 种模式的枚举，含键、默认倍率（伤害 / 血量 / 护甲 / 索敌 / 特殊倍率下限）、显示名与介绍。
- `config/DifficultyOverhaulConfig`：Forge 通用 / 客户端配置，含各模式倍率、索敌视野、特殊倍率下限、全局开关、僵尸概率与自动演进。
- `server/DifficultyOverhaulServer`：模式切换、世界难度强制（`setDifficulty(..., true)` 绕过锁定）、`/difficulty` 命令注册、新建世界初始模式处理与模式随进度自动演进。
- `client/DifficultyOverhaulClient`：构建覆盖全部模式的循环按钮。
- `FollowRangeHelper`：刷新生物 `NearestAttackableTargetGoal` 的索敌判定（非 Mixin，供 `MobMixin` 调用）。
- `mixin/`：
  - `DifficultyInstanceMixin`：将难度实例基础难度强制为困难（和平除外）；重写 `calculateDifficulty` 移除原版两处拍脑袋分支；`getSpecialMultiplier()` 特殊倍率下限注入；`getEffectiveDifficulty()` 有效难度线性保底注入（覆盖僵尸点燃 / 尸壳饥饿 / 骷髅陷阱马全部直算项）。
  - `PlayerHurtMixin`：玩家受伤入口——难度重定为普通 + 怪物伤害倍率（含弹射物修复）。
  - `MobMixin`：生物生成后应用血量 / 护甲倍率与怪物索敌视野。
  - `NearestAttackableTargetGoalMixin`：索敌判定访问器（配合 `MobMixin` 刷新）。
  - `ZombieMixin`：僵尸鸡骑士生成概率配置化（`@ModifyConstant` 替换 `finalizeSpawn` 中两处 `0.05D`）。
  - `DifficultyCommandMixin`：重写 `/difficulty` 命令注册。
  - `OptionsScreenMixin` / `CreateWorldScreenMixin` / `CreateWorldScreenGameTabMixin`：替换原版难度按钮；后者通过 `GridLayout*` 访问器在创建世界网格布局中精确定位并替换难度按钮。
  - `GridLayout*Accessor`：访问 `GridLayout` 私有字段的 Mixin 访问器。
  - `WorldCreationDefaultsMixin`：创建 / 导入世界时应用 `world_creation_defaults` 游戏规则预设（`keep_wallet` 通过反射按需访问可选前置，避免织入期 NPE）。

所有 Mixin 声明于 `src/main/resources/difficulty_overhaul.mixins.json`（公共 6 个、客户端 7 个，`defaultRequire=1`）。

## 与 1.21.1 分支的差异

1.21.1 分支（NeoForge）与 1.20.1 功能完全对齐，仅以下差异：

- 加载器 / 语言版本：NeoForge 21.x / Java 21（ModDevGradle），1.20.1 为 Forge 47.x / Java 17（ForgeGradle）。
- 小僵尸概率：1.21.1 无 Forge 内置 `zombieBabyChance` 配置，改由 `ZombieMixin` 注入 `getSpawnAsBabyOdds`，读取本模组新增的 `baby_zombie_chance` 配置项（默认 0.05）。