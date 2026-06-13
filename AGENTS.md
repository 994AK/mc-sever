# Repository Guidelines

## Project Structure & Module Organization

This repository is a Leaf/Paper 1.21.11 server workspace plus local plugin sources. Root-level files such as `server.properties`, `bukkit.yml`, `spigot.yml`, `plugins/`, and `world*/` belong to the runnable server. Local Gradle modules live under `src/<PluginName>` and are registered in `settings.gradle.kts`: `LeafGomoku`, `LeafFriends`, `LeafResidenceWeb`, `LeafRecycle`, `LeafSoulbind`, and `LeafChainHarvest`. Use `src/<PluginName>/src/main/java` or `src/main/kotlin` for code, `src/main/resources` for `plugin.yml` and default config, and matching `src/test/java` or `src/test/kotlin` for tests. Operational notes, plans, and research belong in `docs/`; packaged handoff artifacts belong in `copy/` or `deploy/`.

## Build, Test, and Development Commands

- `./gradlew check` runs compilation plus all configured tests.
- `./gradlew buildPlugins` builds all local plugin jars with Shadow.
- `./gradlew :LeafGomoku:shadowJar` builds one plugin jar; replace the module name as needed.
- `./gradlew :LeafChainHarvest:installPlugin` builds and copies one jar into root `plugins/`.
- `./gradlew installPlugins` copies all built local jars into `plugins/`.

Do not run install tasks while the server is running; stop the server before replacing plugin jars. Do not start the local server, open localhost, or run browser/mobile checks unless the task explicitly asks for runtime or visual verification.

## Coding Style & Naming Conventions

Use Java 21 and Kotlin JVM 2.4.0 via the Gradle wrapper. Keep package names under `net.leafmc.<plugin>`. Use 4-space indentation, descriptive class names, and existing Bukkit/Paper naming patterns such as `*Plugin`, `*Command`, `*Listener`, `*Service`, and `*Test`. Keep user-facing command text and permissions synchronized with each plugin's `plugin.yml` and default config.

## Testing Guidelines

Tests are lightweight Java/Kotlin classes named `*Test`. Several modules register `mainClassTests` in their `build.gradle.kts`; `./gradlew check` runs those along with Gradle test tasks. Add tests beside the module being changed, especially for command formatting, rule logic, config parsing, and persistence. Prefer deterministic unit tests over live-server checks unless behavior requires a real Paper runtime.

## Commit & Pull Request Guidelines

Recent history uses concise messages, often Conventional Commit style such as `feat: ...`, `chore: ...`, and `build: ...`; prefer that format for new commits. Pull requests should describe the changed plugin or server config, list verification commands, call out any runtime smoke tests, and include screenshots only for GUI/menu changes.

## Security & Configuration Tips

Avoid committing secrets, player data dumps, or unnecessary world/log churn. When changing permissions, update LuckPerms YAML and the relevant README or operations note together.
