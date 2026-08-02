/*
 * Decompiled with CFR 0.152.
 */
package me.mina.manhunt.game;

import java.util.List;
import java.util.UUID;
import me.mina.manhunt.config.PluginConfig;
import me.mina.manhunt.game.GameState;
import me.mina.manhunt.game.ManhuntGameManager;
import me.mina.manhunt.game.ServerBridge;
import me.mina.manhunt.lang.LangManager;
import me.mina.manhunt.scoreboard.ScoreboardManager;
import me.mina.manhunt.support.FakeLangManager;
import me.mina.manhunt.support.FakeScoreboardManager;
import me.mina.manhunt.support.FakeServerBridge;
import me.mina.manhunt.support.FakeTeamManager;
import me.mina.manhunt.support.FakeWorldManager;
import me.mina.manhunt.team.TeamManager;
import me.mina.manhunt.team.TeamType;
import me.mina.manhunt.world.WorldManager;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mockito.verification.VerificationMode;

class ManhuntGameManagerTest {
    private PluginConfig config;
    private FakeTeamManager team;
    private FakeWorldManager world;
    private FakeScoreboardManager scoreboard;
    private FakeLangManager lang;
    private FakeServerBridge bridge;
    private ManhuntGameManager game;
    private final UUID hunterId = UUID.randomUUID();
    private final UUID runnerId = UUID.randomUUID();
    private final UUID runner2Id = UUID.randomUUID();
    private Player hunter;
    private Player runner;
    private Player runner2;

    ManhuntGameManagerTest() {
    }

    @BeforeEach
    void setUp() {
        this.config = new PluginConfig();
        this.team = new FakeTeamManager();
        this.world = new FakeWorldManager();
        this.scoreboard = new FakeScoreboardManager();
        this.lang = new FakeLangManager();
        this.bridge = new FakeServerBridge();
        this.game = new ManhuntGameManager((ServerBridge)this.bridge, (TeamManager)this.team, (WorldManager)this.world, (ScoreboardManager)this.scoreboard, (LangManager)this.lang, this.config);
        this.hunter = this.player(this.hunterId, "Hunter");
        this.runner = this.player(this.runnerId, "Runner");
        this.runner2 = this.player(this.runner2Id, "Runner2");
        this.team.addOnline(this.hunter);
        this.team.addOnline(this.runner);
        this.team.addOnline(this.runner2);
        this.team.joinTeam(this.hunter, TeamType.HUNTER);
        this.team.joinTeam(this.runner, TeamType.RUNNER);
        this.team.joinTeam(this.runner2, TeamType.RUNNER);
        this.world.gameWorld = (World)Mockito.mock(World.class);
        this.world.nether = (World)Mockito.mock(World.class);
        this.world.end = (World)Mockito.mock(World.class);
        this.bridge.onlinePlayers.addAll(List.of(this.hunter, this.runner, this.runner2));
        this.bridge.playerNames.put(this.hunterId, "Hunter");
        this.bridge.playerNames.put(this.runnerId, "Runner");
        this.bridge.playerNames.put(this.runner2Id, "Runner2");
    }

    private Player player(UUID id, String name) {
        Player player = (Player)Mockito.mock(Player.class);
        Mockito.when((Object)player.getUniqueId()).thenReturn((Object)id);
        Mockito.when((Object)player.getName()).thenReturn((Object)name);
        Mockito.when((Object)player.getLocation()).thenReturn((Object)new Location(null, 1.0, 64.0, 2.0));
        return player;
    }

    private void startAndCompleteGame() {
        Assertions.assertTrue((boolean)this.game.startGame());
        this.bridge.runAllLaterTasks();
    }

    @Test
    void startGameRejectsWhenNoPlayers() {
        FakeTeamManager empty = new FakeTeamManager();
        this.game = new ManhuntGameManager((ServerBridge)this.bridge, (TeamManager)empty, (WorldManager)this.world, (ScoreboardManager)this.scoreboard, (LangManager)this.lang, this.config);
        Assertions.assertFalse((boolean)this.game.startGame());
        Assertions.assertEquals((Object)GameState.WAITING, (Object)this.game.getGameState());
        Assertions.assertTrue((boolean)this.lang.sentKeys.contains("game-start-not-enough"));
    }

    @Test
    void startGameRejectsWhenAlreadyRunning() {
        this.startAndCompleteGame();
        Assertions.assertFalse((boolean)this.game.startGame());
        Assertions.assertTrue((boolean)this.lang.sentKeys.contains("game-already-running"));
    }

    @Test
    void startGameRejectsWhileCleanupPending() {
        this.startAndCompleteGame();
        this.game.endGame();
        Assertions.assertTrue((boolean)this.game.isCleanupPending());
        Assertions.assertFalse((boolean)this.game.startGame());
        Assertions.assertTrue((boolean)this.lang.sentKeys.contains("game-cleanup-pending"));
    }

