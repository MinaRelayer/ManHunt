/*
 * Decompiled with CFR 0.152.
 */
package me.mina.manhunt.command;

import me.mina.manhunt.lang.LangManager;
import org.bukkit.command.CommandSender;

public class HelpCommand {
    private final LangManager langManager;

    public HelpCommand(LangManager langManager) {
        this.langManager = langManager;
    }

    public int execute(CommandSender sender) {
        sender.sendMessage(this.langManager.getComponent("help-header", new Object[0]));
        sender.sendMessage(this.langManager.getComponent("help-runner", new Object[0]));
        sender.sendMessage(this.langManager.getComponent("help-hunter", new Object[0]));
        sender.sendMessage(this.langManager.getComponent("help-spectator", new Object[0]));
        sender.sendMessage(this.langManager.getComponent("help-tp", new Object[0]));
        if (sender.hasPermission("manhunt.admin")) {
            sender.sendMessage(this.langManager.getComponent("help-start", new Object[0]));
            sender.sendMessage(this.langManager.getComponent("help-stop", new Object[0]));
            sender.sendMessage(this.langManager.getComponent("help-reload", new Object[0]));
        }
        sender.sendMessage(this.langManager.getComponent("help-footer", new Object[0]));
        return 1;
    }
}
