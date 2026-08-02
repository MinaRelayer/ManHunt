/*
 * Decompiled with CFR 0.152.
 */
package me.mina.manhunt.support;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import me.mina.manhunt.world.WorldManager;
import org.bukkit.World;

public class FakeWorldManager
implements WorldManager {
    public boolean createFails = false;
    public int createCalls = 0;
    public boolean worldsDeleted = false;
    public boolean forceDeleted = false;
    public boolean cleanupSucceeds = true;
    public final List<Consumer<Boolean>> cleanupCallbacks = new ArrayList<Consumer<Boolean>>();
    public World gameWorld;
    public World nether;
    public World end;
    public World defaultWorld;

    public World createGameWorld() {
        ++this.createCalls;
        if (this.createFails) {
            return null;
        }
        return this.gameWorld;
    }

    public void cleanupGameWorlds(Consumer<Boolean> callback) {
        this.worldsDeleted = true;
        this.gameWorld = null;
        this.nether = null;
        this.end = null;
        this.cleanupCallbacks.add(callback);
    }

    public void unloadAndDeleteGameWorlds() {
        this.worldsDeleted = true;
        this.gameWorld = null;
        this.nether = null;
        this.end = null;
    }

    public void forceDeleteAllWorlds() {
        this.forceDeleted = true;
        this.gameWorld = null;
        this.nether = null;
        this.end = null;
    }

    public boolean evacuateGameWorlds() {
        return false;
    }

    public void teleportAllToWorld(World world) {
    }

    public World getGameWorld() {
        return this.gameWorld;
    }

    public World getGameWorldNether() {
        return this.nether;
    }

    public World getGameWorldTheEnd() {
        return this.end;
    }

    public World getDefaultWorld() {
        return this.defaultWorld;
    }

    public void runCleanupCallbacks() {
        ArrayList<Consumer<Boolean>> pending = new ArrayList<Consumer<Boolean>>(this.cleanupCallbacks);
        this.cleanupCallbacks.clear();
        pending.forEach(callback -> callback.accept(this.cleanupSucceeds));
    }
}
