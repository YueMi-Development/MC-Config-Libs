package org.yuemi.config.api;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class ConfigManagerTest {

    private ServerMock server;
    private JavaPlugin plugin;

    @BeforeEach
    public void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.createMockPlugin();
    }

    @AfterEach
    public void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    public void testLoadAndMigrateDoesNotThrow() {
        ConfigManager manager = new ConfigManager(plugin, "org.yuemi.config.api.migrations");
        assertDoesNotThrow(() -> manager.loadAndMigrate(plugin));
    }
}
