package me.mrkirby153.uhc.bot.network.comm;

import redis.clients.jedis.JedisPubSub;

public class CommandListener extends JedisPubSub{

    public static String SERVER_COMMAND_CHANNEL = "command.bot";


    @Override
    public void onPMessage(String pattern, String channel, String message) {
        String[] split = channel.split(":");
        if (!split[0].equalsIgnoreCase(SERVER_COMMAND_CHANNEL))
            return;
        String command = split[1];
        BotCommandManager.instance().handle(command, message);
    }
}
