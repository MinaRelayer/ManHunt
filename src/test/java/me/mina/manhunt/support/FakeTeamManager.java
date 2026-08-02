/*
 * Decompiled with CFR 0.152.
 */
package me.mina.manhunt.support;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import me.mina.manhunt.game.PlayerSessionTracker;
import me.mina.manhunt.team.TeamManager;
import me.mina.manhunt.team.TeamType;
import org.bukkit.entity.Player;

public class FakeTeamManager
implements TeamManager {
    public final Set<UUID> online = new HashSet<UUID>();
    private final Map<UUID, Player> players = new HashMap<UUID, Player>();
    public final PlayerSessionTracker tracker = new PlayerSessionTracker(this.online::contains);

    public void addOnline(Player player) {
        this.online.add(player.getUniqueId());
        this.players.put(player.getUniqueId(), player);
    }

    public void removeOnline(Player player) {
        this.online.remove(player.getUniqueId());
    }

    public void joinTeam(Player player, TeamType type) {
        this.players.put(player.getUniqueId(), player);
        this.tracker.setTeam(player.getUniqueId(), type);
    }

    public void leaveTeam(Player player) {
        this.joinTeam(player, TeamType.SPECTATOR);
    }

    public void clearAll() {
        this.tracker.clear();
    }

    public TeamType getTeam(Player player) {
        return this.tracker.getTeam(player.getUniqueId());
    }

    public List<Player> getOnlineMembers(TeamType type) {
        ArrayList<Player> members = new ArrayList<Player>();
        for (UUID id : this.tracker.getOnlineMemberIds(type)) {
            Player player = this.players.get(id);
            if (player == null) continue;
            members.add(player);
        }
        return members;
    }

    public void markDead(Player player) {
        this.tracker.markDead(player.getUniqueId());
    }

    public boolean isAlive(Player player) {
        return this.tracker.isAlive(player.getUniqueId());
    }

    public boolean isDisconnected(Player player) {
        return this.tracker.isDisconnected(player.getUniqueId());
    }

    public void disconnect(Player player) {
        this.tracker.disconnect(player.getUniqueId(), System.currentTimeMillis());
    }

    public void reconnect(Player player) {
        this.tracker.reconnect(player.getUniqueId());
    }

    public List<UUID> sweepGrace(long nowMillis, long graceMillis) {
        return this.tracker.sweepGrace(nowMillis, graceMillis);
    }

    public PlayerSessionTracker tracker() {
        return this.tracker;
    }
}
