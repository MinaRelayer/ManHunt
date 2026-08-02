/*
 * Decompiled with CFR 0.152.
 */
package me.mina.manhunt.game;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import me.mina.manhunt.game.PlayerSessionTracker;
import me.mina.manhunt.game.RespawnIntent;
import me.mina.manhunt.team.TeamType;
import org.bukkit.Location;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PlayerSessionTrackerTest {
    private final Set<UUID> online = new HashSet<UUID>();
    private PlayerSessionTracker tracker;
    private final UUID hunter = UUID.randomUUID();
    private final UUID runnerA = UUID.randomUUID();
    private final UUID runnerB = UUID.randomUUID();
    private final UUID spectator = UUID.randomUUID();
    private final UUID outsider = UUID.randomUUID();

    PlayerSessionTrackerTest() {
    }

    @BeforeEach
    void setUp() {
        this.tracker = new PlayerSessionTracker(this.online::contains);
        this.tracker.setTeam(this.hunter, TeamType.HUNTER);
        this.tracker.setTeam(this.runnerA, TeamType.RUNNER);
        this.tracker.setTeam(this.runnerB, TeamType.RUNNER);
        this.tracker.setTeam(this.spectator, TeamType.SPECTATOR);
        this.online.add(this.hunter);
        this.online.add(this.runnerA);
    }

    @Test
    void returnsTeamPerPlayerAndNoneForUnknown() {
        Assertions.assertEquals((Object)TeamType.HUNTER, (Object)this.tracker.getTeam(this.hunter));
        Assertions.assertEquals((Object)TeamType.RUNNER, (Object)this.tracker.getTeam(this.runnerA));
        Assertions.assertEquals((Object)TeamType.NONE, (Object)this.tracker.getTeam(this.outsider));
    }

    @Test
    void settingTeamNoneRemovesSession() {
        this.tracker.setTeam(this.runnerA, TeamType.NONE);
        Assertions.assertEquals((Object)TeamType.NONE, (Object)this.tracker.getTeam(this.runnerA));
        Assertions.assertFalse((boolean)this.tracker.hasSession(this.runnerA));
    }

    @Test
    void markDeadTogglesAlive() {
        Assertions.assertTrue((boolean)this.tracker.isAlive(this.runnerA));
        this.tracker.markDead(this.runnerA);
        Assertions.assertFalse((boolean)this.tracker.isAlive(this.runnerA));
    }

    @Test
    void aliveRunnersIncludeDisconnectedPlayersInsideGrace() {
        this.tracker.disconnect(this.runnerB, 1000L);
        Assertions.assertEquals((int)2, (int)this.tracker.getAliveRunnerIds().size());
        Assertions.assertTrue((boolean)this.tracker.getAliveRunnerIds().contains(this.runnerB));
    }

    @Test
    void disconnectedRunnersOnlyReportsRunnersInsideGrace() {
        this.tracker.disconnect(this.runnerB, 1000L);
        this.tracker.disconnect(this.hunter, 1000L);
        List disconnected = this.tracker.getDisconnectedRunnerIds();
        Assertions.assertEquals((int)1, (int)disconnected.size());
        Assertions.assertEquals((Object)this.runnerB, disconnected.get(0));
    }

    @Test
    void sweepGraceTimesOutOnlyAfterGracePeriod() {
        this.tracker.disconnect(this.runnerB, 1000L);
        List first = this.tracker.sweepGrace(1999L, 1000L);
        Assertions.assertTrue((boolean)first.isEmpty());
        Assertions.assertTrue((boolean)this.tracker.isAlive(this.runnerB));
        List second = this.tracker.sweepGrace(2000L, 1000L);
        Assertions.assertEquals(List.of(this.runnerB), (Object)second);
        Assertions.assertFalse((boolean)this.tracker.isAlive(this.runnerB));
        Assertions.assertFalse((boolean)this.tracker.isDisconnected(this.runnerB));
    }

    @Test
    void sweepGraceWithDisabledGraceDoesNothing() {
        this.tracker.disconnect(this.runnerB, 1000L);
        Assertions.assertTrue((boolean)this.tracker.sweepGrace(10000L, 0L).isEmpty());
        Assertions.assertTrue((boolean)this.tracker.isAlive(this.runnerB));
    }

    @Test
    void reconnectClearsDisconnectTime() {
        this.tracker.disconnect(this.runnerB, 1000L);
        Assertions.assertTrue((boolean)this.tracker.isDisconnected(this.runnerB));
        this.tracker.reconnect(this.runnerB);
        Assertions.assertFalse((boolean)this.tracker.isDisconnected(this.runnerB));
        Assertions.assertTrue((boolean)this.tracker.isAlive(this.runnerB));
    }

    @Test
    void onlineMemberIdsUsesOnlineCheck() {
        Assertions.assertEquals(List.of(this.runnerA), (Object)this.tracker.getOnlineMemberIds(TeamType.RUNNER));
        Assertions.assertEquals(List.of(this.hunter), (Object)this.tracker.getOnlineMemberIds(TeamType.HUNTER));
        Assertions.assertTrue((boolean)this.tracker.getOnlineMemberIds(TeamType.SPECTATOR).isEmpty());
    }

    @Test
    void originalLocationIsStoredAndCleared() {
        Location location = new Location(null, 10.0, 64.0, -20.0);
        this.tracker.setOriginalLocation(this.runnerA, location);
        Assertions.assertEquals((Object)location, (Object)this.tracker.getOriginalLocation(this.runnerA));
        Assertions.assertNull((Object)this.tracker.getOriginalLocation(this.outsider));
        this.tracker.clearOriginalLocations();
        Assertions.assertNull((Object)this.tracker.getOriginalLocation(this.runnerA));
    }

    @Test
    void respawnIntentIsSetAndClearedOnRead() {
        this.tracker.setRespawnIntent(this.runnerA, RespawnIntent.SPECTATOR);
        Assertions.assertEquals((Object)RespawnIntent.SPECTATOR, (Object)this.tracker.getAndClearRespawnIntent(this.runnerA));
        Assertions.assertEquals((Object)RespawnIntent.NONE, (Object)this.tracker.getAndClearRespawnIntent(this.runnerA));
        Assertions.assertEquals((Object)RespawnIntent.NONE, (Object)this.tracker.getAndClearRespawnIntent(this.outsider));
    }

    @Test
    void clearResetsAllState() {
        this.tracker.disconnect(this.runnerB, 1000L);
        this.tracker.markDead(this.runnerA);
        this.tracker.clear();
        Assertions.assertEquals((Object)TeamType.NONE, (Object)this.tracker.getTeam(this.hunter));
        Assertions.assertTrue((boolean)this.tracker.getAliveRunnerIds().isEmpty());
        Assertions.assertTrue((boolean)this.tracker.getMemberIds(TeamType.HUNTER).isEmpty());
    }
}
