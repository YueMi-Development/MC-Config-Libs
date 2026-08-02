package org.yuemi.config.api;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.net.URI;
import java.util.*;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Scans for {@link MigrationStep} implementations in the specified package,
 * sorts them by {@link MigrationStep#getTargetVersion()}, and applies them
 * sequentially to a plugin's configuration file.
 *
 * This implementation mirrors the robust approach used in MC-MMO-Mechanics:
 *   • Works both when the plugin is run from a directory (e.g., in the IDE)
 *   • Works when the plugin is packaged as a JAR on a server
 *   • Does not rely on any naming convention – the version is obtained from
 *     {@code getTargetVersion()}.
 */
public final class ConfigManager {
    private final Logger logger;
    private final List<MigrationStep> steps = new ArrayList<>();
    private final int latestVersion;

    /**
     * @param plugin the owning plugin instance
     * @param scanPackage the package that contains migration step classes,
     *                    e.g. "org.yuemi.example.plugin.config.migrations"
     */
    public ConfigManager(JavaPlugin plugin, String scanPackage) {
        this.logger = plugin.getLogger();
        this.steps.addAll(discoverSteps(plugin, scanPackage));
        
        Set<Integer> versions = new HashSet<>();
        for (MigrationStep step : steps) {
            int version = step.getTargetVersion();
            if (!versions.add(version)) {
                throw new IllegalStateException("Duplicate migration step detected for version " + version);
            }
        }

        int max = 1;
        for (MigrationStep step : steps) {
            if (step.getTargetVersion() > max) {
                max = step.getTargetVersion();
            }
        }
        steps.sort(Comparator.comparingInt(MigrationStep::getTargetVersion));
        this.latestVersion = max;
    }

    private List<MigrationStep> discoverSteps(JavaPlugin plugin, String scanPackage) {
        List<MigrationStep> found = new ArrayList<>();
        String path = scanPackage.replace('.', '/');
        try {
            java.net.URL resource = plugin.getClass().getClassLoader().getResource(path);
            if (resource != null && resource.getProtocol().equals("file")) {
                File pkgDir = new File(resource.toURI());
                if (pkgDir.isDirectory()) {
                    File[] files = pkgDir.listFiles();
                    if (files != null) {
                        for (File file : files) {
                            if (file.getName().endsWith(".class") && !file.getName().contains("$")) {
                                String className = scanPackage + "." + file.getName().substring(0, file.getName().length() - 6);
                                tryLoadStep(plugin, className, found);
                            }
                        }
                    }
                    return found;
                }
            }

            java.net.URL location = plugin.getClass().getProtectionDomain().getCodeSource().getLocation();
            if (location == null) {
                location = ConfigManager.class.getProtectionDomain().getCodeSource().getLocation();
            }
            if (location == null) {
                logger.warning("Code source location is null, cannot scan for migration steps");
                return found;
            }
            URI uri = location.toURI();
            File src = new File(uri);
            if (src.isDirectory()) {
                File pkgDir = new File(src, path);
                if (pkgDir.isDirectory()) {
                    File[] files = pkgDir.listFiles();
                    if (files != null) {
                        for (File file : files) {
                            if (file.getName().endsWith(".class") && !file.getName().contains("$")) {
                                String className = scanPackage + "." + file.getName().substring(0, file.getName().length() - 6);
                                tryLoadStep(plugin, className, found);
                            }
                        }
                    }
                }
            } else if (src.isFile() && src.getName().endsWith(".jar")) {
                try (ZipFile zip = new ZipFile(src)) {
                    Enumeration<? extends ZipEntry> entries = zip.entries();
                    while (entries.hasMoreElements()) {
                        ZipEntry entry = entries.nextElement();
                        String name = entry.getName();
                        if (name.startsWith(path + "/") && name.endsWith(".class") && !name.contains("$")) {
                            String className = name.substring(0, name.length() - 6).replace('/', '.');
                            tryLoadStep(plugin, className, found);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warning("Failed to auto-discover config migration steps: " + e.getMessage());
        }
        return found;
    }

    private void tryLoadStep(JavaPlugin plugin, String className, List<MigrationStep> list) {
        try {
            Class<?> clazz = Class.forName(className, true, plugin.getClass().getClassLoader());
            if (MigrationStep.class.isAssignableFrom(clazz) && !clazz.isInterface() && !java.lang.reflect.Modifier.isAbstract(clazz.getModifiers())) {
                MigrationStep step = (MigrationStep) clazz.getDeclaredConstructor().newInstance();
                list.add(step);
                logger.fine("Discovered config migration step: " + clazz.getSimpleName() + " (Target version: " + step.getTargetVersion() + ")");
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * Loads the plugin configuration (creates default if missing), applies any missing
     * migration steps, and saves the file if changes were made.
     *
     * @param plugin the plugin instance to operate on
     */
    public void loadAndMigrate(JavaPlugin plugin) {
        try {
            plugin.saveDefaultConfig();
        } catch (Exception e) {
            logger.warning("Failed to save default config: " + e.getMessage());
        }
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        
        int current;
        boolean modified = false;
        if (!config.contains("config-version") || config.get("config-version") == null) {
            current = 0;
            config.set("config-version", 0);
            modified = true;
        } else {
            current = config.getInt("config-version");
        }

        if (current >= latestVersion) {
            if (modified) {
                try {
                    config.save(configFile);
                } catch (Exception e) {
                    logger.severe("Failed to save migrated configuration: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            return;
        }
        for (MigrationStep step : steps) {
            if (current == step.getTargetVersion() - 1) {
                int old = current;
                logger.fine("Applying config migration step: " + step.getClass().getSimpleName() + " (Target version: " + step.getTargetVersion() + ")");
                step.migrate(config);
                current = step.getTargetVersion();
                config.set("config-version", current);
                logger.info("Migrated " + configFile.getName() + " from version " + old + " to " + current);
                modified = true;
            }
        }
        if (modified) {
            try {
                config.save(configFile);
            } catch (Exception e) {
                logger.severe("Failed to save migrated configuration: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
