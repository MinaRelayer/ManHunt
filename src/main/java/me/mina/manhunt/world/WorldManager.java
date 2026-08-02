/*
 * Decompiled with CFR 0.152.
 */
package me.mina.manhunt.world;

import java.util.function.Consumer;
import org.bukkit.World;

public interface WorldManager {
    public World createGameWorld();

    public void unloadAndDeleteGameWorlds();

    public void forceDeleteAllWorlds();

    public boolean evacuateGameWorlds();

    public void teleportAllToWorld(World var1);

    public World getGameWorld();

    public World getGameWorldNether();

    public World getGameWorldTheEnd();

    public World getDefaultWorld();

    default public void cleanupGameWorlds(Consumer<Boolean> callback) {
        this.unloadAndDeleteGameWorlds();
        callback.accept(true);
    }

    default public boolean isGameWorld(World world) {
        return world != null && (world.equals((Object)this.getGameWorld()) || world.equals((Object)this.getGameWorldNether()) || world.equals((Object)this.getGameWorldTheEnd()));
    }
}
