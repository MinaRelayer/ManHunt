/*
 * Decompiled with CFR 0.152.
 */
package me.mina.manhunt.command;

import me.mina.manhunt.command.TeleportCommand;
import me.mina.manhunt.game.GameManager;
import me.mina.manhunt.game.GameState;
import me.mina.manhunt.lang.LangManager;
import me.mina.manhunt.support.FakeLangManager;
import me.mina.manhunt.team.TeamManager;
import me.mina.manhunt.team.TeamType;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mockito.verification.VerificationMode;

class TeleportCommandTest {
    private GameManager game;
    private TeamManager team;
    private FakeLangManager lang;
    private Player target;
    private TeleportCommand command;

    TeleportCommandTest() {
    }

    @BeforeEach
    void setUp() {
        this.game = (GameManager)Mockito.mock(GameManager.class);
        this.team = (TeamManager)Mockito.mock(TeamManager.class);
        this.lang = new FakeLangManager();
        this.target = (Player)Mockito.mock(Player.class);
        Mockito.when((Object)this.target.getName()).thenReturn((Object)"Steve");
        Mockito.when((Object)this.target.getLocation()).thenReturn((Object)new Location(null, 0.0, 0.0, 0.0));
        this.command = new TeleportCommand(this.game, this.team, (LangManager)this.lang, name -> "Steve".equals(name) ? this.target : null);
    }

    @Test
    void spectatorCanTeleportToPlayerDuringGame() {
        Player spectator = (Player)Mockito.mock(Player.class);
        Mockito.when((Object)this.team.getTeam(spectator)).thenReturn((Object)TeamType.SPECTATOR);
        Mockito.when((Object)this.game.getGameState()).thenReturn((Object)GameState.RUNNING);
        Mockito.when((Object)this.game.isParticipant(this.target)).thenReturn((Object)true);
        Assertions.assertEquals((int)1, (int)this.command.execute((CommandSender)spectator, "Steve"));
        ((Player)Mockito.verify((Object)spectator)).teleport((Location)ArgumentMatchers.any(Location.class));
        Assertions.assertTrue((boolean)this.lang.sentKeys.contains("tp-success"));
    }

    @Test
    void adminCannotTeleportEvenWithAdminPermission() {
        Player admin = (Player)Mockito.mock(Player.class);
        Mockito.when((Object)admin.hasPermission("manhunt.admin")).thenReturn((Object)true);
        Mockito.when((Object)this.game.getGameState()).thenReturn((Object)GameState.RUNNING);
        Assertions.assertEquals((int)0, (int)this.command.execute((CommandSender)admin, "Steve"));
        ((Player)Mockito.verify((Object)admin, (VerificationMode)Mockito.never())).teleport((Location)ArgumentMatchers.any(Location.class));
        Assertions.assertTrue((boolean)this.lang.sentKeys.contains("no-permission"));
    }

    @Test
    void nonSpectatorNonAdminIsRejected() {
        Player runner = (Player)Mockito.mock(Player.class);
        Mockito.when((Object)this.team.getTeam(runner)).thenReturn((Object)TeamType.RUNNER);
        Assertions.assertEquals((int)0, (int)this.command.execute((CommandSender)runner, "Steve"));
        ((Player)Mockito.verify((Object)runner, (VerificationMode)Mockito.never())).teleport((Location)ArgumentMatchers.any(Location.class));
        Assertions.assertTrue((boolean)this.lang.sentKeys.contains("no-permission"));
    }

    @Test
    void consoleIsRejected() {
        CommandSender console = (CommandSender)Mockito.mock(CommandSender.class);
        Assertions.assertEquals((int)0, (int)this.command.execute(console, "Steve"));
        Assertions.assertTrue((boolean)this.lang.sentKeys.contains("no-permission"));
    }

    @Test
    void spectatorCannotTeleportOutsideGame() {
        Player spectator = (Player)Mockito.mock(Player.class);
        Mockito.when((Object)this.team.getTeam(spectator)).thenReturn((Object)TeamType.SPECTATOR);
        Mockito.when((Object)this.game.getGameState()).thenReturn((Object)GameState.WAITING);
        Assertions.assertEquals((int)0, (int)this.command.execute((CommandSender)spectator, "Steve"));
        ((Player)Mockito.verify((Object)spectator, (VerificationMode)Mockito.never())).teleport((Location)ArgumentMatchers.any(Location.class));
        Assertions.assertTrue((boolean)this.lang.sentKeys.contains("no-permission"));
    }

    @Test
    void hunterCannotTeleportDuringGame() {
        Player hunter = (Player)Mockito.mock(Player.class);
        Mockito.when((Object)this.team.getTeam(hunter)).thenReturn((Object)TeamType.HUNTER);
        Mockito.when((Object)this.game.getGameState()).thenReturn((Object)GameState.RUNNING);
        Assertions.assertEquals((int)0, (int)this.command.execute((CommandSender)hunter, "Steve"));
        ((Player)Mockito.verify((Object)hunter, (VerificationMode)Mockito.never())).teleport((Location)ArgumentMatchers.any(Location.class));
        Assertions.assertTrue((boolean)this.lang.sentKeys.contains("no-permission"));
    }

    @Test
    void targetNotFoundReportsError() {
        Player spectator = (Player)Mockito.mock(Player.class);
        Mockito.when((Object)this.team.getTeam(spectator)).thenReturn((Object)TeamType.SPECTATOR);
        Mockito.when((Object)this.game.getGameState()).thenReturn((Object)GameState.RUNNING);
        Assertions.assertEquals((int)0, (int)this.command.execute((CommandSender)spectator, "Nobody"));
        Assertions.assertTrue((boolean)this.lang.sentKeys.contains("player-not-found"));
    }
}
