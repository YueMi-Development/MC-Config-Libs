# MC-Config-Libs

[![Build](https://github.com/YueMi-Development/MC-Config-Libs/actions/workflows/build.yml/badge.svg)](https://github.com/YueMi-Development/MC-Config-Libs/actions/workflows/build.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

A reusable configuration migration library for [PaperMC](https://papermc.io/) plugins.  
Drop it in as a dependency and get automatic, versioned `config.yml` migrations, no boilerplate required.

---

## Features

- **Auto-discovery**, scans a package at startup and loads every `MigrationStep` implementation automatically, whether running from a directory (IDE) or a shaded JAR on a server
- **Sequential migration**, steps are sorted by `getTargetVersion()` and applied in order, skipping versions already applied
- **Saves only when needed**, `config.yml` is written to disk only if at least one step was applied
- **Zero naming conventions**, migration classes can be named anything; the target version comes from `getTargetVersion()`
- **Paper API native**, built against Paper API 1.21.6, fully compatible with modern Minecraft

---

## Installation

Add the YueMi Maven repository and the dependency to your plugin's `build.gradle.kts`:

```kotlin
repositories {
    maven { url = uri("https://repo.yuemi.my.id/repository/maven-releases/") }
}

dependencies {
    implementation("org.yuemi:mc-config-libs:1.0.0")
}
```

> **Shade it in.** Because the library is not bundled with the server, you must shade it into your plugin JAR using the [Shadow Gradle plugin](https://gradleup.com/shadow/).

---

## Usage

### 1. Add `config-version` to your default `config.yml`

```yaml
# config.yml (in src/main/resources)
config-version: 1

some-setting: true
```

### 2. Create migration steps

Each class in your migrations package implements `MigrationStep`:

```java
package org.yuemi.myplugin.config.migrations;

import org.bukkit.configuration.file.FileConfiguration;
import org.yuemi.config.api.MigrationStep;

/**
 * Migrates config from version 1 → 2.
 * Renames "old-key" to "new-key".
 */
public class MigrationV1ToV2 implements MigrationStep {

    @Override
    public int getTargetVersion() {
        return 2;
    }

    @Override
    public void migrate(FileConfiguration config) {
        String value = config.getString("old-key");
        config.set("new-key", value);
        config.set("old-key", null);
    }
}
```

Rules:
- One class per migration step
- `getTargetVersion()` must return the version this step migrates **to**
- No-arg constructor required (auto-instantiated at runtime)
- Inner classes and abstract classes are automatically ignored

### 3. Wire up `ConfigManager` in your plugin

```java
import org.yuemi.config.api.ConfigManager;

public class MyPlugin extends JavaPlugin {

    private ConfigManager configManager;

    @Override
    public void onEnable() {
        configManager = new ConfigManager(this, "org.yuemi.myplugin.config.migrations");
        configManager.loadAndMigrate(this);
    }
}
```

`loadAndMigrate` will:
1. Call `saveDefaultConfig()` to create `config.yml` if it is missing
2. Read the current `config-version` from the file (defaults to `1`)
3. Apply every pending migration step in ascending version order
4. Write the updated file to disk if any step ran

---

## API

### `ConfigManager`

| Method | Description |
|--------|-------------|
| `ConfigManager(JavaPlugin plugin, String scanPackage)` | Discovers all `MigrationStep` implementations in the given package |
| `void loadAndMigrate(JavaPlugin plugin)` | Runs all pending migrations and saves `config.yml` |

### `MigrationStep`

| Method | Description |
|--------|-------------|
| `int getTargetVersion()` | The config version this step migrates **to** |
| `void migrate(FileConfiguration config)` | Apply changes to the loaded configuration |

---

## How migration versioning works

```
config-version: 1  →  MigrationV1ToV2 runs  →  config-version: 2
config-version: 2  →  MigrationV2ToV3 runs  →  config-version: 3
config-version: 3  →  (up to date, nothing runs)
```

Each step is applied only when `currentVersion == targetVersion - 1`, ensuring no step is skipped or applied twice.

---

## Building from source

```bash
./gradlew :config-api:build
```

Running unit tests:

```bash
./gradlew :config-api:test
```
