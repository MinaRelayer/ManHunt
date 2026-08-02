/*
 * Decompiled with CFR 0.152.
 */
package me.mina.manhunt.support;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import me.mina.manhunt.game.ServerBridge;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.mockito.Mockito;

public class FakeServerBridge
implements ServerBridge {
    public final List<Runnable> laterTasks = new ArrayList<Runnable>();
    public final List<Runnable> timerTasks = new ArrayList<Runnable>();
    public final List<BukkitTask> scheduledTasks = new ArrayList<BukkitTask>();
    public final List<Player> onlinePlayers = new ArrayList<Player>();
    public final Map<UUID, String> playerNames = new HashMap<UUID, String>();

    public BukkitTask runTaskLater(Runnable task, long delayTicks) {
        this.laterTasks.add(task);
        return this.newTask();
    }

    public BukkitTask runTaskTimer(Runnable task, long delayTicks, long periodTicks) {
        this.timerTasks.add(task);
        return this.newTask();
    }

    public List<Player> getOnlinePlayers() {
        return List.copyOf(this.onlinePlayers);
    }

    public String getPlayerName(UUID playerId) {
        return this.playerNames.getOrDefault(playerId, "Player");
    }

    public void runAllLaterTasks() {
        ArrayList<Runnable> pending = new ArrayList<Runnable>(this.laterTasks);
        this.laterTasks.clear();
        pending.forEach(Runnable::run);
    }

    public void runTimerTask(int index) {
        this.timerTasks.get(index).run();
    }

    private BukkitTask newTask() {
        BukkitTask task = (BukkitTask)Mockito.mock(BukkitTask.class);
        this.scheduledTasks.add(task);
        return task;
    }
}
