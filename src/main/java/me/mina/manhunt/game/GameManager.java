/*
 * Decompiled with CFR 0.152.
 */
package me.mina.manhunt.game;

import me.mina.manhunt.game.GameState;
import me.mina.manhunt.team.TeamType;
import org.bukkit.World;
import org.bukkit.entity.Player;

public interface GameManager {
    public boolean startGame();

    public void endGame();

    public void declareWinner(TeamType var1);

    public void onRunnerDeath();

    public void onEnderDragonDeath();

    public void handlePlayerQuit(Player var1);

    public void handlePlayerRejoin(Player var1);

    default public void handlePlayerJoin(Player player) {
        this.handlePlayerRejoin(player);
    }

    public GameState getGameState();

    public long getGameStartTime();

    public TeamType getWinner();

    default public boolean isEnding() {
        return this.getGameState() == GameState.CLEANUP;
    }

    default public boolean isStarting() {
        return this.getGameState() == GameState.STARTING;
    }

    default public boolean isCleanupPending() {
        return this.getGameState() == GameState.CLEANUP;
    }

    default public boolean isParticipant(Player player) {
        return false;
    }

    default public boolean isGameWorld(World world) {
        return false;
    }

    public void shutdown();
}
