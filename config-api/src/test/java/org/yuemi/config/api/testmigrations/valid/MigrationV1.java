package org.yuemi.config.api.testmigrations.valid;

import org.bukkit.configuration.file.FileConfiguration;
import org.yuemi.config.api.MigrationStep;

public class MigrationV1 implements MigrationStep {
    @Override
    public int getTargetVersion() {
        return 1;
    }

    @Override
    public void migrate(FileConfiguration config) {
        config.set("migrated-key", "migrated-value");
    }
}
