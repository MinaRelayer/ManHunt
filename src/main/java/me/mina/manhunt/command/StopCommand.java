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

public class StopCommand
implements SubCommand {
    private final GameManager gameManager;
    private final LangManager langManager;

    public StopCommand(GameManager gameManager, LangManager langManager) {
        this.gameManager = gameManager;
        this.langManager = langManager;
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> node() {
        return (LiteralArgumentBuilder)((LiteralArgumentBuilder)LiteralArgumentBuilder.<CommandSourceStack>literal("stop").requires(source -> source.getSender().hasPermission("manhunt.admin"))).executes(ctx -> this.execute(((CommandSourceStack)ctx.getSource()).getSender()));
    }

    public int execute(CommandSender sender) {
        if (!sender.hasPermission("manhunt.admin")) {
            sender.sendMessage(this.langManager.getComponent("no-permission", new Object[0]));
            return 0;
        }
        GameState state = this.gameManager.getGameState();
        if (state == GameState.WAITING || state == GameState.CLEANUP) {
            sender.sendMessage(this.langManager.getComponent("game-not-running", new Object[0]));
            return 0;
        }
        this.gameManager.endGame();
        sender.sendMessage(this.langManager.getComponent("game-stop", new Object[0]));
        return 1;
    }
}
