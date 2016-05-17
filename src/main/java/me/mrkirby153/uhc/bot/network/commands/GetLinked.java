package me.mrkirby153.uhc.bot.network.commands;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import me.mrkirby153.uhc.bot.Main;
import me.mrkirby153.uhc.bot.discord.ServerHandler;
import me.mrkirby153.uhc.bot.network.NetworkCommand;
import net.dv8tion.jda.entities.User;

import java.util.UUID;

public class GetLinked implements NetworkCommand {

    @Override
    public void process(ServerHandler.DiscordServer server, ByteArrayDataInput input, ByteArrayDataOutput response) {
        UUID player = UUID.fromString(input.readUTF());
        User discordUser = Main.discordHandler.getUser(player);
        if (discordUser == null) {
            response.writeBoolean(false);
        } else {
            response.writeBoolean(true);
            response.writeUTF(discordUser.getId());
            response.writeUTF(discordUser.getUsername());
        }
    }
}
