/*
 * Decompiled with CFR 0.152.
 */
package me.mina.manhunt.game;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import me.mina.manhunt.config.PluginConfig;
import me.mina.manhunt.game.GameManager;
import me.mina.manhunt.game.GameState;
import me.mina.manhunt.game.PlayerStateService;
import me.mina.manhunt.game.ServerBridge;
import me.mina.manhunt.lang.LangManager;
import me.mina.manhunt.scoreboard.ScoreboardManager;
import me.mina.manhunt.team.TeamManager;
import me.mina.manhunt.team.TeamType;
import me.mina.manhunt.world.WorldManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

public class ManhuntGameManager
implements GameManager {
    private static final int TITLE_FADE_IN = 10;
    private static final int TITLE_FADE_OUT = 20;
    private static final int TITLE_GENERATING_STAY = 200;
    private static final int TITLE_VICTORY_STAY = 100;
    private static final long WORLD_TELEPORT_DELAY = 20L;
    private static final long GRACE_SWEEP_DELAY = 20L;
    private static final long GRACE_SWEEP_PERIOD = 20L;
    private static final long REJOIN_DELAY = 1L;
    private final ServerBridge bridge;
    private final TeamManager teamManager;
    private final WorldManager worldManager;
    private final ScoreboardManager scoreboardManager;
    private final LangManager langManager;
    private final PluginConfig config;
    private final PlayerStateService playerStateService;
    private final Set<UUID> participants = new HashSet<UUID>();
    private GameState gameState = GameState.WAITING;
    private long gameStartTime;
    private TeamType winner;
    private long generation;
    private BukkitTask graceSweepTask;

    public ManhuntGameManager(ServerBridge bridge, TeamManager teamManager, WorldManager worldManager, ScoreboardManager scoreboardManager, LangManager langManager, PluginConfig config) {
        this(bridge, teamManager, worldManager, scoreboardManager, langManager, config, new PlayerStateService());
    }

    public ManhuntGameManager(ServerBridge bridge, TeamManager teamManager, WorldManager worldManager, ScoreboardManager scoreboardManager, LangManager langManager, PluginConfig config, PlayerStateService playerStateService) {
        this.bridge = bridge;
        this.teamManager = teamManager;
        this.worldManager = worldManager;
        this.scoreboardManager = scoreboardManager;
        this.langManager = langManager;
        this.config = config;
        this.playerStateService = playerStateService;
    }

    @Override
    public boolean startGame() {
        World gameWorld;
        if (this.gameState == GameState.CLEANUP) {
            this.broadcast(this.langManager.getComponent("game-cleanup-pending", new Object[0]));
            return false;
        }
        if (this.gameState != GameState.WAITING) {
            this.broadcast(this.langManager.getComponent("game-already-running", new Object[0]));
            return false;
        }
        if (this.teamManager.getOnlineMembers(TeamType.HUNTER).isEmpty() || this.teamManager.getOnlineMembers(TeamType.RUNNER).isEmpty()) {
            this.broadcast(this.langManager.getComponent("game-start-not-enough", new Object[0]));
            return false;
        }
        this.participants.clear();
        ArrayList<Player> participantPlayers = new ArrayList<Player>();
        for (Player player : this.bridge.getOnlinePlayers()) {
            TeamType team = this.teamManager.getTeam(player);
            if (team != TeamType.HUNTER && team != TeamType.RUNNER && team != TeamType.SPECTATOR) continue;
            this.participants.add(player.getUniqueId());
            participantPlayers.add(player);
            this.playerStateService.capture(player);
        }
        this.winner = null;
        this.gameState = GameState.STARTING;
        long startGeneration = ++this.generation;
        this.showTitleToAll("title-generating", "title-generating-sub", 200);
        try {
            gameWorld = this.worldManager.createGameWorld();
            if (gameWorld == null || this.worldManager.getGameWorldNether() == null || this.worldManager.getGameWorldTheEnd() == null) {
                throw new IllegalStateException("Missing game dimension");
            }
            this.applyDifficulty(gameWorld, this.worldManager.getGameWorldNether(), this.worldManager.getGameWorldTheEnd());
        }
        catch (Exception ex) {
            this.bridge.runTaskLater(() -> this.finishFailedStart(startGeneration), 1L);
            return false;
        }
        this.bridge.runTaskLater(() -> {
            if (this.gameState != GameState.STARTING || startGeneration != this.generation) {
                return;
            }
            ArrayList<Player> onlineParticipants = new ArrayList<Player>();
            for (Player player : this.bridge.getOnlinePlayers()) {
                if (!this.participants.contains(player.getUniqueId())) continue;
                onlineParticipants.add(player);
                this.playerStateService.prepareForGame(player);
                player.teleport(gameWorld.getSpawnLocation());
                this.setModeForTeam(player);
            }
            this.gameState = GameState.RUNNING;
            this.gameStartTime = System.currentTimeMillis();
            this.scoreboardManager.start(this.gameStartTime, onlineParticipants);
            this.startGraceSweep();
            this.broadcast(this.langManager.getComponent("game-start", new Object[0]));
            this.onRunnerDeath();
        }, 20L);
        return true;
    }

    private void finishFailedStart(long startGeneration) {
        if (startGeneration != this.generation || this.gameState != GameState.STARTING) {
            return;
        }
        this.gameState = GameState.CLEANUP;
        long cleanupGeneration = ++this.generation;
        this.worldManager.cleanupGameWorlds(success -> {
            if (cleanupGeneration != this.generation) {
                return;
            }
            this.restoreParticipantStates();
            if (!success.booleanValue()) {
                this.broadcast(this.langManager.getComponent("game-cleanup-failed", new Object[0]));
                return;
            }
            this.participants.clear();
            this.gameState = GameState.WAITING;
            this.broadcast(this.langManager.getComponent("game-world-failed", new Object[0]));
        });
    }

    private void applyDifficulty(World overworld, World nether, World end) {
        Difficulty difficulty = this.config.getWorldDifficulty();
        overworld.setDifficulty(difficulty);
        nether.setDifficulty(difficulty);
        end.setDifficulty(difficulty);
    }

    private void restoreParticipantStates() {
        for (UUID id : Set.copyOf(this.participants)) {
            Player player = this.findOnline(id);
            if (player != null) {
                this.playerStateService.restore(player);
                continue;
            }
            this.playerStateService.markPendingRestore(id);
        }
    }

    @Override
    public void endGame() {
        if (this.gameState == GameState.WAITING || this.gameState == GameState.CLEANUP) {
            return;
        }
        this.stopGraceSweep();
        this.scoreboardManager.stop();
        if (this.winner == null) {
            this.broadcast(this.langManager.getComponent("game-end", new Object[0]));
        }
        this.gameState = GameState.CLEANUP;
        long cleanupGeneration = ++this.generation;
        Set<UUID> currentParticipants = Set.copyOf(this.participants);
        for (UUID id : currentParticipants) {
            Player player = this.findOnline(id);
            if (player != null) {
                this.playerStateService.restore(player);
                continue;
            }
            this.playerStateService.markPendingRestore(id);
        }
        this.worldManager.cleanupGameWorlds(success -> {
            if (cleanupGeneration != this.generation) {
                return;
            }
            if (!success.booleanValue()) {
                this.broadcast(this.langManager.getComponent("game-cleanup-failed", new Object[0]));
                return;
            }
            this.teamManager.clearAll();
            this.participants.clear();
            this.gameState = GameState.WAITING;
        });
    }

    @Override
    public void declareWinner(TeamType teamType) {
        if (this.gameState != GameState.RUNNING || teamType != TeamType.RUNNER && teamType != TeamType.HUNTER) {
            return;
        }
        this.winner = teamType;
        this.stopGraceSweep();
        String titleKey = teamType == TeamType.RUNNER ? "title-victory-runner" : "title-victory-hunter";
        String subKey = teamType == TeamType.RUNNER ? "title-victory-runner-sub" : "title-victory-hunter-sub";
        this.showTitleToAll(titleKey, subKey, 100);
        this.broadcast(this.langManager.getComponent(teamType == TeamType.RUNNER ? "game-winner-runner" : "game-winner-hunter", new Object[0]));
        this.scoreboardManager.freeze();
        for (Player player : this.bridge.getOnlinePlayers()) {
            if (!this.participants.contains(player.getUniqueId())) continue;
            player.setGameMode(GameMode.SPECTATOR);
        }
        this.gameState = GameState.ENDED;
    }

    @Override
    public void onRunnerDeath() {
        if (this.gameState == GameState.RUNNING && this.teamManager.tracker().getAliveRunnerIds().isEmpty()) {
            this.declareWinner(TeamType.HUNTER);
        }
    }

    @Override
    public void onEnderDragonDeath() {
        if (this.gameState == GameState.RUNNING) {
            this.declareWinner(TeamType.RUNNER);
        }
    }

    @Override
    public void handlePlayerQuit(Player player) {
        if (this.gameState != GameState.STARTING && this.gameState != GameState.RUNNING || !this.isParticipant(player)) {
            return;
        }
        TeamType team = this.teamManager.getTeam(player);
        this.teamManager.disconnect(player);
        if (team != TeamType.RUNNER) {
            return;
        }
        if (this.config.getDisconnectGraceSeconds() <= 0) {
            this.teamManager.markDead(player);
            this.broadcast(this.langManager.getComponent("game-runner-timed-out", "player", player.getName()));
            if (this.gameState == GameState.RUNNING) {
                this.onRunnerDeath();
            }
        } else {
            this.broadcast(this.langManager.getComponent("game-runner-disconnected", "player", player.getName(), "seconds", String.valueOf(this.config.getDisconnectGraceSeconds())));
        }
    }

    @Override
    public void handlePlayerRejoin(Player player) {
        this.handlePlayerJoin(player);
    }

    @Override
    public void handlePlayerJoin(Player player) {
        if (this.gameState == GameState.WAITING && this.playerStateService.hasPendingRestore(player.getUniqueId())) {
            this.bridge.runTaskLater(() -> this.playerStateService.restorePending(player), 1L);
            return;
        }
        if (!this.isParticipant(player)) {
            return;
        }
        if (this.gameState == GameState.STARTING) {
            this.teamManager.reconnect(player);
            return;
        }
        if (this.gameState != GameState.RUNNING) {
            return;
        }
        boolean wasDisconnected = this.teamManager.isDisconnected(player);
        this.teamManager.reconnect(player);
        long rejoinGeneration = this.generation;
        this.bridge.runTaskLater(() -> {
            if (rejoinGeneration != this.generation || this.gameState != GameState.RUNNING || !this.isParticipant(player)) {
                return;
            }
            World gameWorld = this.worldManager.getGameWorld();
            if (gameWorld != null) {
                player.teleport(gameWorld.getSpawnLocation());
            }
            this.setModeForTeam(player);
            this.scoreboardManager.applyToPlayer(player);
            if (wasDisconnected) {
                this.broadcast(this.langManager.getComponent("game-runner-reconnected", "player", player.getName()));
            }
        }, 1L);
    }

    private void setModeForTeam(Player player) {
        TeamType team = this.teamManager.getTeam(player);
        if (team == TeamType.SPECTATOR || team == TeamType.RUNNER && !this.teamManager.isAlive(player)) {
            player.setGameMode(GameMode.SPECTATOR);
        } else if (team == TeamType.HUNTER || team == TeamType.RUNNER) {
            player.setGameMode(GameMode.SURVIVAL);
        } else {
            player.setGameMode(GameMode.SPECTATOR);
        }
    }

    private void startGraceSweep() {
        if (this.graceSweepTask != null) {
            return;
        }
        this.graceSweepTask = this.bridge.runTaskTimer(() -> {
            if (this.gameState != GameState.RUNNING) {
                return;
            }
            long graceMillis = (long)this.config.getDisconnectGraceSeconds() * 1000L;
            if (graceMillis <= 0L) {
                return;
            }
            List<UUID> timedOut = this.teamManager.sweepGrace(System.currentTimeMillis(), graceMillis);
            for (UUID id : timedOut) {
                String name = this.bridge.getPlayerName(id);
                this.broadcast(this.langManager.getComponent("game-runner-timed-out", "player", name == null ? id.toString() : name));
            }
            if (!timedOut.isEmpty()) {
                this.onRunnerDeath();
            }
        }, 20L, 20L);
    }

    private void stopGraceSweep() {
        if (this.graceSweepTask != null) {
            this.graceSweepTask.cancel();
            this.graceSweepTask = null;
        }
    }

    private Player findOnline(UUID id) {
        for (Player player : this.bridge.getOnlinePlayers()) {
            if (!player.getUniqueId().equals(id)) continue;
            return player;
        }
        return null;
    }

    private void showTitleToAll(String titleKey, String subtitleKey, int stayTicks) {
        Component title = this.langManager.getComponentWithoutPrefix(titleKey, new Object[0]);
        Component subtitle = this.langManager.getComponentWithoutPrefix(subtitleKey, new Object[0]);
        Title titleObj = Title.title((Component)title, (Component)subtitle, (Title.Times)Title.Times.times((Duration)Duration.ofMillis(500L), (Duration)Duration.ofMillis((long)stayTicks * 50L), (Duration)Duration.ofMillis(1000L)));
        for (Player player : this.bridge.getOnlinePlayers()) {
            player.showTitle(titleObj);
        }
    }

    private void broadcast(Component component) {
        for (Player player : this.bridge.getOnlinePlayers()) {
            player.sendMessage(component);
        }
    }

    @Override
    public GameState getGameState() {
        return this.gameState;
    }

    @Override
    public long getGameStartTime() {
        return this.gameStartTime;
    }

    @Override
    public TeamType getWinner() {
        return this.winner;
    }

    @Override
    public boolean isEnding() {
        return this.gameState == GameState.CLEANUP;
    }

    @Override
    public boolean isStarting() {
        return this.gameState == GameState.STARTING;
    }

    @Override
    public boolean isCleanupPending() {
        return this.gameState == GameState.CLEANUP;
    }

    @Override
    public boolean isParticipant(Player player) {
        return player != null && this.participants.contains(player.getUniqueId());
    }

    @Override
    public boolean isGameWorld(World world) {
        return this.worldManager.isGameWorld(world);
    }

    @Override
    public void shutdown() {
        this.stopGraceSweep();
        this.scoreboardManager.stop();
        this.playerStateService.restoreAllOnline(this.bridge.getOnlinePlayers());
        this.playerStateService.clear();
        this.teamManager.clearAll();
        this.participants.clear();
        ++this.generation;
        this.worldManager.forceDeleteAllWorlds();
        this.gameState = GameState.WAITING;
    }
}
