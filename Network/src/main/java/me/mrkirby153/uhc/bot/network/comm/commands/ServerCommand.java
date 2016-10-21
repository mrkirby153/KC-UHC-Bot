package me.mrkirby153.uhc.bot.network.comm.commands;

import me.mrkirby153.uhc.bot.network.comm.BotCommand;

public class ServerCommand extends BotCommand{

    private final String serverId;

    public ServerCommand(String serverId){
        this.serverId = serverId;
    }

    public String getServerId(){
        return serverId;
    }
}
