package org.yuemi.config.api;

import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a single configuration migration step.
 * Implementations declare the version they migrate TO via {@link #getTargetVersion()}.
 */
public interface MigrationStep {
    /**
     * The configuration version this step migrates TO.
     * For example, a step that converts version 1 to version 2 should return 2.
     */
    int getTargetVersion();

    /**
     * Apply the migration to the provided configuration.
     * The implementation may add, modify, or remove keys as needed.
     *
     * @param config the configuration instance that is currently loaded
     */
    void migrate(@NotNull FileConfiguration config);
}
