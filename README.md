# Forge 1.20.1 Mod 开发模板（MDK）

基于 `New-Armor-System` 项目整理的可复用模组开发模板，开箱即用，避免每次从零配置 `build.gradle` 等构建文件。

## 功能特性

- **Forge 1.20.1** + ForgeGradle 6.0.54，Java 17
- **Parchment 映射**（2023.09.03-1.20.1）
- **MixinGradle 0.7.38** 一键配置 Mixin（refmap / reobf / MANIFEST 自动处理），附空 mixin 配置模板
- `gradle.properties` 集中管理版本号与模组元数据，`processResources` 自动注入 `mods.toml` / `pack.mcmeta`
- 内置软依赖（如 Curios）的 build.gradle 与 mods.toml 写法示例（默认注释）
- 附离线开发优化代码（本地有 assets 时可跳过资源下载，默认注释）

## 使用方法

### 1. 复制为新项目

将本 `mdk` 目录整个复制为一个新目录（或拷贝其中所有文件到新项目目录）。

### 2. 修改 `gradle.properties`

新建模组时通常只需修改此文件的 **模组信息段**：

| 属性 | 说明 | 示例 |
| --- | --- | --- |
| `mod_id` | 模组 id，全小写，须匹配 `[a-z][a-z0-9_]{1,63}` | `my_mod` |
| `mod_name` | 显示名称 | `My Mod` |
| `mod_version` | 模组版本 | `1.0.0-1.20.1` |
| `mod_group_id` | Maven group / 包名基路径 | `com.example.mymod` |
| `mod_authors` | 作者 | `Your Name` |
| `mod_description` | 描述 | `...` |

> `minecraft_version` / `forge_version` / `mapping_*` 等环境版本如需更换请同步修改。

### 3. 调整代码与资源

- **主类**：将 `src/main/java/com/example/examplemod/ExampleMod.java` 移动/改包到 `mod_group_id` 对应路径，并把 `MOD_ID` 常量改为 `mod_id`。
- **包结构**：把 `com.example.examplemod` 全局替换为你的包名。
- **Mixin**：重命名 `src/main/resources/example_mod.mixins.json` 为 `<mod_id>.mixins.json`，修改其中 `package` 字段；同时更新 `build.gradle` 的 `mixin { add ...; config ... }` 块与 refmap 文件名（二者保持同名）。
- **软依赖**：如需 Curios 等，取消 `build.gradle` 与 `mods.toml` 中对应注释。

### 4. 首次构建 / 生成 IDE 工程

```bash
# IntelliJ IDEA
./gradlew genIntellijRuns --refresh-dependencies

# Eclipse
./gradlew genEclipseRuns --refresh-dependencies

# 或直接运行客户端（首次会自动下载资源）
./gradlew runClient
```

> 国内网络可参考 `gradle/wrapper/gradle-wrapper.properties` 使用镜像源（已配置腾讯云镜像）。
> 若本地已有 `.minecraft` 资产目录，可在 `build.gradle` 的 `configureEach` 中设置
> `environment 'assets_root', file('你的路径/.minecraft/assets').absolutePath`
> 并取消注释文件末尾的离线优化代码，跳过资源重复下载。

### 5. 打包

```bash
./gradlew build
```

产物位于 `build/libs/`（`reobf` 后的可发布 jar）。

## 目录结构

```
mdk/
├── build.gradle              # 构建脚本（通用，一般无需修改）
├── gradle.properties         # ★ 版本号与模组元数据（新建模组主要改这里）
├── settings.gradle           # 插件仓库
├── gradlew / gradlew.bat     # Gradle Wrapper
├── gradle/wrapper/           # Wrapper 配置与 jar
├── .gitignore
├── README.md
└── src/
    ├── main/
    │   ├── java/com/example/examplemod/ExampleMod.java
    │   └── resources/
    │       ├── META-INF/mods.toml          # 模组元数据（${} 自动注入）
    │       ├── pack.mcmeta
    │       └── example_mod.mixins.json     # Mixin 配置（可重命名）
    └── generated/resources/  # 数据生成器输出目录
```

## 注意事项

- 首次构建需联网下载 Forge / Parchment / Mixin 依赖，耗时较长，请耐心等待。
- 修改 `mod_id` 后，若无法编译，请检查：主类 `MOD_ID` 常量、`mods.toml` 中的 `${mod_id}`（自动注入，无需手改）、mixins.json 文件名与 `package` 字段。
- 本模板基于 Forge 47.4.20 / 1.20.1，切换 Minecraft 大版本时请参考官方 MDK 更新插件版本。
