/*
 * Decompiled with CFR 0.152.
 */
package me.mina.manhunt.team;

import java.util.List;
import java.util.UUID;
import me.mina.manhunt.game.PlayerSessionTracker;
import me.mina.manhunt.team.TeamType;
import org.bukkit.entity.Player;

public interface TeamManager {
    public void joinTeam(Player var1, TeamType var2);

    public void leaveTeam(Player var1);

    public void clearAll();

    public TeamType getTeam(Player var1);

    public List<Player> getOnlineMembers(TeamType var1);

    public void markDead(Player var1);

    public boolean isAlive(Player var1);

    public boolean isDisconnected(Player var1);

    public void disconnect(Player var1);

    public void reconnect(Player var1);

    public List<UUID> sweepGrace(long var1, long var3);

    public PlayerSessionTracker tracker();
}
