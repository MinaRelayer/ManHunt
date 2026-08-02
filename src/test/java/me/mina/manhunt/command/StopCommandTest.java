/*
 * Decompiled with CFR 0.152.
 */
package me.mina.manhunt.command;

import me.mina.manhunt.command.StopCommand;
import me.mina.manhunt.game.GameManager;
import me.mina.manhunt.game.GameState;
import me.mina.manhunt.lang.LangManager;
import me.mina.manhunt.support.FakeLangManager;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.verification.VerificationMode;

class StopCommandTest {
    private GameManager game;
    private FakeLangManager lang;
    private StopCommand command;

    StopCommandTest() {
    }

    @BeforeEach
    void setUp() {
        this.game = (GameManager)Mockito.mock(GameManager.class);
        this.lang = new FakeLangManager();
        this.command = new StopCommand(this.game, (LangManager)this.lang);
    }

    @Test
    void nonAdminIsRejected() {
        CommandSender sender = (CommandSender)Mockito.mock(CommandSender.class);
        Mockito.when((Object)sender.hasPermission("manhunt.admin")).thenReturn((Object)false);
        Assertions.assertEquals((int)0, (int)this.command.execute(sender));
        ((GameManager)Mockito.verify((Object)this.game, (VerificationMode)Mockito.never())).endGame();
        Assertions.assertTrue((boolean)this.lang.sentKeys.contains("no-permission"));
    }

    @Test
    void stopWhileWaitingIsRejected() {
        CommandSender sender = (CommandSender)Mockito.mock(CommandSender.class);
        Mockito.when((Object)sender.hasPermission("manhunt.admin")).thenReturn((Object)true);
        Mockito.when((Object)this.game.getGameState()).thenReturn((Object)GameState.WAITING);
        Mockito.when((Object)this.game.isStarting()).thenReturn((Object)false);
        Assertions.assertEquals((int)0, (int)this.command.execute(sender));
        ((GameManager)Mockito.verify((Object)this.game, (VerificationMode)Mockito.never())).endGame();
        Assertions.assertTrue((boolean)this.lang.sentKeys.contains("game-not-running"));
    }

    @Test
    void stopWhileRunningEndsGame() {
        CommandSender sender = (CommandSender)Mockito.mock(CommandSender.class);
        Mockito.when((Object)sender.hasPermission("manhunt.admin")).thenReturn((Object)true);
        Mockito.when((Object)this.game.getGameState()).thenReturn((Object)GameState.RUNNING);
        Assertions.assertEquals((int)1, (int)this.command.execute(sender));
        ((GameManager)Mockito.verify((Object)this.game)).endGame();
        Assertions.assertTrue((boolean)this.lang.sentKeys.contains("game-stop"));
    }

    @Test
    void stopAfterWinnerDeclaredEndsGame() {
        CommandSender sender = (CommandSender)Mockito.mock(CommandSender.class);
        Mockito.when((Object)sender.hasPermission("manhunt.admin")).thenReturn((Object)true);
        Mockito.when((Object)this.game.getGameState()).thenReturn((Object)GameState.ENDED);
        Assertions.assertEquals((int)1, (int)this.command.execute(sender));
        ((GameManager)Mockito.verify((Object)this.game)).endGame();
    }
}
