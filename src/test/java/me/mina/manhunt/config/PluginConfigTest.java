/*
 * Decompiled with CFR 0.152.
 */
package me.mina.manhunt.config;

import me.mina.manhunt.config.PluginConfig;
import org.bukkit.Difficulty;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PluginConfigTest {
    PluginConfigTest() {
    }

    @Test
    void usesDefaultsForEmptyConfig() {
        PluginConfig config = new PluginConfig();
        config.load((FileConfiguration)new YamlConfiguration());
        Assertions.assertEquals((Object)"en_us", (Object)config.getLanguage());
        Assertions.assertEquals((long)20L, (long)config.getScoreboardRefreshTicks());
        Assertions.assertEquals((Object)"manhunt", (Object)config.getWorldNamespace());
        Assertions.assertTrue((boolean)config.isRandomSeed());
        Assertions.assertEquals((Object)Difficulty.HARD, (Object)config.getWorldDifficulty());
        Assertions.assertEquals((int)300, (int)config.getDisconnectGraceSeconds());
    }

    @Test
    void parsesConfiguredValues() {
        YamlConfiguration yml = new YamlConfiguration();
        yml.set("language", (Object)"zh_cn");
        yml.set("scoreboard.refresh-ticks", (Object)10L);
        yml.set("world.namespace", (Object)"arena_1");
        yml.set("world.seed", (Object)42L);
        yml.set("world.difficulty", (Object)"NORMAL");
        yml.set("game.disconnect-grace-seconds", (Object)60);
        PluginConfig config = new PluginConfig();
        config.load((FileConfiguration)yml);
        Assertions.assertEquals((Object)"zh_cn", (Object)config.getLanguage());
        Assertions.assertEquals((long)10L, (long)config.getScoreboardRefreshTicks());
        Assertions.assertEquals((Object)"arena_1", (Object)config.getWorldNamespace());
        Assertions.assertFalse((boolean)config.isRandomSeed());
        Assertions.assertEquals((long)42L, (long)config.getWorldSeed());
        Assertions.assertEquals((Object)Difficulty.NORMAL, (Object)config.getWorldDifficulty());
        Assertions.assertEquals((int)60, (int)config.getDisconnectGraceSeconds());
    }

    @Test
    void invalidDifficultyFallsBackToHard() {
        YamlConfiguration yml = new YamlConfiguration();
        yml.set("world.difficulty", (Object)"banana");
        PluginConfig config = new PluginConfig();
        config.load((FileConfiguration)yml);
        Assertions.assertEquals((Object)Difficulty.HARD, (Object)config.getWorldDifficulty());
    }

    @Test
    void negativeGraceSecondsClampedToZero() {
        YamlConfiguration yml = new YamlConfiguration();
        yml.set("game.disconnect-grace-seconds", -5);
        PluginConfig config = new PluginConfig();
        config.load((FileConfiguration)yml);
        Assertions.assertEquals((int)0, (int)config.getDisconnectGraceSeconds());
    }

    @Test
    void invalidWorldNamesFallBackToManhunt() {
        for (String value : new String[]{"Bad Name!", ".", "..", "../outside"}) {
            YamlConfiguration yml = new YamlConfiguration();
            yml.set("world.namespace", (Object)value);
            PluginConfig config = new PluginConfig();
            config.load((FileConfiguration)yml);
            Assertions.assertEquals((Object)"manhunt", (Object)config.getWorldNamespace(), (String)value);
        }
    }

    @Test
    void zeroRefreshTicksClampedToDefault() {
        YamlConfiguration yml = new YamlConfiguration();
        yml.set("scoreboard.refresh-ticks", (Object)0L);
        PluginConfig config = new PluginConfig();
        config.load((FileConfiguration)yml);
        Assertions.assertEquals((long)20L, (long)config.getScoreboardRefreshTicks());
    }
}
