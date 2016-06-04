package me.mrkirby153.uhc.bot.network.commands;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import me.mrkirby153.uhc.bot.Main;
import me.mrkirby153.uhc.bot.discord.ServerHandler;
import me.mrkirby153.uhc.bot.network.NetworkCommand;
import net.dv8tion.jda.entities.Role;
import net.dv8tion.jda.entities.User;

import java.util.UUID;

public class AssignSpectatorRole implements NetworkCommand {

    @Override
    public void process(ServerHandler.DiscordServer server, ByteArrayDataInput input, ByteArrayDataOutput response) {
        UUID u = UUID.fromString(input.readUTF());
        User user = Main.discordHandler.getLinkedUser(u);
        if (user != null) {
            System.out.println("Assigning role to " + user.getUsername());
            Role spectatorRole = server.getSpectatorRole();
            server.getGuild().getManager().addRoleToUser(user, spectatorRole);
            server.getGuild().getManager().update();
        }
    }
}
