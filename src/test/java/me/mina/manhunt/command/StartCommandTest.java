/*
 * Decompiled with CFR 0.152.
 */
package me.mina.manhunt.command;

import me.mina.manhunt.command.StartCommand;
import me.mina.manhunt.game.GameManager;
import me.mina.manhunt.lang.LangManager;
import me.mina.manhunt.support.FakeLangManager;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.verification.VerificationMode;

class StartCommandTest {
    private GameManager game;
    private FakeLangManager lang;
    private StartCommand command;

    StartCommandTest() {
    }

    @BeforeEach
    void setUp() {
        this.game = (GameManager)Mockito.mock(GameManager.class);
        this.lang = new FakeLangManager();
        this.command = new StartCommand(this.game, (LangManager)this.lang);
    }

    @Test
    void nonAdminIsRejected() {
        CommandSender sender = (CommandSender)Mockito.mock(CommandSender.class);
        Mockito.when((Object)sender.hasPermission("manhunt.admin")).thenReturn((Object)false);
        Assertions.assertEquals((int)0, (int)this.command.execute(sender));
        ((GameManager)Mockito.verify((Object)this.game, (VerificationMode)Mockito.never())).startGame();
        Assertions.assertTrue((boolean)this.lang.sentKeys.contains("no-permission"));
    }

    @Test
    void adminStartsGame() {
        CommandSender sender = (CommandSender)Mockito.mock(CommandSender.class);
        Mockito.when((Object)sender.hasPermission("manhunt.admin")).thenReturn((Object)true);
        Mockito.when((Object)this.game.startGame()).thenReturn((Object)true);
        Assertions.assertEquals((int)1, (int)this.command.execute(sender));
        ((GameManager)Mockito.verify((Object)this.game)).startGame();
    }
}
