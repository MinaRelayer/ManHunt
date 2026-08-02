/*
 * Decompiled with CFR 0.152.
 */
package me.mina.manhunt.command;

import me.mina.manhunt.command.JoinTeamCommand;
import me.mina.manhunt.game.GameManager;
import me.mina.manhunt.game.GameState;
import me.mina.manhunt.lang.LangManager;
import me.mina.manhunt.support.FakeLangManager;
import me.mina.manhunt.team.TeamManager;
import me.mina.manhunt.team.TeamType;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mockito.verification.VerificationMode;

class JoinTeamCommandTest {
    private GameManager game;
    private TeamManager team;
    private FakeLangManager lang;
    private Player target;
    private JoinTeamCommand command;

    JoinTeamCommandTest() {
    }

    @BeforeEach
    void setUp() {
        this.game = (GameManager)Mockito.mock(GameManager.class);
        this.team = (TeamManager)Mockito.mock(TeamManager.class);
        this.lang = new FakeLangManager();
        this.target = (Player)Mockito.mock(Player.class);
        Mockito.when((Object)this.target.getName()).thenReturn((Object)"Steve");
        this.command = new JoinTeamCommand(this.game, this.team, (LangManager)this.lang, name -> "Steve".equals(name) ? this.target : null, TeamType.HUNTER);
    }

    @Test
    void playerJoinsOwnTeam() {
        Mockito.when((Object)this.game.getGameState()).thenReturn((Object)GameState.WAITING);
        Player sender = (Player)Mockito.mock(Player.class);
        Assertions.assertEquals((int)1, (int)this.command.execute((CommandSender)sender, null));
        ((TeamManager)Mockito.verify((Object)this.team)).joinTeam(sender, TeamType.HUNTER);
        Assertions.assertTrue((boolean)this.lang.sentKeys.contains("team-joined"));
    }

    @Test
    void teamSwitchRejectedDuringRunningGame() {
        Mockito.when((Object)this.game.getGameState()).thenReturn((Object)GameState.RUNNING);
        Player sender = (Player)Mockito.mock(Player.class);
        Assertions.assertEquals((int)0, (int)this.command.execute((CommandSender)sender, null));
        ((TeamManager)Mockito.verify((Object)this.team, (VerificationMode)Mockito.never())).joinTeam(JoinTeamCommandTest.anyPlayer(), JoinTeamCommandTest.anyType());
        Assertions.assertTrue((boolean)this.lang.sentKeys.contains("team-join-during-game"));
    }

    @Test
    void teamSwitchRejectedWhileStarting() {
        Mockito.when((Object)this.game.getGameState()).thenReturn((Object)GameState.STARTING);
        Player sender = (Player)Mockito.mock(Player.class);
        Assertions.assertEquals((int)0, (int)this.command.execute((CommandSender)sender, null));
        ((TeamManager)Mockito.verify((Object)this.team, (VerificationMode)Mockito.never())).joinTeam(JoinTeamCommandTest.anyPlayer(), JoinTeamCommandTest.anyType());
        Assertions.assertTrue((boolean)this.lang.sentKeys.contains("team-join-during-game"));
    }

    @Test
    void teamSwitchRejectedAfterWinner() {
        Mockito.when((Object)this.game.getGameState()).thenReturn((Object)GameState.ENDED);
        Player sender = (Player)Mockito.mock(Player.class);
        Assertions.assertEquals((int)0, (int)this.command.execute((CommandSender)sender, null));
        ((TeamManager)Mockito.verify((Object)this.team, (VerificationMode)Mockito.never())).joinTeam(JoinTeamCommandTest.anyPlayer(), JoinTeamCommandTest.anyType());
        Assertions.assertTrue((boolean)this.lang.sentKeys.contains("team-join-during-game"));
    }

    @Test
    void consoleCannotJoinOwnTeam() {
        Mockito.when((Object)this.game.getGameState()).thenReturn((Object)GameState.WAITING);
        CommandSender console = (CommandSender)Mockito.mock(CommandSender.class);
        Assertions.assertEquals((int)0, (int)this.command.execute(console, null));
        Assertions.assertTrue((boolean)this.lang.sentKeys.contains("player-only"));
    }

    @Test
    void adminCanAssignTargetPlayer() {
        Mockito.when((Object)this.game.getGameState()).thenReturn((Object)GameState.WAITING);
        Player admin = (Player)Mockito.mock(Player.class);
        Mockito.when((Object)admin.hasPermission("manhunt.admin")).thenReturn((Object)true);
        Assertions.assertEquals((int)1, (int)this.command.execute((CommandSender)admin, "Steve"));
        ((TeamManager)Mockito.verify((Object)this.team)).joinTeam(this.target, TeamType.HUNTER);
        Assertions.assertTrue((boolean)this.lang.sentKeys.contains("team-joined-target"));
    }

    @Test
    void nonAdminCannotAssignTargetPlayer() {
        Mockito.when((Object)this.game.getGameState()).thenReturn((Object)GameState.WAITING);
        Player admin = (Player)Mockito.mock(Player.class);
        Mockito.when((Object)admin.hasPermission("manhunt.admin")).thenReturn((Object)false);
        Assertions.assertEquals((int)0, (int)this.command.execute((CommandSender)admin, "Steve"));
        ((TeamManager)Mockito.verify((Object)this.team, (VerificationMode)Mockito.never())).joinTeam(this.target, TeamType.HUNTER);
        Assertions.assertTrue((boolean)this.lang.sentKeys.contains("no-permission"));
    }

    @Test
    void targetNotFoundReportsError() {
        Mockito.when((Object)this.game.getGameState()).thenReturn((Object)GameState.WAITING);
        Player admin = (Player)Mockito.mock(Player.class);
        Mockito.when((Object)admin.hasPermission("manhunt.admin")).thenReturn((Object)true);
        Assertions.assertEquals((int)0, (int)this.command.execute((CommandSender)admin, "Nobody"));
        Assertions.assertTrue((boolean)this.lang.sentKeys.contains("player-not-found"));
    }

    private static Player anyPlayer() {
        return (Player)ArgumentMatchers.any(Player.class);
    }

    private static TeamType anyType() {
        return (TeamType)ArgumentMatchers.any(TeamType.class);
    }
}
