package me.mrkirby153.uhc.bot.network;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import me.mrkirby153.uhc.bot.discord.ServerHandler;

public interface NetworkCommand {


    void process(ServerHandler.DiscordServer server, ByteArrayDataInput input, ByteArrayDataOutput response);
}
