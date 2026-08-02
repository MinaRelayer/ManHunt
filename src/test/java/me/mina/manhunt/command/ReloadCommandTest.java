/*
 * Decompiled with CFR 0.152.
 */
package me.mina.manhunt.command;

import java.util.concurrent.atomic.AtomicBoolean;
import me.mina.manhunt.command.ReloadCommand;
import me.mina.manhunt.lang.LangManager;
import me.mina.manhunt.support.FakeLangManager;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ReloadCommandTest {
    ReloadCommandTest() {
    }

    @Test
    void adminTriggersReload() {
        FakeLangManager lang = new FakeLangManager();
        AtomicBoolean reloaded = new AtomicBoolean(false);
        ReloadCommand command = new ReloadCommand((LangManager)lang, () -> reloaded.set(true));
        CommandSender sender = (CommandSender)Mockito.mock(CommandSender.class);
        Mockito.when((Object)sender.hasPermission("manhunt.admin")).thenReturn((Object)true);
        Assertions.assertEquals((int)1, (int)command.execute(sender));
        Assertions.assertTrue((boolean)reloaded.get());
        Assertions.assertTrue((boolean)lang.sentKeys.contains("reload-success"));
    }

    @Test
    void nonAdminDoesNotReload() {
        FakeLangManager lang = new FakeLangManager();
        AtomicBoolean reloaded = new AtomicBoolean(false);
        ReloadCommand command = new ReloadCommand((LangManager)lang, () -> reloaded.set(true));
        CommandSender sender = (CommandSender)Mockito.mock(CommandSender.class);
        Mockito.when((Object)sender.hasPermission("manhunt.admin")).thenReturn((Object)false);
        Assertions.assertEquals((int)0, (int)command.execute(sender));
        Assertions.assertFalse((boolean)reloaded.get());
        Assertions.assertTrue((boolean)lang.sentKeys.contains("no-permission"));
    }
}
