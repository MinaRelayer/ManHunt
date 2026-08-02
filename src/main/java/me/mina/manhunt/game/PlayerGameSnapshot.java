/*
 * Decompiled with CFR 0.152.
 */
package me.mina.manhunt.game;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scoreboard.Scoreboard;

public final class PlayerGameSnapshot {
    private final UUID playerId;
    private final Location location;
    private final GameMode gameMode;
    private final boolean allowFlight;
    private final boolean flying;
    private final ItemStack[] contents;
    private final ItemStack[] armorContents;
    private final ItemStack offHand;
    private final double health;
    private final int foodLevel;
    private final float saturation;
    private final float exhaustion;
    private final float exp;
    private final int level;
    private final int totalExperience;
    private final List<PotionEffect> effects;
    private final int fireTicks;
    private final float fallDistance;
    private final Scoreboard scoreboard;
    private final Map<NamespacedKey, Set<String>> advancements;

    private PlayerGameSnapshot(UUID playerId, Location location, GameMode gameMode, boolean allowFlight, boolean flying, ItemStack[] contents, ItemStack[] armorContents, ItemStack offHand, double health, int foodLevel, float saturation, float exhaustion, float exp, int level, int totalExperience, List<PotionEffect> effects, int fireTicks, float fallDistance, Scoreboard scoreboard, Map<NamespacedKey, Set<String>> advancements) {
        this.playerId = playerId;
        this.location = location == null ? null : location.clone();
        this.gameMode = gameMode;
        this.allowFlight = allowFlight;
        this.flying = flying;
        this.contents = PlayerGameSnapshot.cloneItems(contents);
        this.armorContents = PlayerGameSnapshot.cloneItems(armorContents);
        this.offHand = offHand == null ? null : offHand.clone();
        this.health = health;
        this.foodLevel = foodLevel;
        this.saturation = saturation;
        this.exhaustion = exhaustion;
        this.exp = exp;
        this.level = level;
        this.totalExperience = totalExperience;
        this.effects = List.copyOf(effects);
        this.fireTicks = fireTicks;
        this.fallDistance = fallDistance;
        this.scoreboard = scoreboard;
        this.advancements = copyAdvancements(advancements);
    }

    public static PlayerGameSnapshot capture(Player player) {
        PlayerInventory inventory = player.getInventory();
        return new PlayerGameSnapshot(player.getUniqueId(), player.getLocation(), player.getGameMode(), player.getAllowFlight(), player.isFlying(), inventory == null ? null : inventory.getContents(), inventory == null ? null : inventory.getArmorContents(), inventory == null ? null : inventory.getItemInOffHand(), player.getHealth(), player.getFoodLevel(), player.getSaturation(), player.getExhaustion(), player.getExp(), player.getLevel(), player.getTotalExperience(), new ArrayList<PotionEffect>(player.getActivePotionEffects()), player.getFireTicks(), player.getFallDistance(), player.getScoreboard(), captureAdvancements(player));
    }

    public UUID getPlayerId() {
        return this.playerId;
    }

    public Location getLocation() {
        return this.location == null ? null : this.location.clone();
    }

    public void restore(Player player) {
        PlayerInventory inventory = player.getInventory();
        if (inventory != null) {
            inventory.setContents(PlayerGameSnapshot.cloneItems(this.contents));
            inventory.setArmorContents(PlayerGameSnapshot.cloneItems(this.armorContents));
            inventory.setItemInOffHand(this.offHand == null ? null : this.offHand.clone());
        }
        player.setGameMode(this.gameMode == null ? GameMode.SURVIVAL : this.gameMode);
        player.setAllowFlight(this.allowFlight);
        player.setFlying(this.allowFlight && this.flying);
        player.setFoodLevel(this.foodLevel);
        player.setSaturation(this.saturation);
        player.setExhaustion(this.exhaustion);
        player.setExp(this.exp);
        player.setLevel(this.level);
        player.setTotalExperience(this.totalExperience);
        player.setFireTicks(this.fireTicks);
        player.setFallDistance(this.fallDistance);
        for (PotionEffect active : new ArrayList<PotionEffect>(player.getActivePotionEffects())) {
            player.removePotionEffect(active.getType());
        }
        for (PotionEffect effect : this.effects) {
            player.addPotionEffect(effect);
        }
        double maxHealth = player.getMaxHealth();
        player.setHealth(Math.max(0.0, Math.min(this.health, maxHealth)));
        if (this.scoreboard != null) {
            player.setScoreboard(this.scoreboard);
        }
        if (this.location != null && this.location.getWorld() != null) {
            player.teleport(this.location);
        }
        restoreAdvancements(player);
    }

    /** Captures only awarded criteria, keeping the snapshot small and lossless. */
    private static Map<NamespacedKey, Set<String>> captureAdvancements(Player player) {
        Map<NamespacedKey, Set<String>> captured = new HashMap<NamespacedKey, Set<String>>();
        try {
            Iterator<Advancement> iterator = Bukkit.advancementIterator();
            while (iterator.hasNext()) {
                Advancement advancement = iterator.next();
                AdvancementProgress progress = player.getAdvancementProgress(advancement);
                if (progress == null || progress.getAwardedCriteria().isEmpty()) {
                    continue;
                }
                captured.put(advancement.getKey(), new HashSet<String>(progress.getAwardedCriteria()));
            }
        } catch (RuntimeException ignored) {
            // A partially initialized server may not expose its advancement registry.
            // Player state restoration must remain safe in that situation.
        }
        return captured;
    }

    private void restoreAdvancements(Player player) {
        try {
            Iterator<Advancement> iterator = Bukkit.advancementIterator();
            while (iterator.hasNext()) {
                Advancement advancement = iterator.next();
                AdvancementProgress progress = player.getAdvancementProgress(advancement);
                if (progress == null) {
                    continue;
                }
                for (String criterion : new HashSet<String>(progress.getAwardedCriteria())) {
                    progress.revokeCriteria(criterion);
                }
                Set<String> awarded = this.advancements.get(advancement.getKey());
                if (awarded != null) {
                    for (String criterion : awarded) {
                        progress.awardCriteria(criterion);
                    }
                }
            }
        } catch (RuntimeException ignored) {
            // Do not prevent inventory/location restoration if advancement APIs fail.
        }
    }

    private static Map<NamespacedKey, Set<String>> copyAdvancements(Map<NamespacedKey, Set<String>> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<NamespacedKey, Set<String>> copy = new HashMap<NamespacedKey, Set<String>>();
        for (Map.Entry<NamespacedKey, Set<String>> entry : source.entrySet()) {
            copy.put(entry.getKey(), Collections.unmodifiableSet(new HashSet<String>(entry.getValue())));
        }
        return Collections.unmodifiableMap(copy);
    }

    private static ItemStack[] cloneItems(ItemStack[] source) {
        if (source == null) {
            return null;
        }
        ItemStack[] copy = new ItemStack[source.length];
        for (int i = 0; i < source.length; ++i) {
            copy[i] = source[i] == null ? null : source[i].clone();
        }
        return copy;
    }
}
