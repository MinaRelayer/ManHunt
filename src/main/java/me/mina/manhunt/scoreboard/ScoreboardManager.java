/*
 * Decompiled with CFR 0.152.
 */
package me.mina.manhunt.scoreboard;

import java.util.Collection;
import org.bukkit.entity.Player;

public interface ScoreboardManager {
    public void start();

    public void stop();

    public void applyToAll();

    default public void start(long startTime, Collection<Player> participants) {
        this.start();
    }

    default public void applyToPlayer(Player player) {
        this.applyToAll();
    }
}
