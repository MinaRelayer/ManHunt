/*
 * Decompiled with CFR 0.152.
 */
package me.mina.manhunt.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import me.mina.manhunt.command.SubCommand;
import me.mina.manhunt.game.GameManager;
import me.mina.manhunt.lang.LangManager;
import org.bukkit.command.CommandSender;

public class StartCommand
implements SubCommand {
    private final GameManager gameManager;
    private final LangManager langManager;

    public StartCommand(GameManager gameManager, LangManager langManager) {
        this.gameManager = gameManager;
        this.langManager = langManager;
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> node() {
        return (LiteralArgumentBuilder)((LiteralArgumentBuilder)LiteralArgumentBuilder.<CommandSourceStack>literal("start").requires(source -> source.getSender().hasPermission("manhunt.admin"))).executes(ctx -> this.execute(((CommandSourceStack)ctx.getSource()).getSender()));
    }

    public int execute(CommandSender sender) {
        if (!sender.hasPermission("manhunt.admin")) {
            sender.sendMessage(this.langManager.getComponent("no-permission", new Object[0]));
            return 0;
        }
        return this.gameManager.startGame() ? 1 : 0;
    }
}
