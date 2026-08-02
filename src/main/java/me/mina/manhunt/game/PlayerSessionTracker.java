/*
 * Decompiled with CFR 0.152.
 */
package me.mina.manhunt.game;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import me.mina.manhunt.game.PlayerSession;
import me.mina.manhunt.game.RespawnIntent;
import me.mina.manhunt.team.TeamType;
import org.bukkit.Location;

public class PlayerSessionTracker {
    private final Map<UUID, PlayerSession> sessions = new HashMap<UUID, PlayerSession>();
    private final Predicate<UUID> onlineCheck;

    public PlayerSessionTracker(Predicate<UUID> onlineCheck) {
        this.onlineCheck = onlineCheck;
    }

    public void setTeam(UUID playerId, TeamType type) {
        if (type == null || type == TeamType.NONE) {
            this.sessions.remove(playerId);
            return;
        }
        PlayerSession session = this.sessions.computeIfAbsent(playerId, PlayerSession::new);
        if (session.getTeam() != type) {
            session.resetForTeam(type);
        } else {
            session.setTeam(type);
        }
    }

    public TeamType getTeam(UUID playerId) {
        PlayerSession session = this.sessions.get(playerId);
        return session == null ? TeamType.NONE : session.getTeam();
    }

    public PlayerSession getSession(UUID playerId) {
        return this.sessions.get(playerId);
    }

    public boolean hasSession(UUID playerId) {
        return this.sessions.containsKey(playerId);
    }

    public List<UUID> getMemberIds(TeamType type) {
        ArrayList<UUID> ids = new ArrayList<UUID>();
        for (PlayerSession session : this.sessions.values()) {
            if (session.getTeam() != type) continue;
            ids.add(session.getPlayerId());
        }
        return ids;
    }

    public List<UUID> getOnlineMemberIds(TeamType type) {
        ArrayList<UUID> ids = new ArrayList<UUID>();
        for (UUID id : this.getMemberIds(type)) {
            if (!this.onlineCheck.test(id)) continue;
            ids.add(id);
        }
        return ids;
    }

    public List<UUID> getAliveRunnerIds() {
        ArrayList<UUID> ids = new ArrayList<UUID>();
        for (PlayerSession session : this.sessions.values()) {
            if (session.getTeam() != TeamType.RUNNER || !session.isAlive()) continue;
            ids.add(session.getPlayerId());
        }
        return ids;
    }

    public List<UUID> getDisconnectedRunnerIds() {
        ArrayList<UUID> ids = new ArrayList<UUID>();
        for (PlayerSession session : this.sessions.values()) {
            if (session.getTeam() != TeamType.RUNNER || !session.isAlive() || session.getDisconnectedAt() == 0L) continue;
            ids.add(session.getPlayerId());
        }
        return ids;
    }

    public boolean isAlive(UUID playerId) {
        PlayerSession session = this.sessions.get(playerId);
        return session != null && session.isAlive();
    }

    public boolean isDisconnected(UUID playerId) {
        PlayerSession session = this.sessions.get(playerId);
        return session != null && session.getDisconnectedAt() != 0L;
    }

    public void disconnect(UUID playerId, long nowMillis) {
        PlayerSession session = this.sessions.get(playerId);
        if (session != null && session.getTeam() != TeamType.NONE) {
            session.setDisconnectedAt(nowMillis);
        }
    }

    public void reconnect(UUID playerId) {
        PlayerSession session = this.sessions.get(playerId);
        if (session != null) {
            session.setDisconnectedAt(0L);
        }
    }

    public void markDead(UUID playerId) {
        PlayerSession session = this.sessions.get(playerId);
        if (session != null) {
            session.setAlive(false);
            session.setDisconnectedAt(0L);
        }
    }

    public List<UUID> sweepGrace(long nowMillis, long graceMillis) {
        ArrayList<UUID> timedOut = new ArrayList<UUID>();
        if (graceMillis <= 0L) {
            return timedOut;
        }
        for (PlayerSession session : this.sessions.values()) {
            if (session.getTeam() != TeamType.RUNNER || !session.isAlive() || session.getDisconnectedAt() == 0L || nowMillis - session.getDisconnectedAt() < graceMillis) continue;
            session.setAlive(false);
            session.setDisconnectedAt(0L);
            timedOut.add(session.getPlayerId());
        }
        return timedOut;
    }

    public void setOriginalLocation(UUID playerId, Location location) {
        this.sessions.computeIfAbsent(playerId, PlayerSession::new).setOriginalLocation(location);
    }

    public Location getOriginalLocation(UUID playerId) {
        PlayerSession session = this.sessions.get(playerId);
        return session == null ? null : session.getOriginalLocation();
    }

    public void clearOriginalLocations() {
        for (PlayerSession session : this.sessions.values()) {
            session.setOriginalLocation(null);
        }
    }

    public void setRespawnIntent(UUID playerId, RespawnIntent intent) {
        PlayerSession session = this.sessions.get(playerId);
        if (session != null) {
            session.setRespawnIntent(intent);
        }
    }

    public RespawnIntent getAndClearRespawnIntent(UUID playerId) {
        PlayerSession session = this.sessions.get(playerId);
        if (session == null) {
            return RespawnIntent.NONE;
        }
        RespawnIntent intent = session.getRespawnIntent();
        session.setRespawnIntent(RespawnIntent.NONE);
        return intent;
    }

    public void clear() {
        this.sessions.clear();
    }
}
