package me.mrkirby153.uhc.bot.network.commands;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import me.mrkirby153.uhc.bot.Main;
import me.mrkirby153.uhc.bot.discord.ServerHandler;
import me.mrkirby153.uhc.bot.network.NetworkCommand;
import net.dv8tion.jda.Permission;
import net.dv8tion.jda.entities.TextChannel;
import net.dv8tion.jda.entities.VoiceChannel;
import net.dv8tion.jda.managers.PermissionOverrideManager;

public class CreateTeam implements NetworkCommand {

    @Override
    public void process(ServerHandler.DiscordServer server, ByteArrayDataInput input, ByteArrayDataOutput response) {
        if (server == null) {
            response.writeInt(-1);
            return;
        }
        String team = input.readUTF();
        Main.logger.info("Creating rank " + team);
        TextChannel c = server.createTextChannel("team-" + team);
        VoiceChannel v = server.createVoiceChannel("Team " + team);

        ServerHandler.DiscordRank rank = server.getRankByName(team);
        if (rank == null)
            rank = server.createRank(team);
        // Remove permissions
        PermissionOverrideManager textChannel = c.createPermissionOverride(rank.getRole());
        PermissionOverrideManager defaultTextChannel = c.createPermissionOverride(c.getGuild().getPublicRole());

        PermissionOverrideManager voiceChannel = v.createPermissionOverride(rank.getRole());
        PermissionOverrideManager defaultVoiceChannel = v.createPermissionOverride(v.getGuild().getPublicRole());

        if (server.getSpectatorRole() != null) {
            PermissionOverrideManager spectatorText = c.createPermissionOverride(server.getSpectatorRole());
            PermissionOverrideManager spectatorVoice = v.createPermissionOverride(server.getSpectatorRole());
            spectatorText.deny(Permission.MESSAGE_WRITE);
            spectatorText.grant(Permission.MESSAGE_READ);

            spectatorVoice.deny(Permission.VOICE_SPEAK);
            spectatorVoice.grant(Permission.VOICE_CONNECT);

            spectatorText.update();
            spectatorVoice.update();
        }

        textChannel.grant(Permission.MESSAGE_READ, Permission.MESSAGE_WRITE);
        defaultTextChannel.deny(Permission.MESSAGE_READ, Permission.MESSAGE_WRITE);

        voiceChannel.grant(Permission.VOICE_CONNECT, Permission.VOICE_SPEAK);
        defaultVoiceChannel.deny(Permission.VOICE_CONNECT, Permission.VOICE_SPEAK);
        textChannel.update();
        defaultTextChannel.update();
        voiceChannel.update();
        defaultVoiceChannel.update();
        response.writeInt(0);
    }
}
