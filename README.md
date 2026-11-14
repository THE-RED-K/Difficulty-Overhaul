# Difficulty Overhaul

一个面向 **Minecraft Forge 1.20.1**（Java 17）的难度系统扩展模组，用一套可精细配置的多档模式取代原版只有 4 档（和平 / 简单 / 普通 / 困难）的难度体系，并修复原版难度缩放的若干缺陷。

> mod_id：`difficulty_overhaul` ｜ 许可证：MIT ｜ 构建：ForgeGradle 6.0.54 + MixinGradle 0.7.38 + Parchment 映射

## 功能特性

- **10 种可配置难度模式**：在普通、困难之间补充了休闲、进阶、专家、大师、宗师、神明等档位，每档的伤害 / 血量 / 护甲倍率均可在配置文件中单独调整。
- **所有非和平模式一律判定为困难**：游戏内世界难度与 `DifficultyInstance` 都被强制为困难，使其他模组在困难模式下提供的特殊技能、装备掉落、刷怪等内容全部生效。
- **修复原版难度缩放缺陷**：原版 `DamageSource#scalesWithDifficulty()` 只检测直接实体是否为生物，导致怪物发射的弹射物（箭、火球、凋灵头颅、女巫药水等）伤害不随难度缩放。本模组补充判定“伤害造成者（施法者 / 发射者）为非玩家生物”时也放大，并在玩家受伤入口统一应用倍率。
- **伤害判定还原为普通、避免双重计算**：玩家受伤时，世界难度读取被重定为普通，使伤害完全由“原始伤害 × 当前模式倍率”单一来源决定（普通模式即与原版普通难度一致）。
- **覆盖式难度按钮**：替换原版选项界面与创建世界界面中的难度按钮，提供覆盖全部模式的循环按钮；极限模式（Hardcore）下仅暴露困难及以上的模式。
- **重写 `/difficulty` 命令**：支持全部模式名称（含原版 4 个名称），并带补全提示（需权限 2）。
- **服务端配置自动同步 + 客户端记忆**：服务端记录当前模式并自动下发客户端；客户端记住创建世界 / 选项界面选择的模式，作为新建世界的初始模式。

## 难度模式

| 模式 | 键（key） | 默认伤害倍率 | 默认血量倍率 | 默认护甲倍率 |
| --- | --- | --- | --- | --- |
| 和平 | `peaceful` | 1.0 | 1.0 | 1.0（不生成敌对生物） |
| 休闲 | `casual` | 0.5 | 1.0 | 1.0 |
| 简单 | `easy` | 0.75 | 1.0 | 1.0 |
| 普通 | `normal` | 1.0 | 1.0 | 1.0 |
| 进阶 | `advanced` | 1.25 | 1.25 | 1.25 |
| 困难 | `hard` | 1.5 | 1.5 | 1.5 |
| 专家 | `expert` | 1.75 | 1.75 | 1.75 |
| 大师 | `master` | 2.0 | 2.0 | 2.0 |
| 宗师 | `grandmaster` | 2.25 | 2.25 | 2.25 |
| 神明 | `god` | 2.5 | 2.5 | 2.5 |

各模式的悬停介绍（原版风格）已内置中英文（`zh_cn` / `en_us`）。

## 缩放原理

- **伤害倍率**：在 `Player#hurt` 入口处、以 `invokespecial LivingEntity.hurt` 为注入点放大伤害。判定按原版 `scalesWithDifficulty()` 为真（非玩家生物造成的近战 / 魔法 / 荆棘反伤，以及爆炸、声波等 `ALWAYS` 类）再加上“造成者为非玩家生物”的弹射物修复。天然排除玩家间伤害、玩家射出的弹射物、以及摔落 / 溺水 / 窒息 / 虚空 / 饥饿等无造成者的环境伤害；TNT 自爆（无造成者）按 `ALWAYS` 保留原版放大。
- **血量与护甲倍率**：在生物 `finalizeSpawn` 完成后，直接乘以当前模式的倍率（血量随后恢复满血）。和平模式跳过，避免影响中立生物。

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

配置文件位于 `config/difficulty_overhaul-server.toml`（服务端，自动同步到客户端）与 `config/difficulty_overhaul-client.toml`（客户端记忆）。

- `mode`：当前生效的模式（字符串键，如 `hard`）。
- `mode_multipliers.<模式>.{damage,health,armor}_multiplier`：各模式的默认倍率，可覆盖上表默认值。
- `global.{damage,health,armor}_enabled`：开启后无视当前模式，统一使用下方全局倍率。
- `global.{damage,health,armor}_multiplier`：全局倍率（范围 0.0–100.0），仅在对应开关开启时生效。

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

- `DifficultyOverhaul`：主类，注册服务端 / 客户端配置与服务器事件。
- `DifficultyLevel`：10 种模式的枚举，含键、默认倍率、显示名与介绍。
- `config/DifficultyOverhaulConfig`：Forge 服务端 / 客户端配置（自动同步），含各模式倍率与全局开关。
- `server/DifficultyOverhaulServer`：模式切换、世界难度强制（`setDifficulty(..., true)` 绕过锁定）、`/difficulty` 命令注册与新建世界初始模式处理。
- `client/DifficultyOverhaulClient`：构建覆盖全部模式的循环按钮。
- `mixin/`：
  - `DifficultyInstanceMixin`：将难度实例基础难度强制为困难（和平除外）。
  - `PlayerHurtMixin`：玩家受伤入口——难度重定为普通 + 怪物伤害倍率（含弹射物修复）。
  - `MobMixin`：生物生成后应用血量 / 护甲倍率。
  - `DifficultyCommandMixin`：重写 `/difficulty` 命令注册。
  - `OptionsScreenMixin` / `CreateWorldScreenMixin` / `CreateWorldScreenGameTabMixin`：替换原版难度按钮；后者通过 `GridLayout*` 访问器在创建世界网格布局中精确定位并替换难度按钮。
  - `GridLayout*Accessor`：访问 `GridLayout` 私有字段的 Mixin 访问器。

所有 Mixin 声明于 `src/main/resources/difficulty_overhaul.mixins.json`（服务端 4 个、客户端 6 个，`defaultRequire=1`）。
