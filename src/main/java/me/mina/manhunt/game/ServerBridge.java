/*
 * Decompiled with CFR 0.152.
 */
package me.mina.manhunt.game;

import java.util.List;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

public interface ServerBridge {
    public BukkitTask runTaskLater(Runnable var1, long var2);

    public BukkitTask runTaskTimer(Runnable var1, long var2, long var4);

    public List<Player> getOnlinePlayers();

    public String getPlayerName(UUID var1);
}
