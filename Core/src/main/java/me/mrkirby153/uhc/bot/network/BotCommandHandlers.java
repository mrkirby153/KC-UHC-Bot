package me.mrkirby153.uhc.bot.network;

import me.mrkirby153.uhc.bot.Main;
import me.mrkirby153.uhc.bot.discord.ServerHandler;
import me.mrkirby153.uhc.bot.network.comm.BotCommand;
import me.mrkirby153.uhc.bot.network.comm.BotCommandHandler;
import me.mrkirby153.uhc.bot.network.comm.commands.BotCommandAssignRole;
import me.mrkirby153.uhc.bot.network.comm.commands.BotCommandAssignSpectator;
import me.mrkirby153.uhc.bot.network.comm.commands.BotCommandLink;
import me.mrkirby153.uhc.bot.network.comm.commands.ServerCommand;
import me.mrkirby153.uhc.bot.network.comm.commands.team.BotCommandAssignTeams;
import me.mrkirby153.uhc.bot.network.comm.commands.team.BotCommandNewTeam;
import me.mrkirby153.uhc.bot.network.comm.commands.team.BotCommandRemoveTeam;
import net.dv8tion.jda.entities.Role;
import net.dv8tion.jda.entities.User;
import net.dv8tion.jda.entities.VoiceChannel;

import java.util.UUID;

public class BotCommandHandlers {


    protected static ServerHandler.DiscordServer getServer(String id) {
        return Main.discordHandler.getServerHandler().getForMineraftServer(id);
    }

    public static class LinkServer implements BotCommandHandler {

        @Override
        public void handleCommand(BotCommand command) {
            if (command instanceof BotCommandLink) {
                String serverId = ((BotCommandLink) command).getServerId();
                String discordGuild = ((BotCommandLink) command).getGuild();
                Main.discordHandler.getServerHandler().linkMcServer(serverId, discordGuild);
                Main.logger.info("Linked minecraft server " + serverId + " to guild " + discordGuild);
            }
        }
    }

    public static class CreateTeam implements BotCommandHandler {

        @Override
        public void handleCommand(BotCommand command) {
            if (command instanceof BotCommandNewTeam) {
                getServer(((BotCommandNewTeam) command).getServerId()).createTeam(((BotCommandNewTeam) command).getTeamName());
            }
        }
    }

    public static class RemoveTeam implements BotCommandHandler {

        @Override
        public void handleCommand(BotCommand command) {
            if (command instanceof BotCommandRemoveTeam) {
                BotCommandRemoveTeam cmd = (BotCommandRemoveTeam) command;
                ServerHandler.DiscordServer server = getServer(cmd.getServerId());
                server.deleteChannel("team-" + cmd.getTeamName());
                server.deleteChannel("Team " + cmd.getTeamName());
            }
        }
    }

    public static class AssignTeams implements BotCommandHandler {

        @Override
        public void handleCommand(BotCommand command) {
            if (command instanceof BotCommandAssignTeams) {
                ServerHandler.DiscordServer server = getServer(((BotCommandAssignTeams) command).getServerId());
                ((BotCommandAssignTeams) command).getTeams().forEach((u, team) -> {
                    User user = Main.discordHandler.getUser(u);
                    if (user == null)
                        return;
                    server.assignRank(user, team);
                    if (connectedToVice(server, user)) {
                        VoiceChannel channel = server.getVoiceChannel("Team " + team);
                        if (channel != null) {
                            server.getGuild().getManager().moveVoiceUser(user, channel);
                        }
                    }
                });
            }
        }

        public static boolean connectedToVice(ServerHandler.DiscordServer server, User user) {
            for (VoiceChannel c : server.getGuild().getVoiceChannels()) {
                if (c.getUsers() != null)
                    for (User u : c.getUsers()) {
                        if (u.getId().equalsIgnoreCase(user.getId()))
                            return true;
                    }
            }
            return false;
        }
    }

    public static class AssignRole implements BotCommandHandler {

        @Override
        public void handleCommand(BotCommand command) {
            if (command instanceof BotCommandAssignRole) {
                BotCommandAssignRole cmd = (BotCommandAssignRole) command;
                UUID u = cmd.getUser();
                String role = cmd.getRole();
                ServerHandler.DiscordRank rank = getServer(cmd.getServerId()).getRankByName(role);
                User user = Main.discordHandler.getUser(u);
                if (user != null && rank != null)
                    rank.assign(user);
            }
        }
    }

    public static class ToLobby implements BotCommandHandler {

        @Override
        public void handleCommand(BotCommand command) {
            if (command instanceof ServerCommand)
                getServer(((ServerCommand) command).getServerId()).bringAllToLobby();
        }
    }

    public static class CreateSpectator implements BotCommandHandler {

        @Override
        public void handleCommand(BotCommand command) {
            if (command instanceof ServerCommand)
                getServer(((ServerCommand) command).getServerId()).createSpectatorRole();
        }
    }

    public static class AssignSpectator implements BotCommandHandler {

        @Override
        public void handleCommand(BotCommand command) {
            if (command instanceof BotCommandAssignSpectator) {
                User u = Main.discordHandler.getLinkedUser(((BotCommandAssignSpectator) command).getUser());
                if (u != null) {
                    ServerHandler.DiscordServer server = getServer(((BotCommandAssignSpectator) command).getServerId());
                    Role spectatorRole = server.getSpectatorRole();
                    server.getGuild().getManager().addRoleToUser(u, spectatorRole);
                    server.getGuild().getManager().update();
                }
            }
        }
    }
}
