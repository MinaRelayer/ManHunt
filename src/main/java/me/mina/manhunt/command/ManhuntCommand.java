/*
 * Decompiled with CFR 0.152.
 */
package me.mina.manhunt.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import java.util.List;
import me.mina.manhunt.command.HelpCommand;
import me.mina.manhunt.command.JoinTeamCommand;
import me.mina.manhunt.command.PlayerResolver;
import me.mina.manhunt.command.ReloadCommand;
import me.mina.manhunt.command.StartCommand;
import me.mina.manhunt.command.StopCommand;
import me.mina.manhunt.command.SubCommand;
import me.mina.manhunt.command.TeleportCommand;
import me.mina.manhunt.game.GameManager;
import me.mina.manhunt.lang.LangManager;
import me.mina.manhunt.team.TeamManager;
import me.mina.manhunt.team.TeamType;
import org.bukkit.plugin.Plugin;

public class ManhuntCommand {
    private final GameManager gameManager;
    private final TeamManager teamManager;
    private final LangManager langManager;
    private final HelpCommand help;
    private final List<SubCommand> subCommands;

    public ManhuntCommand(GameManager gameManager, TeamManager teamManager, LangManager langManager, PlayerResolver playerResolver, Runnable reloadAction) {
        this.gameManager = gameManager;
        this.teamManager = teamManager;
        this.langManager = langManager;
        this.help = new HelpCommand(langManager);
        this.subCommands = List.of(new JoinTeamCommand(gameManager, teamManager, langManager, playerResolver, TeamType.RUNNER), new JoinTeamCommand(gameManager, teamManager, langManager, playerResolver, TeamType.HUNTER), new JoinTeamCommand(gameManager, teamManager, langManager, playerResolver, TeamType.SPECTATOR), new StartCommand(gameManager, langManager), new StopCommand(gameManager, langManager), new ReloadCommand(gameManager, langManager, reloadAction), new TeleportCommand(gameManager, teamManager, langManager, playerResolver));
    }

    public void register(LifecycleEventManager<Plugin> manager) {
        LiteralCommandNode root = ((LiteralArgumentBuilder)Commands.literal((String)"mh").executes(ctx -> this.help.execute(((CommandSourceStack)ctx.getSource()).getSender()))).build();
        for (SubCommand subCommand : this.subCommands) {
            root.addChild((CommandNode)subCommand.node().build());
        }
        manager.registerEventHandler(LifecycleEvents.COMMANDS.newHandler(event -> ((Commands)event.registrar()).register(root)));
    }
}
