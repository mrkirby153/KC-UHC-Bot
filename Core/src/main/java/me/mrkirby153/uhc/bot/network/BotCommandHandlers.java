package me.mrkirby153.uhc.bot.network;

import me.mrkirby153.uhc.bot.Main;
import me.mrkirby153.uhc.bot.discord.DiscordGuild;
import me.mrkirby153.uhc.bot.network.comm.BotCommand;
import me.mrkirby153.uhc.bot.network.comm.BotCommandHandler;
import me.mrkirby153.uhc.bot.network.comm.commands.BotCommandAssignSpectator;
import me.mrkirby153.uhc.bot.network.comm.commands.BotCommandLink;
import me.mrkirby153.uhc.bot.network.comm.commands.ServerCommand;
import me.mrkirby153.uhc.bot.network.comm.commands.team.BotCommandAssignTeams;
import me.mrkirby153.uhc.bot.network.comm.commands.team.BotCommandNewTeam;
import me.mrkirby153.uhc.bot.network.comm.commands.team.BotCommandRemoveTeam;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.entities.VoiceChannel;


public class BotCommandHandlers {


    protected static DiscordGuild getServer(String id) {
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
                DiscordGuild server = getServer(cmd.getServerId());
                server.deleteChannel("team-" + cmd.getTeamName());
                server.deleteChannel("Team " + cmd.getTeamName());
            }
        }
    }

    public static class AssignTeams implements BotCommandHandler {

        public static boolean connectedToVice(DiscordGuild server, Member user) {
            for (VoiceChannel c : server.getGuild().getVoiceChannels()) {
                if (c.getMembers() != null)
                    for (Member u : c.getMembers()) {
                        if (u.getUser().getId().equalsIgnoreCase(user.getUser().getId()))
                            return true;
                    }
            }
            return false;
        }

        @Override
        public void handleCommand(BotCommand command) {
            if (command instanceof BotCommandAssignTeams) {
                DiscordGuild server = getServer(((BotCommandAssignTeams) command).getServerId());
                ((BotCommandAssignTeams) command).getTeams().forEach((u, team) -> {
                    User user = Main.discordHandler.getUser(u);
                    if(user == null)
                        return;
                    Member member = server.getGuild().getMember(user);
                    if (member == null)
                        return;
                    server.getTeam(team).assignUser(member);
                    if (connectedToVice(server, member)) {
                        VoiceChannel channel = (VoiceChannel) server.getChannel("Team " + team, DiscordGuild.ChannelType.VOICE);
                        if (channel != null) {
                            server.getGuild().getController().moveVoiceMember(member, channel).queue();
                        }
                    }
                });
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

    public static class AssignSpectator implements BotCommandHandler {

        @Override
        public void handleCommand(BotCommand command) {
            if (command instanceof BotCommandAssignSpectator) {
                DiscordGuild server = getServer(((BotCommandAssignSpectator) command).getServerId());
                Member u = server.getGuild().getMember(Main.discordHandler.getLinkedUser(((BotCommandAssignSpectator) command).getUser()));
                if (u != null) {
                    Role spectatorRole = server.getSpectatorRole();
                    server.getGuild().getController().addRolesToMember(u, spectatorRole).queue();
                }
            }
        }
    }
}
