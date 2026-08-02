/*
 * Decompiled with CFR 0.152.
 */
package me.mina.manhunt.listener;

import me.mina.manhunt.game.GameManager;
import me.mina.manhunt.game.GameState;
import me.mina.manhunt.game.RespawnIntent;
import me.mina.manhunt.team.TeamManager;
import me.mina.manhunt.team.TeamType;
import me.mina.manhunt.world.WorldManager;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.Plugin;

public class ManhuntListener
implements Listener {
    private final Plugin plugin;
    private final GameManager gameManager;
    private final TeamManager teamManager;
    private final WorldManager worldManager;

    public ManhuntListener(Plugin plugin, GameManager gameManager, TeamManager teamManager, WorldManager worldManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
        this.teamManager = teamManager;
        this.worldManager = worldManager;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (this.gameManager.getGameState() != GameState.RUNNING) {
            return;
        }
        Player player = event.getEntity();
        if (!this.gameManager.isParticipant(player) || !this.worldManager.isGameWorld(player.getWorld())) {
            return;
        }
        TeamType team = this.teamManager.getTeam(player);
        if (team == TeamType.RUNNER) {
            this.teamManager.markDead(player);
            this.teamManager.tracker().setRespawnIntent(player.getUniqueId(), RespawnIntent.SPECTATOR);
            this.gameManager.onRunnerDeath();
        } else if (team == TeamType.HUNTER) {
            this.teamManager.tracker().setRespawnIntent(player.getUniqueId(), RespawnIntent.SURVIVAL);
        } else if (team == TeamType.SPECTATOR) {
            this.teamManager.tracker().setRespawnIntent(player.getUniqueId(), RespawnIntent.SPECTATOR);
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        RespawnIntent intent = this.teamManager.tracker().getAndClearRespawnIntent(player.getUniqueId());
        if (intent == RespawnIntent.NONE || this.gameManager.getGameState() != GameState.RUNNING || !this.gameManager.isParticipant(player)) {
            return;
        }
        World gameWorld = this.worldManager.getGameWorld();
        if (gameWorld == null) {
            return;
        }
        Location vanilla = event.getRespawnLocation();
        if (vanilla == null || vanilla.getWorld() == null || !vanilla.getWorld().equals((Object)gameWorld)) {
            event.setRespawnLocation(gameWorld.getSpawnLocation());
        }
        GameMode mode = intent == RespawnIntent.SURVIVAL ? GameMode.SURVIVAL : GameMode.SPECTATOR;
        Bukkit.getScheduler().runTask(this.plugin, () -> {
            if (this.gameManager.getGameState() == GameState.RUNNING && this.gameManager.isParticipant(player)) {
                player.setGameMode(mode);
            }
        });
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity livingEntity = event.getEntity();
        if (livingEntity instanceof EnderDragon) {
            EnderDragon dragon = (EnderDragon)livingEntity;
            if (this.gameManager.getGameState() == GameState.RUNNING && this.worldManager.getGameWorldTheEnd() != null && dragon.getWorld().equals((Object)this.worldManager.getGameWorldTheEnd())) {
                this.gameManager.onEnderDragonDeath();
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        this.gameManager.handlePlayerQuit(event.getPlayer());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        this.gameManager.handlePlayerJoin(event.getPlayer());
    }
}
