package me.mrkirby153.uhc.bot.network.commands;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import me.mrkirby153.uhc.bot.Main;
import me.mrkirby153.uhc.bot.discord.ServerHandler;
import me.mrkirby153.uhc.bot.network.NetworkCommand;
import net.dv8tion.jda.entities.User;

import java.util.UUID;

public class AssignRole implements NetworkCommand {

    @Override
    public void process(ServerHandler.DiscordServer server, ByteArrayDataInput input, ByteArrayDataOutput response) {
        UUID u = UUID.fromString(input.readUTF());
        String role = input.readUTF();
        ServerHandler.DiscordRank rank = server.getRankByName(role);
        User user = Main.discordHandler.getUser(u);
        if (user != null && rank != null)
            rank.assign(user);
    }
}
