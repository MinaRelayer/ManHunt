/*
 * Decompiled with CFR 0.152.
 */
package me.mina.manhunt.scoreboard;

import io.papermc.paper.scoreboard.numbers.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import me.mina.manhunt.config.PluginConfig;
import me.mina.manhunt.lang.LangManager;
import me.mina.manhunt.scoreboard.ScoreboardManager;
import me.mina.manhunt.team.TeamManager;
import me.mina.manhunt.team.TeamType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class BukkitScoreboardManager
implements ScoreboardManager {
    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final String OBJECTIVE_NAME = "manhunt_sidebar";
    private static final String ROLE_ENTRY = "\u00a7a";
    private static final String TIME_ENTRY = "\u00a7b";
    private static final String RUNNERS_ENTRY = "\u00a7c";
    private final Plugin plugin;
    private final TeamManager teamManager;
    private final LangManager langManager;
    private final PluginConfig config;
    private final Map<UUID, Scoreboard> boards = new HashMap<UUID, Scoreboard>();
    private final Map<UUID, Scoreboard> previousBoards = new HashMap<UUID, Scoreboard>();
    private BukkitTask task;
    private long startTime;
    private long frozenElapsedMillis;
    private boolean active;
    private boolean frozen;

    public BukkitScoreboardManager(Plugin plugin, TeamManager teamManager, LangManager langManager, PluginConfig config) {
        this.plugin = plugin;
        this.teamManager = teamManager;
        this.langManager = langManager;
        this.config = config;
    }

    @Override
    public void start() {
        this.start(System.currentTimeMillis(), new ArrayList<Player>(Bukkit.getOnlinePlayers()));
    }

    @Override
    public void start(long startTime, Collection<Player> participants) {
        this.stop();
        this.startTime = startTime;
        this.frozenElapsedMillis = 0L;
        this.active = true;
        this.frozen = false;
        for (Player player : participants) {
            this.createBoard(player);
        }
        this.task = new BukkitRunnable() {
            @Override
            public void run() {
                BukkitScoreboardManager.this.update();
            }
        }.runTaskTimer(this.plugin, 0L, this.config.getScoreboardRefreshTicks());
        this.update();
    }

    private void createBoard(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        UUID id = player.getUniqueId();
        this.previousBoards.putIfAbsent(id, player.getScoreboard());
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = board.registerNewObjective(OBJECTIVE_NAME, Criteria.DUMMY, this.langManager.getComponentWithoutPrefix("scoreboard-title", new Object[0]));
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        objective.numberFormat(NumberFormat.blank());
        board.registerNewTeam(this.teamName(TeamType.HUNTER));
        board.registerNewTeam(this.teamName(TeamType.RUNNER));
        board.registerNewTeam(this.teamName(TeamType.SPECTATOR));
        objective.getScore(ROLE_ENTRY).setScore(3);
        objective.getScore(TIME_ENTRY).setScore(2);
        objective.getScore(RUNNERS_ENTRY).setScore(1);
        this.boards.put(id, board);
        player.setScoreboard(board);
        this.updateBoard(player, board);
    }

    private void update() {
        if (!this.active) {
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!this.boards.containsKey(player.getUniqueId())) continue;
            this.updateBoard(player, this.boards.get(player.getUniqueId()));
        }
    }

    private void updateBoard(Player viewer, Scoreboard board) {
        Objective objective = board.getObjective(OBJECTIVE_NAME);
        if (objective == null) {
            return;
        }
        this.syncTeams(board);
        TeamType type = this.teamManager.getTeam(viewer);
        String role = this.langManager.getRaw("team-" + type.getConfigKey());
        String time = this.formatTime(this.elapsedMillis());
        String alive = String.valueOf(this.teamManager.tracker().getAliveRunnerIds().size());
        board.getTeam(this.teamName(TeamType.HUNTER)).prefix(this.langManager.getComponentWithoutPrefix("team-hunter-prefix", new Object[0]));
        board.getTeam(this.teamName(TeamType.RUNNER)).prefix(this.langManager.getComponentWithoutPrefix("team-runner-prefix", new Object[0]));
        board.getTeam(this.teamName(TeamType.SPECTATOR)).prefix(this.langManager.getComponentWithoutPrefix("team-spectator-prefix", new Object[0]));
        this.setPrefix(board, ROLE_ENTRY, this.langManager.getComponentWithoutPrefix("scoreboard-role", "role", role));
        this.setPrefix(board, TIME_ENTRY, this.langManager.getComponentWithoutPrefix("scoreboard-time", "time", time));
        this.setPrefix(board, RUNNERS_ENTRY, this.langManager.getComponentWithoutPrefix("scoreboard-runners-alive", "count", alive));
    }

    private void syncTeams(Scoreboard board) {
        TeamType[] types = new TeamType[]{TeamType.HUNTER, TeamType.RUNNER, TeamType.SPECTATOR};
        HashMap<String, TeamType> desired = new HashMap<String, TeamType>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            TeamType type = this.teamManager.getTeam(player);
            if (type != TeamType.HUNTER && type != TeamType.RUNNER && type != TeamType.SPECTATOR) continue;
            desired.put(player.getName(), type);
        }
        for (TeamType type : types) {
            Team team = board.getTeam(this.teamName(type));
            if (team == null) {
                team = board.registerNewTeam(this.teamName(type));
            }
            for (String entry : (String[])team.getEntries().toArray(String[]::new)) {
                if (desired.get(entry) == type) continue;
                team.removeEntry(entry);
            }
        }
        for (Map.Entry entry : desired.entrySet()) {
            Team team = board.getTeam(this.teamName((TeamType)((Object)entry.getValue())));
            if (team == null || team.hasEntry((String)entry.getKey())) continue;
            team.addEntry((String)entry.getKey());
        }
    }

    private void setPrefix(Scoreboard board, String entry, Component component) {
        Team team = board.getTeam("line_" + entry.substring(1));
        if (team == null) {
            team = board.registerNewTeam("line_" + entry.substring(1));
            team.addEntry(entry);
        }
        team.prefix(component);
    }

    private String teamName(TeamType type) {
        return "manhunt_" + type.getConfigKey();
    }

    private String formatTime(long millis) {
        long seconds = Math.max(0L, millis) / 1000L;
        long hours = seconds / 3600L;
        long minutes = seconds % 3600L / 60L;
        long secs = seconds % 60L;
        return String.format(Locale.ROOT, "%02d:%02d:%02d", hours, minutes, secs);
    }

    private long elapsedMillis() {
        if (this.frozen) {
            return this.frozenElapsedMillis;
        }
        return System.currentTimeMillis() - this.startTime;
    }

    @Override
    public void applyToAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            this.applyToPlayer(player);
        }
    }

    @Override
    public void applyToPlayer(Player player) {
        if (this.active && player != null && this.teamManager.tracker().hasSession(player.getUniqueId())) {
            if (!this.boards.containsKey(player.getUniqueId())) {
                this.createBoard(player);
            } else {
                player.setScoreboard(this.boards.get(player.getUniqueId()));
            }
        }
    }

    @Override
    public void freeze() {
        if (!this.active || this.frozen) {
            return;
        }
        this.frozenElapsedMillis = Math.max(0L, System.currentTimeMillis() - this.startTime);
        this.frozen = true;
        this.update();
        if (this.task != null) {
            this.task.cancel();
            this.task = null;
        }
    }

    @Override
    public void stop() {
        this.active = false;
        this.frozen = false;
        this.frozenElapsedMillis = 0L;
        if (this.task != null) {
            this.task.cancel();
            this.task = null;
        }
        for (Map.Entry<UUID, Scoreboard> entry : this.previousBoards.entrySet()) {
            Player player = Bukkit.getPlayer((UUID)entry.getKey());
            if (player == null || !player.isOnline()) continue;
            player.setScoreboard(entry.getValue());
        }
        this.boards.clear();
        this.previousBoards.clear();
    }
}
