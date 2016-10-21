package me.mrkirby153.uhc.bot.network.comm.commands;

public class BotCommandLink extends ServerCommand{

    private String guild;

    public BotCommandLink(String serverId, String guild) {
        super(serverId);
        this.guild = guild;
    }

    public String getGuild() {
        return guild;
    }
}
