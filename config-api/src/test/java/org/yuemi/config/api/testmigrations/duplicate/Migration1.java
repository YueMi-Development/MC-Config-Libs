package org.yuemi.config.api.testmigrations.duplicate;

import org.bukkit.configuration.file.FileConfiguration;
import org.yuemi.config.api.MigrationStep;

public class Migration1 implements MigrationStep {
    @Override
    public int getTargetVersion() {
        return 2;
    }

    @Override
    public void migrate(FileConfiguration config) {
    }
}
