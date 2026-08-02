/*
 * Decompiled with CFR 0.152.
 */
package me.mina.manhunt.command;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import java.util.Collection;
import me.mina.manhunt.command.PlayerResolver;
import me.mina.manhunt.command.SubCommand;
import me.mina.manhunt.game.GameManager;
import me.mina.manhunt.game.GameState;
import me.mina.manhunt.lang.LangManager;
import me.mina.manhunt.team.TeamManager;
import me.mina.manhunt.team.TeamType;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TeleportCommand
implements SubCommand {
    private final GameManager gameManager;
    private final TeamManager teamManager;
    private final LangManager langManager;
    private final PlayerResolver playerResolver;

    public TeleportCommand(GameManager gameManager, TeamManager teamManager, LangManager langManager, PlayerResolver playerResolver) {
        this.gameManager = gameManager;
        this.teamManager = teamManager;
        this.langManager = langManager;
        this.playerResolver = playerResolver;
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> node() {
        return (LiteralArgumentBuilder)((LiteralArgumentBuilder)LiteralArgumentBuilder.<CommandSourceStack>literal("tp").requires(source -> this.canTeleport(source.getSender()))).then(RequiredArgumentBuilder.<CommandSourceStack, String>argument("player", StringArgumentType.string()).suggests(this.suggestOnlinePlayers()).executes(ctx -> this.execute(((CommandSourceStack)ctx.getSource()).getSender(), StringArgumentType.getString((CommandContext)ctx, (String)"player"))));
    }

    public int execute(CommandSender sender, String targetName) {
        if (!this.canTeleport(sender)) {
            sender.sendMessage(this.langManager.getComponent("no-permission", new Object[0]));
            return 0;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage(this.langManager.getComponent("player-only", new Object[0]));
            return 0;
        }
        Player player = (Player)sender;
        Player target = this.playerResolver.resolve(targetName);
        if (target == null) {
            sender.sendMessage(this.langManager.getComponent("player-not-found", "player", targetName));
            return 0;
        }
        if (!this.gameManager.isParticipant(target)) {
            sender.sendMessage(this.langManager.getComponent("player-not-found", "player", targetName));
            return 0;
        }
        player.teleport(target.getLocation());
        sender.sendMessage(this.langManager.getComponent("tp-success", "player", target.getName()));
        return 1;
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    private boolean canTeleport(CommandSender sender) {
        if (!(sender instanceof Player)) return false;
        Player player = (Player)sender;
        if (this.gameManager.getGameState() != GameState.RUNNING) {
            if (this.gameManager.getGameState() != GameState.ENDED) return false;
        }
        if (this.teamManager.getTeam(player) != TeamType.SPECTATOR) return false;
        return true;
    }

    private SuggestionProvider<CommandSourceStack> suggestOnlinePlayers() {
        return (context, builder) -> {
            Collection<? extends Player> players = Bukkit.getOnlinePlayers();
            for (Player player : players) {
                builder.suggest(player.getName());
            }
            return builder.buildFuture();
        };
    }
}
