/*
 * Decompiled with CFR 0.152.
 */
package me.mina.manhunt.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import me.mina.manhunt.command.SubCommand;
import me.mina.manhunt.game.GameManager;
import me.mina.manhunt.game.GameState;
import me.mina.manhunt.lang.LangManager;
import org.bukkit.command.CommandSender;

public class ReloadCommand
implements SubCommand {
    private final GameManager gameManager;
    private final LangManager langManager;
    private final Runnable reloadAction;

    public ReloadCommand(GameManager gameManager, LangManager langManager, Runnable reloadAction) {
        this.gameManager = gameManager;
        this.langManager = langManager;
        this.reloadAction = reloadAction;
    }

    public ReloadCommand(LangManager langManager, Runnable reloadAction) {
        this(null, langManager, reloadAction);
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> node() {
        return (LiteralArgumentBuilder)((LiteralArgumentBuilder)LiteralArgumentBuilder.<CommandSourceStack>literal("reload").requires(source -> source.getSender().hasPermission("manhunt.admin"))).executes(ctx -> this.execute(((CommandSourceStack)ctx.getSource()).getSender()));
    }

    public int execute(CommandSender sender) {
        if (!sender.hasPermission("manhunt.admin")) {
            sender.sendMessage(this.langManager.getComponent("no-permission", new Object[0]));
            return 0;
        }
        if (this.gameManager != null && this.gameManager.getGameState() != GameState.WAITING) {
            sender.sendMessage(this.langManager.getComponent("reload-during-game", new Object[0]));
            return 0;
        }
        this.reloadAction.run();
        sender.sendMessage(this.langManager.getComponent("reload-success", new Object[0]));
        return 1;
    }
}
