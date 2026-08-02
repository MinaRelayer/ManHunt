/*
 * Decompiled with CFR 0.152.
 */
package me.mina.manhunt.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.Difficulty;
import org.bukkit.configuration.file.FileConfiguration;

public class PluginConfig {
    private static final String DEFAULT_LANGUAGE = "en_us";
    private static final String DEFAULT_NAMESPACE = "manhunt";
    private String language = "en_us";
    private long scoreboardRefreshTicks = 20L;
    private String worldNamespace = "manhunt";
    private boolean randomSeed = true;
    private long worldSeed;
    private Difficulty worldDifficulty = Difficulty.HARD;
    private int disconnectGraceSeconds = 300;
    private final List<String> warnings = new ArrayList<String>();

    public void load(FileConfiguration cfg) {
        this.warnings.clear();
        this.language = PluginConfig.readNonBlank(cfg.getString("language"), DEFAULT_LANGUAGE);
        this.scoreboardRefreshTicks = cfg.getLong("scoreboard.refresh-ticks", 20L);
        if (this.scoreboardRefreshTicks < 1L) {
            this.warnings.add("scoreboard.refresh-ticks must be at least 1; using 20");
            this.scoreboardRefreshTicks = 20L;
        } else if (this.scoreboardRefreshTicks > 72000L) {
            this.warnings.add("scoreboard.refresh-ticks is too large; using 20");
            this.scoreboardRefreshTicks = 20L;
        }
        this.worldNamespace = cfg.getString("world.namespace", DEFAULT_NAMESPACE);
        if (!PluginConfig.isValidNamespace(this.worldNamespace)) {
            this.warnings.add("world.namespace is invalid; using 'manhunt'");
            this.worldNamespace = DEFAULT_NAMESPACE;
        }
        this.parseSeed(cfg);
        String difficultyValue = cfg.getString("world.difficulty", Difficulty.HARD.name());
        try {
            this.worldDifficulty = Difficulty.valueOf((String)difficultyValue.toUpperCase(Locale.ROOT));
        }
        catch (Exception ex) {
            this.warnings.add("world.difficulty is invalid; using HARD");
            this.worldDifficulty = Difficulty.HARD;
        }
        this.disconnectGraceSeconds = cfg.getInt("game.disconnect-grace-seconds", 300);
        if (this.disconnectGraceSeconds < 0) {
            this.warnings.add("game.disconnect-grace-seconds cannot be negative; using 0");
            this.disconnectGraceSeconds = 0;
        }
    }

    private void parseSeed(FileConfiguration cfg) {
        Object raw = cfg.get("world.seed", (Object)"random");
        if (raw == null || "random".equalsIgnoreCase(String.valueOf(raw).trim())) {
            this.randomSeed = true;
            this.worldSeed = 0L;
            return;
        }
        try {
            long l;
            if (raw instanceof Number) {
                Number number = (Number)raw;
                l = number.longValue();
            } else {
                l = Long.parseLong(String.valueOf(raw).trim());
            }
            this.worldSeed = l;
            this.randomSeed = false;
        }
        catch (NumberFormatException ex) {
            this.warnings.add("world.seed must be 'random' or an integer; using random");
            this.randomSeed = true;
            this.worldSeed = 0L;
        }
    }

    private static String readNonBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    public static boolean isValidNamespace(String value) {
        return value != null && !value.isBlank() && !value.equals(".") && !value.equals("..") && value.length() <= 32 && value.matches("[a-z0-9_\\-.]+");
    }

    public String getLanguage() {
        return this.language;
    }

    public long getScoreboardRefreshTicks() {
        return this.scoreboardRefreshTicks;
    }

    public String getWorldNamespace() {
        return this.worldNamespace;
    }

    public boolean isRandomSeed() {
        return this.randomSeed;
    }

    public long getWorldSeed() {
        return this.worldSeed;
    }

    public Difficulty getWorldDifficulty() {
        return this.worldDifficulty;
    }

    public int getDisconnectGraceSeconds() {
        return this.disconnectGraceSeconds;
    }

    public List<String> getWarnings() {
        return List.copyOf(this.warnings);
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public void setDisconnectGraceSeconds(int seconds) {
        this.disconnectGraceSeconds = Math.max(0, seconds);
    }

    public void setWorldSeed(long seed) {
        this.worldSeed = seed;
        this.randomSeed = false;
    }
}
