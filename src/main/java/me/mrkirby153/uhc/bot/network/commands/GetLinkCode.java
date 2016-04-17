package me.mrkirby153.uhc.bot.network.commands;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import me.mrkirby153.uhc.bot.Main;
import me.mrkirby153.uhc.bot.discord.ServerHandler;
import me.mrkirby153.uhc.bot.network.NetworkCommand;

import java.util.UUID;

public class GetLinkCode implements NetworkCommand{
    @Override
    public void process(ServerHandler.DiscordServer server, ByteArrayDataInput input, ByteArrayDataOutput response) {
        UUID u = UUID.fromString(input.readUTF());
        String name = input.readUTF();
        String linkCode = Main.discordHandler.generateLinkCode(u, name);
        response.writeInt(2);
        response.writeUTF(linkCode);
    }
}