    @Test
    void startGameRollsBackWhenWorldCreationFails() {
        this.world.createFails = true;
        Assertions.assertFalse((boolean)this.game.startGame());
        this.bridge.runAllLaterTasks();
        this.world.runCleanupCallbacks();
        Assertions.assertFalse((boolean)this.world.forceDeleted);
        Assertions.assertTrue((boolean)this.world.worldsDeleted);
        Assertions.assertFalse((boolean)this.scoreboard.started);
        Assertions.assertEquals((Object)GameState.WAITING, (Object)this.game.getGameState());
        Assertions.assertTrue((boolean)this.lang.sentKeys.contains("game-world-failed"));
    }

    @Test
    void startAndCompleteTransitionsToRunning() {
        this.startAndCompleteGame();
        Assertions.assertEquals((Object)GameState.RUNNING, (Object)this.game.getGameState());
        Assertions.assertTrue((boolean)this.scoreboard.started);
        Assertions.assertEquals((int)1, (int)this.bridge.timerTasks.size());
        Assertions.assertFalse((boolean)this.game.isStarting());
        Assertions.assertFalse((boolean)this.game.isCleanupPending());
    }

    @Test
    void endGameResetsStateAndCleansUpOnDelayedTask() {
        this.startAndCompleteGame();
        this.game.endGame();
        Assertions.assertEquals((Object)GameState.CLEANUP, (Object)this.game.getGameState());
        Assertions.assertTrue((boolean)this.game.isCleanupPending());
        Assertions.assertFalse((boolean)this.scoreboard.started);
        this.world.runCleanupCallbacks();
        Assertions.assertTrue((boolean)this.world.worldsDeleted);
        Assertions.assertFalse((boolean)this.game.isCleanupPending());
        Assertions.assertTrue((boolean)this.team.tracker().getMemberIds(TeamType.HUNTER).isEmpty());
    }

    @Test
    void endGameIgnoresRepeatedCalls() {
        this.startAndCompleteGame();
        this.game.endGame();
        int scheduledCleanups = this.bridge.laterTasks.size();
        this.game.endGame();
        Assertions.assertEquals((int)scheduledCleanups, (int)this.bridge.laterTasks.size());
    }

    @Test
    void endGameDuringWorldGenerationCleansUpCreatedWorlds() {
        Assertions.assertTrue((boolean)this.game.startGame());
        Assertions.assertTrue((boolean)this.game.isStarting());
        this.game.endGame();
        this.bridge.runAllLaterTasks();
        this.world.runCleanupCallbacks();
        Assertions.assertTrue((boolean)this.world.worldsDeleted);
        Assertions.assertEquals((Object)GameState.WAITING, (Object)this.game.getGameState());
    }

    @Test
    void allRunnersEliminatedHuntersWin() {
        this.startAndCompleteGame();
        this.team.markDead(this.runner);
        this.game.onRunnerDeath();
        Assertions.assertEquals((Object)GameState.RUNNING, (Object)this.game.getGameState());
        this.team.markDead(this.runner2);
        this.game.onRunnerDeath();
        Assertions.assertEquals((Object)GameState.ENDED, (Object)this.game.getGameState());
        Assertions.assertEquals((Object)TeamType.HUNTER, (Object)this.game.getWinner());
    }

    @Test
    void enderDragonDeathRunnersWin() {
        this.startAndCompleteGame();
        Mockito.reset((Object[])new Player[]{this.runner});
        Mockito.when((Object)this.runner.getUniqueId()).thenReturn((Object)this.runnerId);
        this.game.onEnderDragonDeath();
        Assertions.assertEquals((Object)GameState.ENDED, (Object)this.game.getGameState());
        Assertions.assertEquals((Object)TeamType.RUNNER, (Object)this.game.getWinner());
        ((Player)Mockito.verify((Object)this.runner)).setGameMode(GameMode.SPECTATOR);
    }

    @Test
    void runnerQuitInsideGracePeriodStaysAlive() {
        this.startAndCompleteGame();
        this.team.removeOnline(this.runner);
        this.game.handlePlayerQuit(this.runner);
        Assertions.assertTrue((boolean)this.team.tracker().isDisconnected(this.runnerId));
        Assertions.assertTrue((boolean)this.team.tracker().isAlive(this.runnerId));
        Assertions.assertEquals((Object)GameState.RUNNING, (Object)this.game.getGameState());
        Assertions.assertTrue((boolean)this.lang.sentKeys.contains("game-runner-disconnected"));
    }

