/*
 * Decompiled with CFR 0.152.
 */
package me.mina.manhunt.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.mina.manhunt.game.GameManager;
import me.mina.manhunt.game.GameState;
import me.mina.manhunt.game.PlayerSession;
import me.mina.manhunt.team.TeamManager;
import me.mina.manhunt.team.TeamType;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public class ManhuntPlaceholderExpansion
extends PlaceholderExpansion {
    private final Plugin plugin;
    private final GameManager gameManager;
    private final TeamManager teamManager;

    public ManhuntPlaceholderExpansion(Plugin plugin, GameManager gameManager, TeamManager teamManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
        this.teamManager = teamManager;
    }

    @NotNull
    public String getIdentifier() {
        return "manhunt";
    }

    @NotNull
    public String getAuthor() {
        return "MinaRelayer";
    }

    @NotNull
    public String getVersion() {
        return this.plugin.getPluginMeta().getVersion();
    }

    public boolean persist() {
        return true;
    }

    public String onRequest(OfflinePlayer offlinePlayer, @NotNull String params) {
        switch (params.toLowerCase()) {
            case "team": {
                if (offlinePlayer == null || !offlinePlayer.isOnline()) {
                    return "none";
                }
                return this.teamManager.getTeam(offlinePlayer.getPlayer()).name().toLowerCase();
            }
            case "state": {
                return this.gameManager.getGameState().name().toLowerCase();
            }
            case "runners_alive": {
                return String.valueOf(this.teamManager.tracker().getAliveRunnerIds().size());
            }
            case "runners_disconnected": {
                return String.valueOf(this.teamManager.tracker().getDisconnectedRunnerIds().size());
            }
            case "hunter_count": {
                return String.valueOf(this.teamManager.getOnlineMembers(TeamType.HUNTER).size());
            }
            case "runner_count": {
                return String.valueOf(this.teamManager.getOnlineMembers(TeamType.RUNNER).size());
            }
            case "spectator_count": {
                return String.valueOf(this.teamManager.getOnlineMembers(TeamType.SPECTATOR).size());
            }
            case "game_time": {
                return this.formatGameTime();
            }
            case "winner": {
                TeamType winner = this.gameManager.getWinner();
                return winner == null ? "" : winner.name().toLowerCase();
            }
            case "player_alive": {
                return this.playerAlive(offlinePlayer);
            }
        }
        return null;
    }

    private String playerAlive(OfflinePlayer offlinePlayer) {
        if (offlinePlayer == null) {
            return "";
        }
        PlayerSession session = this.teamManager.tracker().getSession(offlinePlayer.getUniqueId());
        if (session == null) {
            return "";
        }
        if (session.getTeam() == TeamType.HUNTER) {
            return "true";
        }
        if (session.getTeam() == TeamType.RUNNER) {
            return String.valueOf(session.isAlive());
        }
        return "";
    }

    private String formatGameTime() {
        if (this.gameManager.getGameState() != GameState.RUNNING) {
            return "--:--:--";
        }
        long elapsedMillis = System.currentTimeMillis() - this.gameManager.getGameStartTime();
        long totalSeconds = elapsedMillis / 1000L;
        long hours = totalSeconds / 3600L;
        long minutes = totalSeconds % 3600L / 60L;
        long seconds = totalSeconds % 60L;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}
