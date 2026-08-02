/*
 * Decompiled with CFR 0.152.
 */
package me.mina.manhunt.game;

import java.util.List;
import java.util.UUID;
import me.mina.manhunt.game.ServerBridge;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public class BukkitServerBridge
implements ServerBridge {
    private final Plugin plugin;

    public BukkitServerBridge(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public BukkitTask runTaskLater(Runnable task, long delayTicks) {
        return Bukkit.getScheduler().runTaskLater(this.plugin, task, delayTicks);
    }

    @Override
    public BukkitTask runTaskTimer(Runnable task, long delayTicks, long periodTicks) {
        return Bukkit.getScheduler().runTaskTimer(this.plugin, task, delayTicks, periodTicks);
    }

    @Override
    public List<Player> getOnlinePlayers() {
        return List.copyOf(Bukkit.getOnlinePlayers());
    }

    @Override
    public String getPlayerName(UUID playerId) {
        return Bukkit.getOfflinePlayer((UUID)playerId).getName();
    }
}
