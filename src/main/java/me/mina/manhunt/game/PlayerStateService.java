/*
 * Decompiled with CFR 0.152.
 */
package me.mina.manhunt.game;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import me.mina.manhunt.game.PlayerGameSnapshot;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;

public final class PlayerStateService {
    private final Map<UUID, PlayerGameSnapshot> active = new HashMap<UUID, PlayerGameSnapshot>();
    private final Map<UUID, PlayerGameSnapshot> pendingRestore = new HashMap<UUID, PlayerGameSnapshot>();

    public void capture(Player player) {
        this.active.put(player.getUniqueId(), PlayerGameSnapshot.capture(player));
    }

    public boolean hasSnapshot(UUID playerId) {
        return this.active.containsKey(playerId);
    }

    public void prepareForGame(Player player) {
        if (player.getInventory() != null) {
            player.getInventory().clear();
        }
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.setSaturation(5.0f);
        player.setExhaustion(0.0f);
        player.setExp(0.0f);
        player.setLevel(0);
        player.setTotalExperience(0);
        player.setFireTicks(0);
        player.setFallDistance(0.0f);
        Collection<PotionEffect> activeEffects = player.getActivePotionEffects();
        if (activeEffects != null) {
            for (PotionEffect effect : activeEffects) {
                player.removePotionEffect(effect.getType());
            }
        }
        player.setAllowFlight(false);
        player.setFlying(false);
    }

    public void restore(Player player) {
        PlayerGameSnapshot snapshot = this.active.remove(player.getUniqueId());
        if (snapshot == null) {
            snapshot = this.pendingRestore.remove(player.getUniqueId());
        }
        if (snapshot != null) {
            snapshot.restore(player);
        }
    }

    public void markPendingRestore(UUID playerId) {
        PlayerGameSnapshot snapshot = this.active.remove(playerId);
        if (snapshot != null) {
            this.pendingRestore.put(playerId, snapshot);
        }
    }

    public boolean hasPendingRestore(UUID playerId) {
        return this.pendingRestore.containsKey(playerId);
    }

    public void restorePending(Player player) {
        PlayerGameSnapshot snapshot = this.pendingRestore.remove(player.getUniqueId());
        if (snapshot != null) {
            snapshot.restore(player);
        }
    }

    public void restoreAllOnline(Iterable<Player> players) {
        for (Player player : players) {
            this.restore(player);
        }
    }

    public void markAllOfflinePending(Iterable<UUID> participantIds, Predicate<UUID> online) {
        for (UUID id : participantIds) {
            if (online.test(id)) continue;
            this.markPendingRestore(id);
        }
    }

    public void clear() {
        this.active.clear();
        this.pendingRestore.clear();
    }
}
