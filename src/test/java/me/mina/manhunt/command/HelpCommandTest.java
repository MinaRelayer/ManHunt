package me.mina.manhunt.command;

import me.mina.manhunt.lang.LangManager;
import me.mina.manhunt.support.FakeLangManager;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class HelpCommandTest {
    @Test
    void playerReceivesOnlyPlayerHelp() {
        FakeLangManager lang = new FakeLangManager();
        CommandSender sender = Mockito.mock(CommandSender.class);
        Mockito.when(sender.hasPermission("manhunt.admin")).thenReturn(false);

        new HelpCommand((LangManager) lang).execute(sender);

        Assertions.assertTrue(lang.sentKeys.contains("help-player-header"));
        Assertions.assertTrue(lang.sentKeys.contains("help-runner"));
        Assertions.assertFalse(lang.sentKeys.contains("help-admin-header"));
        Assertions.assertFalse(lang.sentKeys.contains("help-start"));
    }

    @Test
    void adminReceivesSeparatedAdminHelp() {
        FakeLangManager lang = new FakeLangManager();
        CommandSender sender = Mockito.mock(CommandSender.class);
        Mockito.when(sender.hasPermission("manhunt.admin")).thenReturn(true);

        new HelpCommand((LangManager) lang).execute(sender);

        Assertions.assertTrue(lang.sentKeys.contains("help-player-header"));
        Assertions.assertTrue(lang.sentKeys.contains("help-admin-header"));
        Assertions.assertTrue(lang.sentKeys.contains("help-runner-target"));
        Assertions.assertTrue(lang.sentKeys.contains("help-start"));
    }
}
