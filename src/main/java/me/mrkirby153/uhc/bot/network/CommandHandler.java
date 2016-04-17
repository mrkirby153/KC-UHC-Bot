package me.mrkirby153.uhc.bot.network;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import me.mrkirby153.uhc.bot.Main;
import me.mrkirby153.uhc.bot.discord.ServerHandler;

import java.util.HashMap;
import java.util.Map;

public class CommandHandler {


    private static Map<String, NetworkCommand> commands = new HashMap<>();

    public static void registerCommand(String name, NetworkCommand command) {
        if (commands.containsKey(name.toLowerCase()))
            throw new IllegalArgumentException("Cannot register a command with the same name twice!");
        Main.logger.info("Registering command " + name + " (" + command.getClass().getCanonicalName() + ")");
        commands.put(name, command);
    }


    public static ByteArrayDataOutput execute(String minecraftServerId, String commandName, ByteArrayDataInput data) {
        ByteArrayDataOutput resp = ByteStreams.newDataOutput();
        resp.writeUTF(commandName);
        NetworkCommand networkCommand = commands.get(commandName);
        if (networkCommand == null)
            return resp;
        ServerHandler.DiscordServer minecraftServer = Main.discordHandler.getServerHandler().getForMineraftServer(minecraftServerId);
        networkCommand.process(minecraftServer, data, resp);
        return resp;
    }
}
