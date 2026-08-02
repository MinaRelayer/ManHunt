/*
 * Decompiled with CFR 0.152.
 */
package me.mina.manhunt.team;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import me.mina.manhunt.game.PlayerSessionTracker;
import me.mina.manhunt.team.TeamManager;
import me.mina.manhunt.team.TeamType;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

public class BukkitTeamManager
implements TeamManager {
    private final PlayerSessionTracker tracker = new PlayerSessionTracker(uuid -> Bukkit.getPlayer((UUID)uuid) != null);

    @Override
    public void joinTeam(Player player, TeamType type) {
        if (type == null || type == TeamType.NONE) {
            this.leaveTeam(player);
            return;
        }
        this.tracker.setTeam(player.getUniqueId(), type);
        player.setGameMode(type == TeamType.SPECTATOR ? GameMode.SPECTATOR : GameMode.SURVIVAL);
    }

    @Override
    public void leaveTeam(Player player) {
        this.joinTeam(player, TeamType.SPECTATOR);
    }

    @Override
    public void clearAll() {
        this.tracker.clear();
    }

    @Override
    public TeamType getTeam(Player player) {
        return this.tracker.getTeam(player.getUniqueId());
    }

    @Override
    public List<Player> getOnlineMembers(TeamType type) {
        ArrayList<Player> members = new ArrayList<Player>();
        for (UUID id : this.tracker.getOnlineMemberIds(type)) {
            Player player = Bukkit.getPlayer((UUID)id);
            if (player == null || !player.isOnline()) continue;
            members.add(player);
        }
        return members;
    }

    @Override
    public void markDead(Player player) {
        this.tracker.markDead(player.getUniqueId());
    }

    @Override
    public boolean isAlive(Player player) {
        return this.tracker.isAlive(player.getUniqueId());
    }

    @Override
    public boolean isDisconnected(Player player) {
        return this.tracker.isDisconnected(player.getUniqueId());
    }

    @Override
    public void disconnect(Player player) {
        this.tracker.disconnect(player.getUniqueId(), System.currentTimeMillis());
    }

    @Override
    public void reconnect(Player player) {
        this.tracker.reconnect(player.getUniqueId());
    }

    @Override
    public List<UUID> sweepGrace(long nowMillis, long graceMillis) {
        return this.tracker.sweepGrace(nowMillis, graceMillis);
    }

    @Override
    public PlayerSessionTracker tracker() {
        return this.tracker;
    }
}
