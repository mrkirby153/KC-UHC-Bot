package me.mrkirby153.uhc.bot.network.commands;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import me.mrkirby153.uhc.bot.discord.ServerHandler;
import me.mrkirby153.uhc.bot.network.NetworkCommand;

public class CreateSpectatorRole implements NetworkCommand{
    @Override
    public void process(ServerHandler.DiscordServer server, ByteArrayDataInput input, ByteArrayDataOutput response) {
        server.createSpectatorRole();
    }
}
