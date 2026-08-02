/*
 * Decompiled with CFR 0.152.
 */
package me.mina.manhunt;

import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import java.io.File;
import me.mina.manhunt.command.ManhuntCommand;
import me.mina.manhunt.command.PlayerResolver;
import me.mina.manhunt.config.PluginConfig;
import me.mina.manhunt.game.BukkitServerBridge;
import me.mina.manhunt.game.GameManager;
import me.mina.manhunt.game.HunterCompassService;
import me.mina.manhunt.game.ManhuntGameManager;
import me.mina.manhunt.lang.LangManager;
import me.mina.manhunt.lang.YamlLangManager;
import me.mina.manhunt.listener.ManhuntListener;
import me.mina.manhunt.placeholder.ManhuntPlaceholderExpansion;
import me.mina.manhunt.scoreboard.BukkitScoreboardManager;
import me.mina.manhunt.scoreboard.ScoreboardManager;
import me.mina.manhunt.team.BukkitTeamManager;
import me.mina.manhunt.team.TeamManager;
import me.mina.manhunt.world.BukkitWorldManager;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class ManhuntPlugin
extends JavaPlugin {
    private PluginConfig config;
    private LangManager langManager;
    private TeamManager teamManager;
    private BukkitWorldManager worldManager;
    private ScoreboardManager scoreboardManager;
    private GameManager gameManager;
    private HunterCompassService compassService;

    public void onEnable() {
        this.saveDefaultConfig();
        this.saveResourceIfMissing("lang/en_us.yml");
        this.saveResourceIfMissing("lang/zh_cn.yml");
        this.config = new PluginConfig();
        this.config.load(this.getConfig());
        for (String warning : this.config.getWarnings()) {
            this.getLogger().warning(warning);
        }
        this.langManager = new YamlLangManager(this.getDataFolder(), this.config, this.getLogger()::warning, this.getLogger()::info);
        this.langManager.load();
        this.teamManager = new BukkitTeamManager();
        this.worldManager = new BukkitWorldManager((Plugin)this, this.config);
        this.worldManager.cleanupOrphanedWorlds();
        this.scoreboardManager = new BukkitScoreboardManager((Plugin)this, this.teamManager, this.langManager, this.config);
        BukkitServerBridge bridge = new BukkitServerBridge((Plugin)this);
        this.gameManager = new ManhuntGameManager(bridge, this.teamManager, this.worldManager, this.scoreboardManager, this.langManager, this.config);
        this.compassService = new HunterCompassService((Plugin)this, this.gameManager, this.teamManager, this.worldManager, bridge);
        this.compassService.start();
        PlayerResolver playerResolver = Bukkit::getPlayerExact;
        ManhuntCommand command = new ManhuntCommand(this.gameManager, this.teamManager, this.langManager, playerResolver, this::reloadAll);
        command.register((LifecycleEventManager<Plugin>)this.getLifecycleManager());
        this.getServer().getPluginManager().registerEvents((Listener)new ManhuntListener((Plugin)this, this.gameManager, this.teamManager, this.worldManager, this.compassService), (Plugin)this);
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new ManhuntPlaceholderExpansion((Plugin)this, this.gameManager, this.teamManager).register();
            this.getLogger().info("PlaceholderAPI expansion registered.");
        } else {
            this.getLogger().warning("PlaceholderAPI not detected; placeholders are unavailable.");
        }
        this.getLogger().info("ManHunt plugin enabled.");
    }

    public void onDisable() {
        if (this.compassService != null) {
            this.compassService.stop();
        }
        if (this.gameManager != null) {
            this.getLogger().info("Performing shutdown cleanup (worlds, player state)...");
            this.gameManager.shutdown();
        }
        this.getLogger().info("ManHunt plugin disabled.");
    }

    public void reloadAll() {
        this.restoreMissingResources();
        this.reloadConfig();
        this.config.load(this.getConfig());
        for (String warning : this.config.getWarnings()) {
            this.getLogger().warning(warning);
        }
        this.langManager.reload();
    }

    private void saveResourceIfMissing(String resourcePath) {
        File target = new File(this.getDataFolder(), resourcePath);
        if (!target.exists()) {
            this.saveResource(resourcePath, false);
        }
    }

    public void restoreMissingResources() {
        if (!new File(this.getDataFolder(), "config.yml").exists()) {
            this.saveDefaultConfig();
        }
        this.saveResourceIfMissing("lang/en_us.yml");
        this.saveResourceIfMissing("lang/zh_cn.yml");
    }
}