    @Test
    void runnerQuitWithGraceDisabledIsEliminated() {
        this.config.setDisconnectGraceSeconds(0);
        this.startAndCompleteGame();
        this.team.removeOnline(this.runner2);
        this.game.handlePlayerQuit(this.runner2);
        Assertions.assertFalse((boolean)this.team.tracker().isAlive(this.runner2Id));
        Assertions.assertTrue((boolean)this.lang.sentKeys.contains("game-runner-timed-out"));
        Assertions.assertEquals((int)1, (int)this.team.tracker().getAliveRunnerIds().size());
    }

    @Test
    void lastRunnerQuitWithGraceDisabledEndsGame() {
        this.config.setDisconnectGraceSeconds(0);
        this.startAndCompleteGame();
        this.team.removeOnline(this.runner);
        this.team.removeOnline(this.runner2);
        this.game.handlePlayerQuit(this.runner);
        this.game.handlePlayerQuit(this.runner2);
        Assertions.assertEquals((Object)GameState.ENDED, (Object)this.game.getGameState());
        Assertions.assertEquals((Object)TeamType.HUNTER, (Object)this.game.getWinner());
    }

    @Test
    void hunterQuitDoesNotAffectWinCondition() {
        this.startAndCompleteGame();
        this.team.removeOnline(this.hunter);
        this.game.handlePlayerQuit(this.hunter);
        Assertions.assertTrue((boolean)this.team.tracker().isDisconnected(this.hunterId));
        Assertions.assertEquals((Object)GameState.RUNNING, (Object)this.game.getGameState());
        Assertions.assertEquals((int)2, (int)this.team.tracker().getAliveRunnerIds().size());
    }

    @Test
    void runnerRejoinInsideGraceReturnsToGame() {
        this.startAndCompleteGame();
        this.team.removeOnline(this.runner);
        this.game.handlePlayerQuit(this.runner);
        Assertions.assertTrue((boolean)this.team.tracker().isDisconnected(this.runnerId));
        Mockito.reset((Object[])new Player[]{this.runner});
        Mockito.when((Object)this.runner.getUniqueId()).thenReturn((Object)this.runnerId);
        Mockito.when((Object)this.runner.getName()).thenReturn((Object)"Runner");
        this.team.addOnline(this.runner);
        this.game.handlePlayerRejoin(this.runner);
        Assertions.assertFalse((boolean)this.team.tracker().isDisconnected(this.runnerId));
        Mockito.when((Object)this.world.gameWorld.getSpawnLocation()).thenReturn((Object)new Location(null, 5.0, 64.0, 5.0));
        this.bridge.runAllLaterTasks();
        ((Player)Mockito.verify((Object)this.runner)).setGameMode(GameMode.SURVIVAL);
        ((Player)Mockito.verify((Object)this.runner)).teleport((Location)ArgumentMatchers.any(Location.class));
        Assertions.assertTrue((boolean)this.lang.sentKeys.contains("game-runner-reconnected"));
    }

    @Test
    void rejoinDeadRunnerBecomesSpectator() {
        this.startAndCompleteGame();
        this.team.markDead(this.runner);
        Mockito.reset((Object[])new Player[]{this.runner});
        Mockito.when((Object)this.runner.getUniqueId()).thenReturn((Object)this.runnerId);
        this.team.addOnline(this.runner);
        this.game.handlePlayerRejoin(this.runner);
        this.bridge.runAllLaterTasks();
        ((Player)Mockito.verify((Object)this.runner)).setGameMode(GameMode.SPECTATOR);
        ((Player)Mockito.verify((Object)this.runner, (VerificationMode)Mockito.never())).setGameMode(GameMode.SURVIVAL);
    }

    @Test
    void graceSweepEliminatesExpiredRunners() {
        this.config.setDisconnectGraceSeconds(1);
        this.startAndCompleteGame();
        this.team.removeOnline(this.runner2);
        this.team.tracker().disconnect(this.runner2Id, System.currentTimeMillis() - 2000L);
        this.bridge.runTimerTask(0);
        Assertions.assertFalse((boolean)this.team.tracker().isAlive(this.runner2Id));
        Assertions.assertTrue((boolean)this.lang.sentKeys.contains("game-runner-timed-out"));
        Assertions.assertEquals((int)1, (int)this.team.tracker().getAliveRunnerIds().size());
    }

    @Test
    void shutdownStopsScoreboardClearsTeamsAndForceDeletesWorlds() {
        this.startAndCompleteGame();
        this.game.shutdown();
        Assertions.assertEquals((Object)GameState.WAITING, (Object)this.game.getGameState());
        Assertions.assertFalse((boolean)this.scoreboard.started);
        Assertions.assertTrue((boolean)this.world.forceDeleted);
        Assertions.assertTrue((boolean)this.team.tracker().getMemberIds(TeamType.HUNTER).isEmpty());
        Assertions.assertFalse((boolean)this.game.isCleanupPending());
    }
}
