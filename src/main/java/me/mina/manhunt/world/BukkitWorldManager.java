/*
 * Decompiled with CFR 0.152.
 */
package me.mina.manhunt.world;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Stream;
import me.mina.manhunt.config.PluginConfig;
import me.mina.manhunt.world.WorldManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class BukkitWorldManager
implements WorldManager {
    private static final String MARKER = ".manhunt-owned";
    private static final DateTimeFormatter GAME_NAME_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS", Locale.ROOT);
    private final Plugin plugin;
    private final PluginConfig config;
    private final Logger logger;
    private final Path worldContainer;
    private World gameWorld;
    private World gameWorldNether;
    private World gameWorldTheEnd;
    private World defaultWorld;
    private List<Path> activePaths = List.of();

    public BukkitWorldManager(Plugin plugin, PluginConfig config) {
        this.plugin = plugin;
        this.config = config;
        this.logger = plugin.getLogger();
        this.worldContainer = Bukkit.getWorldContainer().toPath().toAbsolutePath().normalize();
        if (!Bukkit.getWorlds().isEmpty()) {
            this.defaultWorld = (World)Bukkit.getWorlds().get(0);
        }
    }

    public void cleanupOrphanedWorlds() {
        if (this.defaultWorld != null) {
            this.cleanupOrphanedRoot(this.defaultWorld.getWorldFolder().toPath().toAbsolutePath().normalize().resolve("dimensions").resolve(this.config.getWorldNamespace()).normalize());
        }
        Path legacyRoot = this.worldContainer.resolve(this.config.getWorldNamespace()).normalize();
        this.cleanupOrphanedRoot(legacyRoot);
        this.removeIfEmpty(legacyRoot);
    }

    private void removeIfEmpty(Path root) {
        if (!root.startsWith(this.worldContainer) || !Files.isDirectory(root, new LinkOption[0])) {
            return;
        }
        try (Stream<Path> stream = Files.list(root);){
            if (!stream.findAny().isPresent()) {
                Files.deleteIfExists(root);
            }
        }
        catch (IOException ex) {
            this.logger.warning("Failed to remove empty legacy directory " + String.valueOf(root) + ": " + ex.getMessage());
        }
    }

    private void cleanupOrphanedRoot(Path root) {
        if (!root.startsWith(this.worldContainer) || !Files.isDirectory(root, new LinkOption[0])) {
            return;
        }
        try (Stream<Path> stream = Files.list(root);){
            stream.filter(x$0 -> Files.isDirectory(x$0, new LinkOption[0])).filter(path -> path.getFileName().toString().startsWith("game_")).filter(this::isOwned).forEach(path -> {
                this.logger.info("Removing orphaned ManHunt world/config: " + String.valueOf(path));
                this.deleteOwnedDirectory((Path)path);
            });
        }
        catch (IOException ex) {
            this.logger.warning("Failed to scan orphaned ManHunt directories: " + ex.getMessage());
        }
    }

    @Override
    public World createGameWorld() {
        if (this.gameWorld != null || this.gameWorldNether != null || this.gameWorldTheEnd != null) {
            throw new IllegalStateException("A previous ManHunt world set is still loaded");
        }
        String id = "game_" + GAME_NAME_FORMAT.format(LocalDateTime.now());
        long seed = this.config.isRandomSeed() ? new Random().nextLong() : this.config.getWorldSeed();
        this.logger.info("Creating temporary ManHunt world set " + id + " with seed " + seed);
        ArrayList<Path> created = new ArrayList<Path>();
        try {
            this.gameWorld = this.createWorld(id + "_world", World.Environment.NORMAL, seed, created);
            this.gameWorldNether = this.createWorld(id + "_the_nether", World.Environment.NETHER, seed, created);
            this.gameWorldTheEnd = this.createWorld(id + "_the_end", World.Environment.THE_END, seed, created);
            if (this.gameWorld == null || this.gameWorldNether == null || this.gameWorldTheEnd == null) {
                throw new IllegalStateException("One or more game worlds could not be created");
            }
            this.activePaths = List.copyOf(created);
            return this.gameWorld;
        }
        catch (Exception ex) {
            this.activePaths = List.copyOf(created);
            this.logger.warning("Failed to create ManHunt world set: " + ex.getMessage());
            return null;
        }
    }

    private World createWorld(String key, World.Environment environment, long seed, List<Path> created) {
        World world = WorldCreator.ofKey((NamespacedKey)new NamespacedKey(this.config.getWorldNamespace(), key)).environment(environment).seed(seed).createWorld();
        if (world != null) {
            Path path = this.worldPath(world);
            if (!path.startsWith(this.worldContainer)) {
                throw new IllegalStateException("World path escaped the world container");
            }
            try {
                Files.createDirectories(path, new FileAttribute[0]);
                Files.writeString(path.resolve(MARKER), (CharSequence)"ManHunt temporary world\n", new OpenOption[0]);
                created.add(path);
                this.activePaths = List.copyOf(created);
            }
            catch (IOException ex) {
                throw new IllegalStateException("Failed to write world ownership marker", ex);
            }
        }
        return world;
    }

    @Override
    public void cleanupGameWorlds(Consumer<Boolean> callback) {
        List<Path> paths = this.activePaths;
        this.activePaths = List.of();
        boolean unloaded = this.unloadLoadedWorlds();
        if (!unloaded) {
            callback.accept(false);
            return;
        }
        if (paths.isEmpty()) {
            callback.accept(true);
            return;
        }
        this.attemptDelete(paths, 1, callback);
    }

    @Override
    public void unloadAndDeleteGameWorlds() {
        this.cleanupGameWorlds(success -> {
            if (!success.booleanValue()) {
                this.logger.warning("ManHunt world cleanup did not complete successfully.");
            }
        });
    }

    @Override
    public void forceDeleteAllWorlds() {
        ArrayList<Path> paths = new ArrayList<Path>(this.activePaths);
        this.activePaths = List.of();
        if (!this.unloadLoadedWorlds()) {
            this.logger.warning("Refusing forced world deletion because at least one world could not be unloaded.");
            return;
        }
        if (paths.isEmpty()) {
            return;
        }
        if (!this.plugin.isEnabled()) {
            this.deletePaths(paths);
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> this.deletePaths(paths));
    }

    private void attemptDelete(List<Path> paths, int attempt, Consumer<Boolean> callback) {
        if (!this.plugin.isEnabled()) {
            this.deletePaths(paths);
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
            boolean success = this.deletePaths(paths);
            if (!this.plugin.isEnabled()) {
                return;
            }
            if (success) {
                Bukkit.getScheduler().runTask(this.plugin, () -> {
                    if (this.plugin.isEnabled()) {
                        callback.accept(true);
                    }
                });
            } else if (attempt < 5 && this.plugin.isEnabled()) {
                Bukkit.getScheduler().runTaskLaterAsynchronously(this.plugin, () -> this.attemptDelete(paths, attempt + 1, callback), 20L);
            } else if (this.plugin.isEnabled()) {
                Bukkit.getScheduler().runTask(this.plugin, () -> {
                    if (this.plugin.isEnabled()) {
                        callback.accept(false);
                    }
                });
            }
        });
    }

    private boolean deletePaths(List<Path> paths) {
        boolean success = true;
        for (Path path : paths) {
            success &= this.deleteOwnedDirectory(path);
        }
        return success;
    }

    private boolean unloadLoadedWorlds() {
        this.evacuateGameWorlds();
        boolean success = true;
        if (this.gameWorldNether != null) {
            success &= this.safeUnload(this.gameWorldNether);
        }
        if (this.gameWorldTheEnd != null) {
            success &= this.safeUnload(this.gameWorldTheEnd);
        }
        if (this.gameWorld != null) {
            success &= this.safeUnload(this.gameWorld);
        }
        if (success) {
            this.gameWorld = null;
            this.gameWorldNether = null;
            this.gameWorldTheEnd = null;
        }
        return success;
    }

    private boolean safeUnload(World world) {
        if (world == null) {
            return true;
        }
        if (!world.getPlayers().isEmpty()) {
            return false;
        }
        try {
            return Bukkit.unloadWorld((World)world, (boolean)false);
        }
        catch (Exception ex) {
            this.logger.warning("Failed to unload world " + world.getName() + ": " + ex.getMessage());
            return false;
        }
    }

    private boolean isOwned(Path path) {
        return path != null && path.toAbsolutePath().normalize().startsWith(this.worldContainer) && Files.isRegularFile(path.resolve(MARKER), new LinkOption[0]);
    }

    private boolean deleteOwnedDirectory(Path path) {
        Path normalized = path.toAbsolutePath().normalize();
        if (!normalized.startsWith(this.worldContainer) || !this.isOwned(normalized)) {
            this.logger.warning("Refusing to delete unowned or unsafe path: " + normalized);
            return false;
        }
        try (Stream<Path> walk = Files.walk(normalized)) {
            walk.sorted(Comparator.reverseOrder()).forEach(candidate -> {
                try {
                    Files.deleteIfExists(candidate);
                } catch (IOException ex) {
                    throw new DeleteFailure(ex);
                }
            });
            return !Files.exists(normalized);
        } catch (IOException | DeleteFailure ex) {
            this.logger.warning("Failed to delete world directory " + normalized + ": " + ex.getMessage());
            return false;
        }
    }
    private Path worldPath(World world) {
        if (this.defaultWorld == null || world == null) {
            throw new IllegalStateException("The primary world is not available");
        }
        NamespacedKey key = world.getKey();
        return this.defaultWorld.getWorldFolder().toPath().toAbsolutePath().normalize().resolve("dimensions").resolve(key.getNamespace()).resolve(key.getKey()).normalize();
    }

    @Override
    public boolean evacuateGameWorlds() {
        if (this.defaultWorld == null) {
            return false;
        }
        Location spawn = this.defaultWorld.getSpawnLocation();
        boolean moved = false;
        for (World world : new World[]{this.gameWorld, this.gameWorldNether, this.gameWorldTheEnd}) {
            if (world == null) continue;
            for (Player player : new ArrayList<Player>(world.getPlayers())) {
                player.teleport(spawn);
                moved = true;
            }
        }
        return moved;
    }

    @Override
    public void teleportAllToWorld(World world) {
        if (world == null) {
            return;
        }
        Location spawn = world.getSpawnLocation();
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.teleport(spawn);
        }
    }

    @Override
    public World getGameWorld() {
        return this.gameWorld;
    }

    @Override
    public World getGameWorldNether() {
        return this.gameWorldNether;
    }

    @Override
    public World getGameWorldTheEnd() {
        return this.gameWorldTheEnd;
    }

    @Override
    public World getDefaultWorld() {
        return this.defaultWorld;
    }

    @Override
    public boolean isGameWorld(World world) {
        return world != null && (world.equals((Object)this.gameWorld) || world.equals((Object)this.gameWorldNether) || world.equals((Object)this.gameWorldTheEnd));
    }

    private static final class DeleteFailure
    extends RuntimeException {
        private DeleteFailure(IOException cause) {
            super(cause);
        }
    }
}
