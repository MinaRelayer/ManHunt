/*
 * Decompiled with CFR 0.152.
 */
package me.mina.manhunt.game;

import java.util.UUID;
import me.mina.manhunt.game.RespawnIntent;
import me.mina.manhunt.team.TeamType;
import org.bukkit.Location;

public class PlayerSession {
    private final UUID playerId;
    private TeamType team = TeamType.NONE;
    private boolean alive = true;
    private Location originalLocation;
    private RespawnIntent respawnIntent = RespawnIntent.NONE;
    private long disconnectedAt = 0L;

    public PlayerSession(UUID playerId) {
        this.playerId = playerId;
    }

    public UUID getPlayerId() {
        return this.playerId;
    }

    public TeamType getTeam() {
        return this.team;
    }

    public void setTeam(TeamType team) {
        this.team = team;
    }

    public boolean isAlive() {
        return this.alive;
    }

    public void setAlive(boolean alive) {
        this.alive = alive;
    }

    public Location getOriginalLocation() {
        return this.originalLocation;
    }

    public void setOriginalLocation(Location originalLocation) {
        this.originalLocation = originalLocation;
    }

    public RespawnIntent getRespawnIntent() {
        return this.respawnIntent;
    }

    public void setRespawnIntent(RespawnIntent respawnIntent) {
        this.respawnIntent = respawnIntent == null ? RespawnIntent.NONE : respawnIntent;
    }

    public long getDisconnectedAt() {
        return this.disconnectedAt;
    }

    public void setDisconnectedAt(long disconnectedAt) {
        this.disconnectedAt = disconnectedAt;
    }

    public void resetForTeam(TeamType newTeam) {
        this.team = newTeam;
        this.alive = true;
        this.originalLocation = null;
        this.respawnIntent = RespawnIntent.NONE;
        this.disconnectedAt = 0L;
    }
}
