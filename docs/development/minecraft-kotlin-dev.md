# Minecraft Kotlin Development

本仓库现在把 Minecraft 插件开发环境统一到 Gradle Kotlin DSL 和 Kotlin/JVM。运行服目录仍是服务器根目录，插件源码集中在 `src/<PluginName>`，构建产物默认在各模块的 `build/libs/`，只有执行 `installPlugin`/`installPlugins` 才会复制到根目录 `plugins/`。

## 当前基线

- 服务端：Leaf `1.21.11 build 158`
- API：Paper API `1.21.11-R0.1-SNAPSHOT`
- Java toolchain：`21`
- Kotlin JVM：`2.4.0`
- Gradle Wrapper：`9.5.1`
- 打包：Shadow `9.4.2`

这些值集中在根目录 `gradle.properties` 和根 `build.gradle.kts`。升级服务端小版本时优先改 `mcVersion`、`paperApiVersion`、`javaLanguageVersion`，再跑一次 `./gradlew check buildPlugins`。

## 日常命令

```bash
./gradlew check
./gradlew buildPlugins
./gradlew :LeafGomoku:installPlugin
./gradlew :LeafFriends:installPlugin
./gradlew installPlugins
```

兼容旧习惯的脚本也保留：

```bash
scripts/test-plugins.sh
scripts/build-plugins.sh
scripts/test-leaf-gomoku.sh
scripts/build-leaf-gomoku.sh
scripts/test-leaf-friends.sh
scripts/build-leaf-friends.sh
```

`installPlugin` 会覆盖 `plugins/<PluginName>-<version>.jar`。服务器运行中不要执行安装任务；先停服、备份，再复制 jar。

## 最新方案取舍

普通 Bukkit/Paper 插件默认走 API-only：

```kotlin
dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
}
```

只有需要 NMS、Minecraft internals、服务端内部类或 Mojang mappings 时，才给对应模块启用 `io.papermc.paperweight.userdev`，并使用 `paperweight.paperDevBundle(...)`。Paper 文档明确只官方支持最新 paperweight-userdev，因此遇到 userdev 问题先升级插件版本。

Kotlin 插件不要假设服务器自带 Kotlin runtime。本仓库默认用 Shadow 把 Kotlin stdlib 打进 jar；Paper API、PlaceholderAPI、WorldEdit、Residence、CMILib 这类由服务器提供的依赖用 `compileOnly`，不打包进插件。

## 新插件结构

新插件放在：

```text
src/NewPlugin/
  build.gradle.kts
  src/main/kotlin/net/leafmc/newplugin/NewPlugin.kt
  src/main/resources/plugin.yml
  src/test/kotlin/...
```

然后在根 `settings.gradle.kts` 加：

```kotlin
include("NewPlugin")
project(":NewPlugin").projectDir = file("src/NewPlugin")
```

入口类继续继承 `JavaPlugin`，但源码用 Kotlin：

```kotlin
package net.leafmc.newplugin

import org.bukkit.plugin.java.JavaPlugin

class NewPlugin : JavaPlugin() {
    override fun onEnable() {
        logger.info("NewPlugin enabled")
    }
}
```

`plugin.yml` 仍是稳定方案，现有 Leaf/Paper 1.21.11 插件继续使用：

```yaml
name: NewPlugin
version: 0.1.0
main: net.leafmc.newplugin.NewPlugin
api-version: '1.21'
```

Paper 新的 `paper-plugin.yml`、loader/bootstrapper 仍带实验属性；本服当前没有需要它们的场景，先不作为默认模板。

## 参考源

- Paper 项目设置：https://docs.papermc.io/paper/dev/project-setup/
- Paper plugin.yml：https://docs.papermc.io/paper/dev/plugin-yml/
- Paper paperweight-userdev：https://docs.papermc.io/paper/dev/userdev/
- Paper 版本 API：https://api.papermc.io/v2/projects/paper
- Gradle 当前版本 API：https://services.gradle.org/versions/current
- Kotlin Maven 元数据：https://repo.maven.apache.org/maven2/org/jetbrains/kotlin/jvm/org.jetbrains.kotlin.jvm.gradle.plugin/maven-metadata.xml
