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

public class JoinTeamCommand
implements SubCommand {
    private final GameManager gameManager;
    private final TeamManager teamManager;
    private final LangManager langManager;
    private final PlayerResolver playerResolver;
    private final TeamType team;

    public JoinTeamCommand(GameManager gameManager, TeamManager teamManager, LangManager langManager, PlayerResolver playerResolver, TeamType team) {
        this.gameManager = gameManager;
        this.teamManager = teamManager;
        this.langManager = langManager;
        this.playerResolver = playerResolver;
        this.team = team;
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> node() {
        return (LiteralArgumentBuilder)((LiteralArgumentBuilder)LiteralArgumentBuilder.<CommandSourceStack>literal(this.team.getConfigKey()).executes(ctx -> this.execute(((CommandSourceStack)ctx.getSource()).getSender(), null))).then(RequiredArgumentBuilder.<CommandSourceStack, String>argument("player", StringArgumentType.string()).suggests(this.suggestOnlinePlayers()).executes(ctx -> this.execute(((CommandSourceStack)ctx.getSource()).getSender(), StringArgumentType.getString((CommandContext)ctx, (String)"player"))));
    }

    public int execute(CommandSender sender, String targetName) {
        if (this.gameManager.getGameState() != GameState.WAITING) {
            sender.sendMessage(this.langManager.getComponent("team-join-during-game", new Object[0]));
            return 0;
        }
        if (targetName == null) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(this.langManager.getComponent("player-only", new Object[0]));
                return 0;
            }
            Player player = (Player)sender;
            this.teamManager.joinTeam(player, this.team);
            sender.sendMessage(this.langManager.getComponent("team-joined", "team", this.getTeamName(this.team)));
            return 1;
        }
        if (!sender.hasPermission("manhunt.admin")) {
            sender.sendMessage(this.langManager.getComponent("no-permission", new Object[0]));
            return 0;
        }
        Player target = this.playerResolver.resolve(targetName);
        if (target == null) {
            sender.sendMessage(this.langManager.getComponent("player-not-found", "player", targetName));
            return 0;
        }
        this.teamManager.joinTeam(target, this.team);
        sender.sendMessage(this.langManager.getComponent("team-joined-target", "player", target.getName(), "team", this.getTeamName(this.team)));
        target.sendMessage(this.langManager.getComponent("team-joined", "team", this.getTeamName(this.team)));
        return 1;
    }

    private String getTeamName(TeamType type) {
        return this.langManager.getRaw("team-" + type.getConfigKey());
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
