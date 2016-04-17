package me.mrkirby153.uhc.bot.network.commands;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import me.mrkirby153.uhc.bot.Main;
import me.mrkirby153.uhc.bot.discord.ServerHandler;
import me.mrkirby153.uhc.bot.network.NetworkCommand;
import net.dv8tion.jda.entities.User;
import net.dv8tion.jda.entities.VoiceChannel;

import java.util.UUID;

public class AssignTeams implements NetworkCommand {
    @Override
    public void process(ServerHandler.DiscordServer server, ByteArrayDataInput input, ByteArrayDataOutput response) {
        if (server == null) {
            response.writeInt(-1);
            return;
        }
        int players = input.readInt();
        for (int i = 0; i < players; i++) {
            UUID playerUUID = UUID.fromString(input.readUTF());
            String team = input.readUTF();
            User u = Main.discordHandler.getUser(playerUUID);
            if (u == null)
                continue;
            server.assignRank(u, team);
            // Move to team channel
            if (connectedToVice(server, u)) {
                VoiceChannel voiceChannel = server.getVoiceChannel("Team " + team);
                if (voiceChannel != null)
                    server.getGuild().getManager().moveVoiceUser(u, voiceChannel);
            }
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
