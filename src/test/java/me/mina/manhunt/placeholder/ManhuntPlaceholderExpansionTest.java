/*
 * Decompiled with CFR 0.152.
 */
package me.mina.manhunt.placeholder;

import io.papermc.paper.plugin.configuration.PluginMeta;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import me.mina.manhunt.game.GameManager;
import me.mina.manhunt.game.GameState;
import me.mina.manhunt.game.PlayerSessionTracker;
import me.mina.manhunt.placeholder.ManhuntPlaceholderExpansion;
import me.mina.manhunt.team.TeamManager;
import me.mina.manhunt.team.TeamType;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ManhuntPlaceholderExpansionTest {
    private final Set<UUID> online = new HashSet<UUID>();
    private PlayerSessionTracker tracker;
    private TeamManager team;
    private GameManager game;
    private ManhuntPlaceholderExpansion expansion;
    private final UUID runnerAlive = UUID.randomUUID();
    private final UUID runnerDisconnected = UUID.randomUUID();
    private final UUID runnerDead = UUID.randomUUID();
    private final UUID hunter = UUID.randomUUID();
    private final UUID outsider = UUID.randomUUID();

    ManhuntPlaceholderExpansionTest() {
    }

    @BeforeEach
    void setUp() {
        Plugin plugin = (Plugin)Mockito.mock(Plugin.class);
        PluginMeta meta = (PluginMeta)Mockito.mock(PluginMeta.class);
        Mockito.when((Object)plugin.getPluginMeta()).thenReturn((Object)meta);
        Mockito.when((Object)meta.getVersion()).thenReturn((Object)"1.0.0");
        this.tracker = new PlayerSessionTracker(this.online::contains);
        this.tracker.setTeam(this.runnerAlive, TeamType.RUNNER);
        this.tracker.setTeam(this.runnerDisconnected, TeamType.RUNNER);
        this.tracker.setTeam(this.runnerDead, TeamType.RUNNER);
        this.tracker.setTeam(this.hunter, TeamType.HUNTER);
        this.tracker.disconnect(this.runnerDisconnected, 1000L);
        this.tracker.markDead(this.runnerDead);
        this.online.add(this.runnerAlive);
        this.online.add(this.hunter);
        this.team = (TeamManager)Mockito.mock(TeamManager.class);
        Mockito.when((Object)this.team.tracker()).thenReturn((Object)this.tracker);
        Mockito.when((Object)this.team.getOnlineMembers(TeamType.HUNTER)).thenReturn(List.of((Player)Mockito.mock(Player.class)));
        Mockito.when((Object)this.team.getOnlineMembers(TeamType.RUNNER)).thenReturn(List.of());
        Mockito.when((Object)this.team.getOnlineMembers(TeamType.SPECTATOR)).thenReturn(List.of());
        this.game = (GameManager)Mockito.mock(GameManager.class);
        Mockito.when((Object)this.game.getGameState()).thenReturn((Object)GameState.RUNNING);
        Mockito.when((Object)this.game.getGameStartTime()).thenReturn((Object)(System.currentTimeMillis() - 3660000L));
        this.expansion = new ManhuntPlaceholderExpansion(plugin, this.game, this.team);
    }

    @Test
    void statePlaceholder() {
        Assertions.assertEquals((Object)"running", (Object)this.expansion.onRequest(null, "state"));
        Mockito.when((Object)this.game.getGameState()).thenReturn((Object)GameState.ENDED);
        Assertions.assertEquals((Object)"ended", (Object)this.expansion.onRequest(null, "state"));
    }

    @Test
    void teamPlaceholderForOnlinePlayer() {
        OfflinePlayer offline = (OfflinePlayer)Mockito.mock(OfflinePlayer.class);
        Player player = (Player)Mockito.mock(Player.class);
        Mockito.when((Object)offline.isOnline()).thenReturn((Object)true);
        Mockito.when((Object)offline.getPlayer()).thenReturn((Object)player);
        Mockito.when((Object)this.team.getTeam(player)).thenReturn((Object)TeamType.RUNNER);
        Assertions.assertEquals((Object)"runner", (Object)this.expansion.onRequest(offline, "team"));
    }

    @Test
    void teamPlaceholderForOfflinePlayerIsNone() {
        OfflinePlayer offline = (OfflinePlayer)Mockito.mock(OfflinePlayer.class);
        Mockito.when((Object)offline.isOnline()).thenReturn((Object)false);
        Assertions.assertEquals((Object)"none", (Object)this.expansion.onRequest(offline, "team"));
    }

    @Test
    void runnersAliveIncludesDisconnectedInsideGrace() {
        Assertions.assertEquals((Object)"2", (Object)this.expansion.onRequest(null, "runners_alive"));
    }

    @Test
    void runnersDisconnectedCountsOnlyGraceRunners() {
        Assertions.assertEquals((Object)"1", (Object)this.expansion.onRequest(null, "runners_disconnected"));
    }

    @Test
    void countPlaceholders() {
        Assertions.assertEquals((Object)"1", (Object)this.expansion.onRequest(null, "hunter_count"));
        Assertions.assertEquals((Object)"0", (Object)this.expansion.onRequest(null, "runner_count"));
        Assertions.assertEquals((Object)"0", (Object)this.expansion.onRequest(null, "spectator_count"));
    }

    @Test
    void gameTimeFormatsRunningDuration() {
        Assertions.assertEquals((Object)"01:01:00", (Object)this.expansion.onRequest(null, "game_time"));
        Mockito.when((Object)this.game.getGameState()).thenReturn((Object)GameState.WAITING);
        Assertions.assertEquals((Object)"--:--:--", (Object)this.expansion.onRequest(null, "game_time"));
    }

    @Test
    void winnerPlaceholder() {
        Assertions.assertEquals((Object)"", (Object)this.expansion.onRequest(null, "winner"));
        Mockito.when((Object)this.game.getWinner()).thenReturn((Object)TeamType.HUNTER);
        Assertions.assertEquals((Object)"hunter", (Object)this.expansion.onRequest(null, "winner"));
    }

    @Test
    void playerAlivePlaceholder() {
        OfflinePlayer alive = this.offlinePlayer(this.runnerAlive);
        OfflinePlayer disconnected = this.offlinePlayer(this.runnerDisconnected);
        OfflinePlayer dead = this.offlinePlayer(this.runnerDead);
        OfflinePlayer hunterOffline = this.offlinePlayer(this.hunter);
        OfflinePlayer outsiderOffline = this.offlinePlayer(this.outsider);
        Assertions.assertEquals((Object)"true", (Object)this.expansion.onRequest(alive, "player_alive"));
        Assertions.assertEquals((Object)"true", (Object)this.expansion.onRequest(disconnected, "player_alive"));
        Assertions.assertEquals((Object)"false", (Object)this.expansion.onRequest(dead, "player_alive"));
        Assertions.assertEquals((Object)"true", (Object)this.expansion.onRequest(hunterOffline, "player_alive"));
        Assertions.assertEquals((Object)"", (Object)this.expansion.onRequest(outsiderOffline, "player_alive"));
    }

    @Test
    void unknownPlaceholderReturnsNull() {
        Assertions.assertNull((Object)this.expansion.onRequest(null, "not_a_placeholder"));
    }

    private OfflinePlayer offlinePlayer(UUID id) {
        OfflinePlayer offline = (OfflinePlayer)Mockito.mock(OfflinePlayer.class);
        Mockito.when((Object)offline.getUniqueId()).thenReturn((Object)id);
        return offline;
    }
}
