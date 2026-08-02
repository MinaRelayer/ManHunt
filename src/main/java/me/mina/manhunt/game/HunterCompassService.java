package me.mina.manhunt.game;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import me.mina.manhunt.team.TeamManager;
import me.mina.manhunt.team.TeamType;
import me.mina.manhunt.world.WorldManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * Keeps hunter compasses in the main inventory and points them at the nearest
 * alive runner in the hunter's current game world.
 */
public final class HunterCompassService {
    private static final long UPDATE_PERIOD_TICKS = 10L;

    private final Plugin plugin;
    private final GameManager gameManager;
    private final TeamManager teamManager;
    private final WorldManager worldManager;
    private final ServerBridge bridge;
    private final NamespacedKey compassKey;
    private final Map<UUID, Map<UUID, Location>> lastRunnerLocations = new HashMap<>();
    private final Set<UUID> issuedCompassPlayers = new HashSet<>();
    private BukkitTask task;

    public HunterCompassService(
            Plugin plugin,
            GameManager gameManager,
            TeamManager teamManager,
            WorldManager worldManager,
            ServerBridge bridge
    ) {
        this.plugin = plugin;
        this.gameManager = gameManager;
        this.teamManager = teamManager;
        this.worldManager = worldManager;
        this.bridge = bridge;
        this.compassKey = new NamespacedKey(plugin, "hunter_compass");
    }

    public void start() {
        stop();
        this.task = this.bridge.runTaskTimer(this::update, 1L, UPDATE_PERIOD_TICKS);
    }

    public void stop() {
        if (this.task != null) {
            this.task.cancel();
            this.task = null;
        }
        this.lastRunnerLocations.clear();
        this.issuedCompassPlayers.clear();
    }

    public boolean isHunterCompass(ItemStack item) {
        if (item == null || item.getType() != Material.COMPASS || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().has(this.compassKey, PersistentDataType.BYTE);
    }

    public boolean shouldProtect(Player player) {
        if (player == null || !this.gameManager.isParticipant(player)) {
            return false;
        }
        GameState state = this.gameManager.getGameState();
        return state == GameState.RUNNING || state == GameState.ENDED;
    }

    private void update() {
        GameState state = this.gameManager.getGameState();
        if (state != GameState.RUNNING && state != GameState.ENDED) {
            this.lastRunnerLocations.clear();
            this.issuedCompassPlayers.clear();
            return;
        }

        this.recordRunnerLocations();
        for (Player hunter : this.bridge.getOnlinePlayers()) {
            if (!this.gameManager.isParticipant(hunter)
                    || this.teamManager.getTeam(hunter) != TeamType.HUNTER
                    || !this.worldManager.isGameWorld(hunter.getWorld())) {
                continue;
            }

            this.ensureCompass(hunter);
            if (state == GameState.RUNNING) {
                Location target = this.findNearestRunnerLocation(hunter);
                hunter.setCompassTarget(target == null ? hunter.getLocation() : target);
            }
        }
    }

    private void recordRunnerLocations() {
        for (UUID runnerId : this.teamManager.tracker().getAliveRunnerIds()) {
            Player runner = findOnline(runnerId);
            if (runner == null || !this.worldManager.isGameWorld(runner.getWorld())) {
                continue;
            }
            this.lastRunnerLocations
                    .computeIfAbsent(runnerId, ignored -> new HashMap<>())
                    .put(runner.getWorld().getUID(), runner.getLocation().clone());
        }
    }

    private Location findNearestRunnerLocation(Player hunter) {
        World hunterWorld = hunter.getWorld();
        Location hunterLocation = hunter.getLocation();
        Location nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (UUID runnerId : this.teamManager.tracker().getAliveRunnerIds()) {
            Player runner = findOnline(runnerId);
            Location candidate = null;
            if (runner != null && runner.getWorld().equals(hunterWorld)) {
                candidate = runner.getLocation();
            } else {
                Map<UUID, Location> knownLocations = this.lastRunnerLocations.get(runnerId);
                if (knownLocations != null) {
                    candidate = knownLocations.get(hunterWorld.getUID());
                }
            }
            if (candidate == null || !candidate.getWorld().equals(hunterWorld)) {
                continue;
            }

            double distance = hunterLocation.distanceSquared(candidate);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = candidate;
            }
        }
        return nearest;
    }

    private void ensureCompass(Player hunter) {
        PlayerInventory inventory = hunter.getInventory();
        if (findCompass(inventory) != null) {
            this.issuedCompassPlayers.add(hunter.getUniqueId());
            return;
        }
        if (this.issuedCompassPlayers.contains(hunter.getUniqueId())) {
            return;
        }
        if (addToMainInventory(inventory, createCompass())) {
            this.issuedCompassPlayers.add(hunter.getUniqueId());
        }
    }

    private boolean addToMainInventory(PlayerInventory inventory, ItemStack compass) {
        Map<Integer, ItemStack> leftovers = inventory.addItem(compass);
        return leftovers.isEmpty();
    }

    private ItemStack findCompass(PlayerInventory inventory) {
        if (this.isHunterCompass(inventory.getItemInOffHand())) {
            return inventory.getItemInOffHand();
        }
        for (ItemStack item : inventory.getStorageContents()) {
            if (this.isHunterCompass(item)) {
                return item;
            }
        }
        return null;
    }

    public void onPlayerDeath(Player player) {
        if (player != null) {
            this.issuedCompassPlayers.remove(player.getUniqueId());
        }
    }

    private ItemStack createCompass() {
        ItemStack compass = new ItemStack(Material.COMPASS);
        ItemMeta meta = compass.getItemMeta();
        meta.displayName(Component.text("Hunter Compass", NamedTextColor.RED));
        meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.getPersistentDataContainer().set(this.compassKey, PersistentDataType.BYTE, (byte) 1);
        compass.setItemMeta(meta);
        return compass;
    }

    private Player findOnline(UUID playerId) {
        for (Player player : this.bridge.getOnlinePlayers()) {
            if (player.getUniqueId().equals(playerId) && player.isOnline()) {
                return player;
            }
        }
        return null;
    }
}
