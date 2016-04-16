package me.mrkirby153.uhc.bot.network.commands;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import me.mrkirby153.uhc.bot.Main;
import me.mrkirby153.uhc.bot.discord.ServerHandler;
import me.mrkirby153.uhc.bot.network.NetworkCommand;

public class LinkServer implements NetworkCommand {

    @Override
    public void process(ServerHandler.DiscordServer server, ByteArrayDataInput input, ByteArrayDataOutput response) {
        String serverId = input.readUTF();
        String discordGuild = input.readUTF();
        if (Main.discordHandler.getServerHandler().linkMcServer(serverId, discordGuild)) {
            Main.logger.info("Linked minecraft server " + serverId + " to guild " + discordGuild);
            response.writeBoolean(true);
        } else {
            response.writeBoolean(false);
        }
    }
}
